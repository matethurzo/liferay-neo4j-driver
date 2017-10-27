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
import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

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
public class GraphDatabase {

	@Activate
	public void activate(Map<String, Object> properties) {
		_graphDatabaseConfiguration = Configurable.createConfigurable(GraphDatabaseConfiguration.class, properties);

		org.neo4j.driver.v1.Driver driver = GraphDatabase.driver(
			"bolt://localhost:7687", AuthTokens.basic("neo4j", "test"));Í
	}

	/**
	 * A basic timeout value after a session is being automatically closed if one of the runStatement methods is being
	 * used with the autocloseSession parameter set to true.
	 */
	public static final int SESSION_AUTOCLOSE_TIMEOUT = 5000;

	/**
	 * Acquires a new Neo4j Driver object.
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
	 * Runs a Cypher statement against a Neo4j database. This method opens up a connection with the given connection
	 * details, runs the statement and automatically closes the session after the default time defined in
	 * {@link #SESSION_AUTOCLOSE_TIMEOUT}
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement) {

		return runStatement(url, userName, password, statement, true);
	}

	/**
	 * Runs a Cypher statement against a Neo4j database. This method opens up a connection with the given connection
	 * details, runs the statement and if the <code>autocloseSession</code> parameter is <code>true</code> automatically
	 * closes the session after the default time defined in {@link #SESSION_AUTOCLOSE_TIMEOUT}
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @param autocloseSession define <code>true</code> if the session needs to be automatically closed after the
 	 *                         default timeout
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, boolean autocloseSession) {

		return doRunStatement(url, userName, password, statement, null, autocloseSession, SESSION_AUTOCLOSE_TIMEOUT);
	}

	/**
	 * Runs a Cypher statement against a Neo4j database. This method opens up a connection with the given connection
	 * details, runs the statement and if the <code>autocloseSession</code> parameter is <code>true</code> automatically
	 * closes the session after the default time defined in the <code>autocloseTimeout</code> parameter.
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
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, boolean autocloseSession,
		int autocloseTimeout) {

		return doRunStatement(url, userName, password, statement, null, autocloseSession, autocloseTimeout);
	}

	/**
	 * Runs a Cypher statement with the given parameters against a Neo4j database. This method opens up a connection
	 * with the given connection details, This method opens up a connection with the given connection details, runs the
	 * statement and automatically closes the session after the default time defined in
	 * {@link #SESSION_AUTOCLOSE_TIMEOUT}
	 *
	 * @param url the server url for a Neo4j database
	 * @param userName the user name being used for the connection to the database
	 * @param password the password being used for the connection to the database
	 * @param statement the Cypher statement which will be executed on the database
	 * @param parameters a parameter map being passed to the database to use with the statement
	 * @return a result object wrapping the Neo4j {@link StatementResult}
	 */
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters) {

		return runStatement(
			url, userName, password, statement, parameters, true);
	}

	/**
	 * Runs a Cypher statement with the given parameters against a Neo4j database. This method opens up a connection
	 * with the given connection details, runs the statement and if the <code>autocloseSession</code> parameter is
	 * <code>true</code> automatically closes the session after the default time defined in
	 * {@link #SESSION_AUTOCLOSE_TIMEOUT}
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
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters,
		boolean autocloseSession) {

		return runStatement(
			url, userName, password, statement, parameters, autocloseSession, SESSION_AUTOCLOSE_TIMEOUT);
	}

	/**
	 * Runs a Cypher statement with the given parameters against a Neo4j database. This method opens up a connection
	 * with the given connection details, runs the statement and if the <code>autocloseSession</code> parameter is
	 * <code>true</code> automatically closes the session after the default time defined in the
	 * <code>autocloseTimeout</code> parameter.
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
	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters,
		boolean autocloseSession, int autocloseTimeout) {

		return doRunStatement(url, userName, password, statement, parameters, autocloseSession, autocloseTimeout);
	}

	/**
	 * Closes a session previously opened by a <code>runStatement method</code>.
	 *
	 * @param resultUuid the UUID value of the result. This can be obtained from a {@link GraphDatabaseResult} object
	 *                   which is a result of a <code>runStatement</code> method
	 */
	public void endStatement(String resultUuid) {
		Session session = _sessionMap.remove(resultUuid);

		session.close();
	}

	@Activate
	@Modified
	protected void activate() {
		_sessionMap = new HashMap<>();
	}

	protected GraphDatabaseResult doRunStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters,
		boolean autocloseSession, int autocloseTimeout) {

		_neo4jDriver = _getNeo4jDriver(url, userName, password);

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

	private Callable<Void> _autoCloseSessionTask(Session session, int autocloseTimeout) {
		return () -> {
			TimeUnit.MILLISECONDS.sleep(autocloseTimeout);

			session.close();

			return null;
		};
	}

	private org.neo4j.driver.v1.Driver _neo4jDriver;
	private Map<String, Session> _sessionMap;
	private GraphDatabaseConfiguration _graphDatabaseConfiguration;

}