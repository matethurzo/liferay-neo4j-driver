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

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;

import java.util.List;
import java.util.UUID;

/**
 * This class is a result of statement ran against a Neo4j database using one of ther <code>runStatement</code> methods
 * in {@link GraphDatabase}. Wraps a Neo4j {@link StatementResult} object and holds an additional result UUID.
 *
 * @author Mate Thurzo
 */
public class GraphDatabaseResult {

	/**
	 * Creates a <code>GraphDatabaseResult</code> object based on a StatementResult coming from the Neo4j database.
	 *
	 * @param statementResult the result of a cypher statement run
	 */
	public GraphDatabaseResult(StatementResult statementResult) {
		_resultUuid = UUID.randomUUID().toString();
		_statementResult = statementResult;
	}

	/**
	 * Return the result's UUID.
	 *
	 * @return the result UUID
	 */
	public String getResultUuid() {
		return _resultUuid;
	}

	/**
	 * Returns the Neo4j {@link StatementResult} this class is wrapping.
	 *
	 * @return the Neo4j result object holding the execution result of a cypher run
	 */
	public StatementResult getStatementResult() {
		return _statementResult;
	}

	/**
	 * Returns the entire result stream. This wraps the {@link StatementResult#list()} method.
	 *
	 * @return the entire result stream
	 */
	public List<Record> list() {
		return _statementResult.list();
	}

	private String _resultUuid;
	private StatementResult _statementResult;

}