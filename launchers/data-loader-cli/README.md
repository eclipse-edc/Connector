# CLI DataLoader tool

This is a command line tool that can be used to load data from a file (JSON, CSV,...)
and store it in a backing persistence system. As a first use case, we implemented loading
`Asset` and `DataAddress` objects (or actually `AssetEntry`, which is wrapper for those two) objects from a JSON file
and storing them using an `AssetLoader`.

For an example JSON file take a look at [this file](src/test/resources/assets.json).

## Synopsis:

```bash
java -jar <path-to-jar> (--assets | --contracts) <path-to-file.json> 
```

depending on our concrete build, it may be necessary to supply a config file, thus specifying
the `Dedc.fs.config=/path/to/config.propertes` parameter may be necessary.

## A few things to notice

- by default both the `AssetLoader` and `ContractDefinitionLoader` are configured to use CosmosDB. Please adapt
  the `build.gradle.kts` file to suit your particular needs!
- Please add a `*.properties` file that contains the following properties. The app _will not_ function properly unless
  you do this.
    - `edc.vault.clientid=<az-clientid>`
    - `edc.vault.tenantid=<az-tenantid>`
    - `edc.vault.certificate=/path/to/cert.pfx`
    - `edc.vault.name=<vault-name>`
    - `edc.cosmos.query-metrics-enabled=true`
    - `edc.cosmos.partition-key=<partition-key>`

- The following entries must be substituted **BOTH** for `assetindex` and `contractdefinitionstore`. So you'll
  effectively add **eight** entries in total!
    - `edc.SUBSTITUTE.cosmos.account-name=<cosmos-account>`
    - `edc.SUBSTITUTE.cosmos.database-name=<cosmos-db-name>`
    - `edc.SUBSTITUTE.cosmos.preferred-region=westeurope`
    - `edc.SUBSTITUTE.cosmos.container-name=<container-name>`

- The commandline tool is intended to run as standalone program
- Only one type of objects can be loaded at a time. Run the program again if you with to load more object types.
- Currently, only Asset/DataEntry and ContractDefinition objects are supported, more commands will follow. This might
  change the synopsis of the tool.
