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

package com.liferay.neo4j.configuration;

import aQute.bnd.annotation.metatype.Meta;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * @author Mate Thurzo
 */
@ObjectClassDefinition(
	id = "com.liferay.neo4j.configuration.GraphDatabaseConfiguration", name = "Liferay Neo4j Service Configuration"
)
public @interface GraphDatabaseConfiguration {

	@Meta.AD(deflt = "neo4j", required = true)
	public String userName() default "neo4j";

	@Meta.AD(deflt = "neo4j", required = true)
	public String password() default "neo4j";

	@Meta.AD(deflt = "localhost", required = true)
	public String hostname() default "localhost";

	@Meta.AD(deflt = "7687", required = true)
	public long port() default 7687;

	@Meta.AD(deflt = "50", required = false)
	public int connectionPoolSize() default 50;

	@Meta.AD(deflt = "data/neo4j/default")
	public String embeddedDatabasePath() default "data/neo4j/default";

}
