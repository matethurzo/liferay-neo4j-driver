/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.neo4j;

import aQute.bnd.annotation.metatype.Configurable;
import com.liferay.neo4j.configuration.GraphDatabaseConfiguration;
import com.liferay.neo4j.result.GraphDatabaseResult;
import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * GraphDatabase component service which will be registered to the OSGi component service. Provides a basic interface to
 * get a Neo4j server connection and running simple queries.
 *
 * @author Mate Thurzo
 */
@Component(
	configurationPid = "com.liferay.neo4j.configuration.GraphDatabaseConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	immediate = true, service = GraphDatabase.class)
@Designate(ocd = GraphDatabaseConfiguration.class)
public class GraphDatabase {

	/**
	 * OSGi activate method
	 *
	 * @param properties service component configuration parameters
	 */
	@Activate
	public void activate(Map<String, Object> properties) {
		_sessionMap = new HashMap<>();

		_graphDatabaseConfiguration = Configurable.createConfigurable(GraphDatabaseConfiguration.class, properties);

		String uri = "bolt://" + _graphDatabaseConfiguration.hostname() + ":" + _graphDatabaseConfiguration.port();

		_neo4jDriver = org.neo4j.driver.v1.GraphDatabase.driver(
			uri, AuthTokens.basic(_graphDatabaseConfiguration.userName(), _graphDatabaseConfiguration.password()));
	}

	/**
	 * OSGi modified method
	 *
	 * @param properties service component configuration parameters
	 */
	@Modified
	public void modified(Map<String, Object> properties) {
		if (_neo4jDriver != null) {
			_neo4jDriver.close();
		}

		_sessionMap = new HashMap<>();

		_graphDatabaseConfiguration = Configurable.createConfigurable(GraphDatabaseConfiguration.class, properties);

		String uri = "bolt://" + _graphDatabaseConfiguration.hostname() + ":" + _graphDatabaseConfiguration.port();

		_neo4jDriver = org.neo4j.driver.v1.GraphDatabase.driver(
			uri, AuthTokens.basic(_graphDatabaseConfiguration.userName(), _graphDatabaseConfiguration.password()));
	}

	/**
	 * A basic timeout value after a session is being automatically closed if one of the runStatement methods is being
	 * used with the autocloseSession parameter set to true.
	 *
	 * <i>This has no effect on the OSGi service configuration</i>
	 */
	public static final int SESSION_AUTOCLOSE_TIMEOUT = 5000;

	public GraphDatabaseService getEmbeddedDatabaseService() {
		if (_embeddedDatabaseService == null) {
			GraphDatabaseFactory graphDatabaseFactory =
				new GraphDatabaseFactory();

			_embeddedDatabaseService = graphDatabaseFactory.newEmbeddedDatabase(
				new File(_graphDatabaseConfiguration.embeddedDatabasePath()));
		}

		return _embeddedDatabaseService;
	}

	/**
	 * Runs a Cypher statement on the graph database instance configured via OSGi. This method automatically opens a
	 * new session and closes it immediately after the statement execution.
	 *
	 * @param statement the Cypher statement which will be executed on the database
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	public GraphDatabaseResult runImmediateStatement(String statement) {
		try (Session session = _neo4jDriver.session()) {
			StatementResult result = session.run(statement);

			return new GraphDatabaseResult(result);
		}
	}

	/**
	 * Runs a Cypher statement on the graph database instance configured via OSGi. The session remains open until the
	 * result set is exhausted and is closed after all result has been retrieved.
	 *
	 * @param statement the Cypher statement to run against the database
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	public GraphDatabaseResult runStatement(String statement) {
		Session session = _neo4jDriver.session();

		StatementResult result = session.run(statement);

		GraphDatabaseResult graphDatabaseResult = new GraphDatabaseResult(result);

		graphDatabaseResult.onExhaustResult(session::close);

		return graphDatabaseResult;
	}

	/**
	 * Returns the Neo4j driver configured via the OSGi service configuration.
	 *
	 * @return a Neo4j driver class, null if the driver is not configured properly
	 */
	public Driver getDriver() {
		if (_neo4jDriver == null) {
			String uri = "bolt://" + _graphDatabaseConfiguration.hostname() + ":" + _graphDatabaseConfiguration.port();

			_neo4jDriver = org.neo4j.driver.v1.GraphDatabase.driver(
				uri, AuthTokens.basic(_graphDatabaseConfiguration.userName(), _graphDatabaseConfiguration.password()));
		}

		return _neo4jDriver;
	}

	/**
	 * Returns a <code>Session</code> from the Neo4j driver configured via OSGi. When using this method the developer
	 * need to take care of closing the session when the work is done.
	 *
	 * @return a <code>Session</code> object from the Neo4j driver
	 */
	public Session getSession() {
		return _neo4jDriver.session();
	}

	/**
	 * Returns a <code>Session</code> from the Neo4j driver configured via OSGi. The session will be automatically
	 * closed after the given timeout.
	 *
	 * @param autoCloseTimeout a timeout in milliseconds after the session will be automatically closed
	 * @return a <code>Session</code> object from the Neo4j driver
	 */
	public Session getAutoclosingSession(long autoCloseTimeout) {
		Session session = _neo4jDriver.session();

		_autoCloseSessionTask(session, autoCloseTimeout);

		return session;
	}

	/**
	 * Acquires a new Neo4j Driver object.
	 *
	 * <i>This method does not respect the OSGi service configuration</i>
	 *
	 * @param url the server url for a Neo4j database
	 * @param authToken the authentication token for the Neo4j database
	 * @return A Neo4j Driver object. This is the Driver object from the official Neo4j driver and it is not wrapped
	 */
	public org.neo4j.driver.v1.Driver newNeo4jDriver(
			String url, AuthToken authToken) {

		return org.neo4j.driver.v1.GraphDatabase.driver(url, authToken);
	}

	/**
	 * @deprecated As of 1.2.0
	 *
	 * Runs a Cypher statement against a Neo4j database. This method opens up a connection with the given connection
	 * details, runs the statement and automatically closes the session after the default time defined in
	 * {@link #SESSION_AUTOCLOSE_TIMEOUT}
	 *
	 * <i>This method does not respect the OSGi service configuration</i>
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	@Deprecated
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement) {

		return runStatement(url, userName, password, statement, true);
	}

	/**
	 * @deprecated As of 1.2.0
	 *
	 * Runs a Cypher statement against a Neo4j database. This method opens up a connection with the given connection
	 * details, runs the statement and if the <code>autocloseSession</code> parameter is <code>true</code> automatically
	 * closes the session after the default time defined in {@link #SESSION_AUTOCLOSE_TIMEOUT}
	 *
	 * <i>This method does not respect the OSGi service configuration</i>
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @param autocloseSession define <code>true</code> if the session needs to be automatically closed after the
 	 *                         default timeout
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	@Deprecated
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, boolean autocloseSession) {

		return doRunStatement(url, userName, password, statement, null, autocloseSession, SESSION_AUTOCLOSE_TIMEOUT);
	}

	/**
	 * @deprecated As of 1.2.0
	 *
	 * Runs a Cypher statement against a Neo4j database. This method opens up a connection with the given connection
	 * details, runs the statement and if the <code>autocloseSession</code> parameter is <code>true</code> automatically
	 * closes the session after the default time defined in the <code>autocloseTimeout</code> parameter.
	 *
	 * <i>This method does not respect the OSGi service configuration</i>
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @param autocloseSession define <code>true</code> if the session needs to be automatically closed after the
	 *                         default timeout
	 * @param autocloseTimeout a time value give in milliseconds to automatically close the opened session after
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	@Deprecated
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, boolean autocloseSession,
		int autocloseTimeout) {

		return doRunStatement(url, userName, password, statement, null, autocloseSession, autocloseTimeout);
	}

	/**
	 * @deprecated As of 1.2.0
	 *
	 * Runs a Cypher statement with the given parameters against a Neo4j database. This method opens up a connection
	 * with the given connection details, This method opens up a connection with the given connection details, runs the
	 * statement and automatically closes the session after the default time defined in
	 * {@link #SESSION_AUTOCLOSE_TIMEOUT}
	 *
	 * <i>This method does not respect the OSGi service configuration</i>
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @param parameters a parameter map being passed to the database to use with the statement
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	@Deprecated
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters) {

		return runStatement(
			url, userName, password, statement, parameters, true);
	}

	/**
	 * @deprecated As of 1.2.0
	 *
	 * Runs a Cypher statement with the given parameters against a Neo4j database. This method opens up a connection
	 * with the given connection details, runs the statement and if the <code>autocloseSession</code> parameter is
	 * <code>true</code> automatically closes the session after the default time defined in
	 * {@link #SESSION_AUTOCLOSE_TIMEOUT}
	 *
	 * <i>This method does not respect the OSGi service configuration</i>
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @param parameters a parameter map being passed to the database to use with the statement
	 * @param autocloseSession define <code>true</code> if the session needs to be automatically closed after the
	 *                         default timeout
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	@Deprecated
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters,
		boolean autocloseSession) {

		return runStatement(
			url, userName, password, statement, parameters, autocloseSession, SESSION_AUTOCLOSE_TIMEOUT);
	}

	/**
	 * @deprecated As of 1.2.0
	 *
	 * Runs a Cypher statement with the given parameters against a Neo4j database. This method opens up a connection
	 * with the given connection details, runs the statement and if the <code>autocloseSession</code> parameter is
	 * <code>true</code> automatically closes the session after the default time defined in the
	 * <code>autocloseTimeout</code> parameter.
	 *
	 * <i>This method does not respect the OSGi service configuration</i>
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @param parameters a parameter map being passed to the database to use with the statement
	 * @param autocloseSession define <code>true</code> if the session needs to be automatically closed after the
	 *                         default timeout
	 * @param autocloseTimeout a time value give in milliseconds to automatically close the opened session after
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	@Deprecated
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters,
		boolean autocloseSession, int autocloseTimeout) {

		return doRunStatement(url, userName, password, statement, parameters, autocloseSession, autocloseTimeout);
	}

	/**
	 * @deprecated As of 1.2.0
	 *
	 * Closes a session previously opened by a <code>runStatement method</code>.
	 *
	 * @param resultUuid the UUID value of the result. This can be obtained from a {@link GraphDatabaseResult} object
	 *                   which is a result of a <code>runStatement</code> method
	 */
	@Deprecated
	public void endStatement(String resultUuid) {
		Session session = _sessionMap.remove(resultUuid);

		session.close();
	}

	protected GraphDatabaseResult doRunStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters,
		boolean autocloseSession, int autocloseTimeout) {

		final Session session = _neo4jDriver.session();

		StatementResult statementResult = session.run(statement, parameters);

		GraphDatabaseResult result = new GraphDatabaseResult(statementResult);

		if (autocloseSession) {
			ExecutorService executorService = Executors.newSingleThreadExecutor();

			executorService.submit(_autoCloseSessionTask(session, autocloseTimeout));
		}
		else {
			_sessionMap.put(result.getResultUuid(), session);
		}

		return result;
	}

	private org.neo4j.driver.v1.Driver _getNeo4jDriver(
		String url, String userName, String password) {

		if (_neo4jDriver != null) {
			return _neo4jDriver;
		}

		_neo4jDriver = org.neo4j.driver.v1.GraphDatabase.driver(url, AuthTokens.basic(userName, password));

		return _neo4jDriver;
	}

	private Callable<Void> _autoCloseSessionTask(Session session, long autocloseTimeout) {
		return () -> {
			TimeUnit.MILLISECONDS.sleep(autocloseTimeout);

			session.close();

			return null;
		};
	}

	private GraphDatabaseService _embeddedDatabaseService;
	private org.neo4j.driver.v1.Driver _neo4jDriver;
	private Map<String, Session> _sessionMap;
	private GraphDatabaseConfiguration _graphDatabaseConfiguration;

}