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

import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * @author Mate Thurzo
 */
@Component(immediate = true, service = GraphDatabase.class)
public class GraphDatabase {

	public static final int SESSION_AUTOCLOSE_TIMEOUT = 5000;

	public org.neo4j.driver.v1.Driver newNeo4jDriver(
		String url, AuthToken authToken) {

		return org.neo4j.driver.v1.GraphDatabase.driver(url, authToken);
	}

	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement) {

		return runStatement(url, userName, password, statement, true);
	}

	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, boolean autocloseSession) {

		return doRunStatement(url, userName, password, statement, null, autocloseSession, SESSION_AUTOCLOSE_TIMEOUT);
	}

	public GraphDatabaseResult runStatement(
		String url, String userName, String password, String statement, Map<String, Object> parameters) {

		return doRunStatement(
			url, userName, password, statement, parameters, true, SESSION_AUTOCLOSE_TIMEOUT);
	}

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

}