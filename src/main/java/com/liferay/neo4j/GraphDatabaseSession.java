/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.neo4j;

import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.TypeSystem;

import java.util.Map;
import java.util.Objects;

/**
 * @author Mate Thurzo
 */
public class GraphDatabaseSession implements Session {

	public GraphDatabaseSession(String uuid, Session session) {
		Objects.requireNonNull(uuid);
		Objects.requireNonNull(session);

		_uuid = uuid;
		_session = session;
	}

	@Override
	public Transaction beginTransaction() {
		return _session.beginTransaction();
	}

	@Override
	public Transaction beginTransaction(String bookmark) {
		return _session.beginTransaction(bookmark);
	}

	@Override
	public <T> T readTransaction(TransactionWork<T> work) {
		return _session.readTransaction(work);
	}

	@Override
	public <T> T writeTransaction(TransactionWork<T> work) {
		return _session.writeTransaction(work);
	}

	@Override
	public String lastBookmark() {
		return _session.lastBookmark();
	}

	@Override
	public void reset() {
		_session.reset();
	}

	@Override
	public boolean isOpen() {
		return _session.isOpen();
	}

	@Override
	public void close() {
		_session.close();
	}

	@Override
	public StatementResult run(String statementTemplate, Value parameters) {
		return _session.run(statementTemplate, parameters);
	}

	@Override
	public StatementResult run(String statementTemplate, Map<String, Object> statementParameters) {
		return _session.run(statementTemplate, statementParameters);
	}

	@Override
	public StatementResult run(String statementTemplate, Record statementParameters) {
		return _session.run(statementTemplate, statementParameters);
	}

	@Override
	public StatementResult run(String statementTemplate) {
		return _session.run(statementTemplate);
	}

	@Override
	public StatementResult run(Statement statement) {
		return _session.run(statement);
	}

	@Override
	public TypeSystem typeSystem() {
		return _session.typeSystem();
	}

	public String getUuid() {
		return _uuid;
	}

	private Session _session;
	private String _uuid;

}
