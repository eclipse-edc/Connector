# Deploying Gaia-X Data Components for Apache Atlas

The data components for using Gaia-X with Apache atlas include Atlas Types and Classifications used to store datasets and manage their governance using Atlas.

This sample project creates the Types and Classifications needed to use Gaia-X with Atlas. It also creates some sample Entities and executes some sample queries to verify that the setup completed successfully. Finally, the code issues some queries against the data to ensure that everything is set up correctly before optionally deleting the elements that it created.

In order to build the project, you will need [Maven](https://search.maven.org/) and [JDK8](https://openjdk.java.net/projects/jdk8/) or later.

### Building the Code

To build the sample code, use the command

```bash
mvn package
```

### Configuring the Code

To configure the code, modify [atlas-application.properties](./sample-code/src/main/resources/atlas-application.properties) in the resources folder as follows:

```bash
atlas.rest.address=http://YOUR_ATLAS_CLUSTER_IP_OR_FQDN:21000

atlas.account.username=YOUR_ATLAS_USERNAME
atlas.account.password=YOUR_ATLAS_PASSWORD

atlas.cleandata=true

```

When atlas.cleandata is set to true, it will delete everything that was created after successfully querying the sample data.

Note that there are three configuration files that contain the [Types](./sample-code/src/main/resources/quick_start_types.json), [Classifications](./sample-code/src/main/resources/quick_start_classifications.json), and [Entities](./sample-code/src/main/resources/quick_start_entities.json) that the code deploys.

If you do not want to deploy sample entities, simply set the entities property to an empty array.

### Running the Code

To run the code, use the following command:

```bash
mvn exec:java -Dexec.mainClass="com.atlas.example.AtlasExample"
```
