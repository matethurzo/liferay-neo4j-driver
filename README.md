# liferay-neo4j-driver
OSGi compatible version of the Neo4j Java driver brought to you by Liferay.

This driver is wrapping the official Neo4j Java language driver by adding OSGi compatibility. The output of this project is an OSGi bundle that you can deploy to your OSGi container and use the driver.

Currently we are wrapping the Neo4j Java driver 1.0.1 version. For more information on the driver itself please visit the official  [Neo4j site](http://neo4j.com/).

# Features

* The bundle exports the org.neo4j.driver.v1 package and all underlying packages. On the contrary the bundle does __not__ export the internal packages (org.neo4j.driver.internal).
* A GraphDatabase declarative service is being registered to the OSGi service registry. GraphDatabase provides basic support to access your Neo4j instance and run simple queries against it.

# Planned features

* Continuously release new versions as the Neo4j driver version increases
* Provide a facility to get sessions or connections to multiple Neo4j instances and register them to the service registry
* Gradle build support

# Usage

* Clone this repository and call ```mvn clean install```
* Alternatively download the pre-built JAR file from this repository's target directory

# Compatibility

* Java 1.8
* Neo4j Java Driver 1.0.1 and Neo4j Server 3.0
* Tested with Liferay 7.0 GA2
