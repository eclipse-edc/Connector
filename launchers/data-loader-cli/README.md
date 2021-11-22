# CLI DataLoader tool

This is a command line tool that can be used to load data from a file (JSON, CSV,...)
and store it in a backing persistence system. As a first use case, we implemented loading
`Asset` and `DataAddress` objects (or actually `AssetEntry`, which is wrapper for those two) objects from a JSON file
and storing them using an `AssetLoader`.

For an example JSON file take a look at [this file](src/test/resources/assets.json).

## Synopsis:

```bash
java -jar <path-to-jar> --assets <path-to-file.json>
```

## A few things to notice

- by default an `AssetIndex` based on CosmosDB is configured. Please adapt the `build.gradle.kts` file to suit your
  particular needs! 
- Please add a `*.properties` file that contains the following properties. The app _will not_ function properly unless you do this.
  - `edc.vault.clientid=<az-clientid>`
  - `edc.vault.tenantid=<az-tenantid>`
  - `edc.vault.certificate=/path/to/cert.pfx`
  - `edc.vault.name=<vault-name>`
  - `edc.assetindex.cosmos.account-name=<cosmos-account>`
  - `edc.assetindex.cosmos.database-name=<cosmos-db-name>`
  - `edc.cosmos.partition-key=<partition-key>`
  - `edc.assetindex.cosmos.preferred-region=westeurope`
  - `edc.assetindex.cosmos.container-name=<container-name>`
  - `edc.cosmos.query-metrics-enabled=true`
- The commandline tool is intended to run as standalone program
- Currently only Asset/DataEntry objects are supported, more commands will follow. This might change the synopsis of the
  tool.
