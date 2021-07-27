# How the refactored structure works


## The `core` module
Contains all absolutely essential building that is necessary to run a connector such as `TransferProcessManager`, 
`ProvisionManager`, `DataFlowManager`, various model classes, the protocol engine and the security piece.

This also exposes all extension points via interfaces in the `spi` submodule.

While it is possible to build a connector with just the code from the `edc` module, it will have very limited capabilities
to communicate and to interact with a data space.

## The `minimal` module
Provides basic low-complexity implementations of many of the extension points in order to get a connector up-and-running quickly. 
Implementations in this module will be okay for developing/testing but shouldn't be used in enterprise-grade 
production environments. 

Examples include: 
- a file-based configuration module
- a file-based secret vault
- a memory-based transfer process store

All the IDS modules are also located here because they are not strictly speaking required to operate a connector. 
However, most actual implementations will likely include them.


## The `common` module
Contains utility code such as collection utils, string utils and helper classes to access Azure Blob Store.

## The `extensions` module
This contains code that extends the connector's core functionality with technology- or cloud-provider-specific code. 
For example a transfer process store based on Azure CosmosDB, a secure vault based on Azure KeyVault, etc.
Basically, this contains more production-ready alternatives to the `minimal` module. This is where technology- and cloud-specific 
implementations should go. If you wish to contribute, say, a DB2-database extension - here's where it should go.

For example, if someone where to create a configuration service based on a Postgres database, then the implementation should go into
a submodule of the `extensions` module.

## The `samples` module
This is similar to the `extensions` module, but focuses more on showing specific use cases rather than technology extensions. 
For example, it shows how to run a connector from a unit test in order to try out functionality quickly or how to implement an
outward-facing REST API for a connector.