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

/**
 * @author Mate Thurzo
 */
@Meta.OCD(
    id = "com.liferay.neo4j.configuration.GraphDatabaseConfiguration",
    name = "Liferay Neo4j Service Configuration"
)
public interface GraphDatabaseConfiguration {

    @Meta.AD(deflt = "neo4j", required = true)
    public String userName();

    @Meta.AD(deflt = "neo4j", required = true)
    public String password();

    @Meta.AD(deflt = "localhost", required = true)
    public String url();

    @Meta.AD(deflt = "7687", required = true)
    public long port();

}
