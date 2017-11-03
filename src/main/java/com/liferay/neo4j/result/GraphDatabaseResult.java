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

package com.liferay.neo4j.result;

import com.liferay.neo4j.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Entity;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.neo4j.driver.v1.util.Function;
import org.neo4j.driver.v1.util.Pair;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
	 * Returns a stream of <code>Record</code> objects. These records are the actual result of a previous statement run.
	 *
	 * @return a stream of records as a result of a statement run.
	 */
	public Stream<Record> recordStream() {
		long estimate = 10L;

		if (!_statementResult.hasNext()) {
			estimate = 0L;
		}

		final Supplier<Record> recordSupplier = () -> {
			if (_statementResult.hasNext()) {
				return _statementResult.next();
			}
			else {
				return _TERMINAL_RECORD;
			}
		};

		return StreamSupport.stream(
			new Spliterators.AbstractSpliterator<Record>(estimate, Spliterator.SIZED | Spliterator.NONNULL) {

				@Override
				public boolean tryAdvance(Consumer<? super Record> action) {
					_processOnBeforeNextResult();

					Record record = recordSupplier.get();

					if (record == _TERMINAL_RECORD) {
						_processOnExhaustResult();

						return false;
					}

					return true;
				}
			},
			false);
	}

	/**
	 * Adds an event handler to run when the result set is exhausted.
	 *
	 * @param eventHandler a <code>GraphDatabaseResultEventHandler</code> to run when the result set is exhausted
	 */
	public void onExhaustResult(GraphDatabaseResultEventHandler eventHandler) {
		_onExhaustEventHandlers.add(eventHandler);
	}

	/**
	 * Adds an event handler to run before retrieving the next result in the result set.
	 *
	 * @param eventHandler a <code>GraphDatabaseResultEventHandler</code> to run before retrieving the next result in a
	 *                     result set
	 */
	public void onBeforeNextResult(GraphDatabaseResultEventHandler eventHandler) {
		_onBeforeNextEventHandlers.add(eventHandler);
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
	 * Returns the entire result stream. This wraps the {@link StatementResult#list()} method. This method is exhausting
	 * the result set.
	 *
	 * @return the entire result stream
	 */
	public List<Record> list() {
		List<Record> records = _statementResult.list();

		_processOnExhaustResult();

		return records;
	}

	private void _processOnExhaustResult() {
		_onExhaustEventHandlers.stream().forEach((eh) -> eh.handle());
	}

	private void _processOnBeforeNextResult() {
		_onBeforeNextEventHandlers.stream().forEach((eh) -> eh.handle());
	}

	private List<GraphDatabaseResultEventHandler> _onBeforeNextEventHandlers = new ArrayList<>();
	private List<GraphDatabaseResultEventHandler> _onExhaustEventHandlers = new ArrayList<>();
	private String _resultUuid;
	private StatementResult _statementResult;
	private static Record _TERMINAL_RECORD = new TerminalRecord();

	private static final class TerminalRecord implements Record {

		@Override
		public List<String> keys() {
			return null;
		}

		@Override
		public List<Value> values() {
			return null;
		}

		@Override
		public boolean containsKey(String key) {
			return false;
		}

		@Override
		public int index(String key) {
			return 0;
		}

		@Override
		public Value get(String key) {
			return null;
		}

		@Override
		public Value get(int index) {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public Map<String, Object> asMap() {
			return null;
		}

		@Override
		public <T> Map<String, T> asMap(Function<Value, T> mapper) {
			return null;
		}

		@Override
		public List<Pair<String, Value>> fields() {
			return null;
		}

		@Override
		public Value get(String key, Value defaultValue) {
			return null;
		}

		@Override
		public Object get(String key, Object defaultValue) {
			return null;
		}

		@Override
		public Number get(String key, Number defaultValue) {
			return null;
		}

		@Override
		public Entity get(String key, Entity defaultValue) {
			return null;
		}

		@Override
		public Node get(String key, Node defaultValue) {
			return null;
		}

		@Override
		public Path get(String key, Path defaultValue) {
			return null;
		}

		@Override
		public Relationship get(String key, Relationship defaultValue) {
			return null;
		}

		@Override
		public List<Object> get(String key, List<Object> defaultValue) {
			return null;
		}

		@Override
		public <T> List<T> get(String key, List<T> defaultValue, Function<Value, T> mapFunc) {
			return null;
		}

		@Override
		public Map<String, Object> get(String key, Map<String, Object> defaultValue) {
			return null;
		}

		@Override
		public <T> Map<String, T> get(String key, Map<String, T> defaultValue, Function<Value, T> mapFunc) {
			return null;
		}

		@Override
		public int get(String key, int defaultValue) {
			return 0;
		}

		@Override
		public long get(String key, long defaultValue) {
			return 0;
		}

		@Override
		public boolean get(String key, boolean defaultValue) {
			return false;
		}

		@Override
		public String get(String key, String defaultValue) {
			return null;
		}

		@Override
		public float get(String key, float defaultValue) {
			return 0;
		}

		@Override
		public double get(String key, double defaultValue) {
			return 0;
		}
	}

}