# Data loading

The EDC provides a data loading extension that can be used to load `Asset`s or `ContractDefinition`s into the connector
and store them in a backing persistence store. Hereinafter, you will find an introduction to how the dataloading
extension works. The launcher then demonstrates how this can be used to load data from JSON files, but the inserted
data could just as well come from a different origin.

## The dataloading extension

Dataloading has been implemented for `Asset` and `DataAddress` objects (or actually `AssetEntry`, which is wrapper for
those two) and `ContractDefinition` objects as a first use case.

The `dataloading` extension contains 3 sub-modules: `dataloading-spi`, `dataloading-asset` and
`dataloading-contractdef`. The `dataloading-spi` module contains the common classes and interfaces required for any
type of data loading, while the two other modules contain classes and interfaces specific to loading either assets
or contract definitions.

### The DataLoader

Let's take a look at the `DataLoader`, which is the class performing the actual loading and is located in
`dataloading-spi`:

The `DataLoader` is a generic class, meaning an instance will always be specific to a certain type. Thus, the same
`DataLoader` cannot be used to load assets __and__ contract definitions. Designated `DataLoader` instances have to
be created for both use cases.

The class contains two fields: a `DataSink` (_sink_), and a collection of validation functions (_validationPredicates_),
which validate an instance of the generic type and return a
[`Result`](../../spi/core-spi/src/main/java/org/eclipse/dataspaceconnector/spi/result/Result.java). The
[`DataSink`](../../extensions/dataloading/dataloading-spi/src/main/java/org/eclipse/dataspaceconnector/dataloading/DataLoader.java)
interface is generic as well and will be used to persist the loaded data, therefore it contains just one method for
accepting a new entry of the specified type. The interface should be implemented by persistence stores so that they
may be used for dataloading.

The `DataLoader` offers two public methods for inserting new data: `insert` for inserting a single item and `insertAll`
for inserting a collection of items. In both methods, the data to insert is first validated using all
_validationPredicates_ functions. If any validation fails, an exception is raised, and __no__ data is persisted.
If the new data passes all validation functions, the data is inserted into the persistence store defined in the 
`sink` field.

```java
public void insert(T item) {
	// see that the item satisfies all predicates
	var failedValidations = validate(item).filter(ValidationResult::isInvalid)
			.collect(Collectors.toUnmodifiableList());

	// throw exception if item does not pass all validations
	if (!failedValidations.isEmpty()) {
		throw new ValidationException(failedValidations.stream().map(ValidationResult::getError).collect(Collectors.joining("; ")));
	}

	sink.accept(item);
}

public void insertAll(Collection<T> items) {

	var allValidationResults = items.stream().flatMap(this::validate);

	var errorMessages = allValidationResults.filter(ValidationResult::isInvalid).map(ValidationResult::getError).collect(Collectors.toList());

	if (!errorMessages.isEmpty()) {
		throw new ValidationException(String.join("; ", errorMessages));
	}

	items.forEach(sink::accept);
}
```

### AssetLoader and ContractDefinitionLoader

In the `dataloading-asset` and `dataloading-contractdef` modules you can find the `AssetLoader` and
`ContractDefinitionLoader` interfaces respectively. Both are sub-interfaces of `DataSink` and thus are implemented
by respective backing stores, so that these stores may be used for persisting loaded data.

The `AssetLoader` is currently implemented by the `CosmosAssetIndex` and the `InMemoryAssetIndex`, the
`ContractDefinitionLoader` is implemented anonymously in the `CosmosContractDefinitionStoreExtension`
using the `CosmosContractDefinitionStore` and then registered in the `ServiceExtensionContext`.

Consequently, a backing CosmosDB can be used to load assets as well as contract definitions. When using in-memory
stores instead, only asset loading is possible at the moment.

## Example: Dataloading CLI

The goal of this sample is to demonstrate how the dataloading feature may be used in the EDC. For this demonstration
the data will be loaded from JSON files using the command line, but of course the data could also come from a different
origin (e.g. sent by a user via a REST API).

### CLI implementation

This sample comes with a custom runtime implementation, the `DataLoaderRuntime`. This extends the EDC's `BaseRuntime`,
but performs additional operations depending on the command line arguments given when starting the connector. Also, the
`DataLoaderRuntime` requires an `AssetLoader` as well as an `ContractDefinitionLoader` implementation in the context.
If either one is missing, the application is shut down. This launcher's `build.gradle.kts` therefore includes - next
to the dataloading modules - the CosmosDB implementation of the `AssetIndex` as well as the `ContractDefinitionStore`:

```gradle
implementation(project(":extensions:dataloading"))

...

implementation(project(":extensions:azure:cosmos:assetindex-cosmos"))

...

implementation(project(":extensions:azure:cosmos:contract-definition-store-cosmos"))
```

When both stores are present during the boot process, a
[`LoadCommand`](./src/main/java/org/eclipse/dataspaceconnector/dataloader/cli/LoadCommand.java) is created using the
stores and the command line arguments and then executed using the _picocli_ library. The `LoadCommand` reads the file
specified in the command line arguments and, depending on whether assets or contracts were specified, parses the file
content to a collection of the respective type. This collection is then handed to the corresponding `DataSink`'s
`insertAll` method. Thus, the data from the file is persisted, if each entry passes the validation.

### Try it out

Before starting the connector that will load data from a file, a few prepartions have to be done:

1. By default, both the `AssetLoader` and `ContractDefinitionLoader` are configured to use CosmosDB. Please adapt
   the `build.gradle.kts` file to suit your particular needs.    If you do not have access to a CosmosDB, you can use
   the in-memory implementation instead (__note, that this is currently only supported for assets__).
2. In case you are using CosmosDB, you have to provide a custom `config.properties` file. A template for which
   properties are required can be found in the `resources` folder of this launcher.
3. Create a `*.json` file which contains assets or contract definitions. This file will be loaded by the connector and
   its entries will be persisted. Examples for both assets and contract definitions can be found in the `resources`
   folder of this launcher. For an example on how to generate these `*.json` files, have a look at the
   `JsonFileGenerator` under the `test/java/org/eclipse/dataspaceconnector/examples` directory of this launcher.

Once the preparation is complete, the connector can be built using gradle and then started. Depending on whether you
are using CosmosDB or in-memory stores, the `config.properties` file has to be supplied:

```bash
java -jar <path-to-jar> (--assets | --contracts) <path-to-file.json> 
```

```bash
java -jar <path-to-jar> (--assets | --contracts) <path-to-file.json> -Dedc.fs.config=/path/to/config.properties
```

__Note, that only one type of object can be loaded at a time. Therefore, specify either _--assets_ or _--contracts_.
In order to load more objects, just run the program again.__

### Outlook

Currently, only Asset/DataEntry and ContractDefinition objects are supported by the dataloading functionality. In the
future, this will be extended to include more types of objects. The implementation of the dataloading CLI might also
change, so that multiple object types may be loaded at once. If any enhancements are added or other changes take place,
this documentation will be updated accordingly.
