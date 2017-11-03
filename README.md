# liferay-neo4j-driver
OSGi compatible version of the Neo4j Java driver brought to you by Liferay.

This driver is wrapping the official Neo4j Java language driver by adding OSGi compatibility. The Neo4j driver is being included as-is, we haven't modified anything on it. The output of this project is an OSGi bundle that you can deploy to your OSGi container and use the driver.

Currently we are wrapping the Neo4j Java driver 1.4.4 version. For more information on the driver itself please visit the official  [Neo4j site](http://neo4j.com/).

# Features

* The bundle exports the org.neo4j.driver.v1 package and all underlying packages. On the contrary the bundle does __not__ export the internal packages (org.neo4j.driver.internal).
* A GraphDatabase declarative service is being registered to the OSGi service registry. GraphDatabase provides basic support to access your Neo4j instance and run simple queries against it.
* OSGi configuration support
* Minor utility features

# Planned features

* Continuously release new versions as the Neo4j driver version increases
* Provide a facility to get sessions or connections to multiple Neo4j instances and register them to the service registry
* More sophisticated session and connection management through component services
* Gradle build support

# Usage

* Clone this repository and call ```mvn clean install```
* Alternatively download the pre-built JAR file from this repository's target directory
* In you maven project add this as a dependency
```
<groupId>com.liferay.neo4j</groupId>
<artifactId>neo4j-osgi-driver</artifactId>
<version>1.1.2</version>
```
* In case you are using Liferay and developing a custom plugin, you can add this as a gradle dependency as well
```
compile 'com.liferay.neo4j:neo4j-osgi-driver:1.1.2'
```

Once you have the JAR file you can reference it as a compile time dependency. You can drop it to the OSGi container and the driver will be available for other bundles to use.

# OSGi service configuration in Liferay

Go to Control Panel > Configuration > System Settings. Select 'Other' tab and find 'Liferay Neo4j Service Configuration'.

Fill out fields according to your environment, like the following:

![Service configuration screenshot](/scr_config.png)

# Example code

```java
@Component(immediate = true, service = NeoTest.class)
public class NeoTest {

	@Modified
	@Activate
	public void activate() {

		// Example 1

		GraphDatabaseResult result = _graphDatabase.runStatement(
			"MATCH (n:TestNode) return n");

		result.recordStream().forEach((record) -> _log.info(record.fields()));


		// Example 2
		// Rider nodes have an attribute called name

		result = _graphDatabase.runStatement("MATCH (r:Rider) return r");

		// Prints each rider's name to the console

		result.recordStream().map(
			(record) -> record.get("r").asNode().get("name")
		).forEach(
			System.out::println
		);

		// Example 3
		// Standard Neo4j driver methods

		Session session = _graphDatabase.getSession();

		session.run("CREATE (r:Rider {number: 46}) return r");

		session.close();

		// Example 4
		// Autoclosing session

		session = _graphDatabase.getAutoclosingSession(10000);

		session.run("CREATE (r:Rider {number: 9}) return r");
	}

	@Reference
	private GraphDatabase _graphDatabase;
}
```

# Compatibility

* Java 1.8
* Neo4j Java Driver 1.4.4 and Neo4j Server 3.3
* Tested with Liferay 7.0 GA2
* For a sample Liferay 7.0 plugin with Neo4j support check out our [sample repository](https://github.com/danielkocsis/neo4j-sample-portlet)
