# Decision Record Private Properties

## Decision

---
Extend the following entities with an additional `privateProperties` map,
that enables the saving of information only accessible through the Data Management API.
- `Asset`

## Rationale

---
Enriching an `Asset` with information not relevant for
external consumers/providers is currently not possible.
To achieve this, it has to be done through an external storage.
A prominent use-case for `privateProperties` would be internal tags, that help with filtering `Assets` when it comes
to selecting a policy.

## Affected Areas

* Data-Management-API for Assets (Models, Transformers)
* SPI for Assets (Core Datatype)
* In-Memory-Storage for Assets (Storage, Manipulation and Filtering)
* SQL Storage for Assets (Storage, Manipulation and Filtering)
* Cosmos Storage for Assets (Storage, Manipulation and Filtering)

## Approach

---

### SPI
Since the `Asset` file inside the SPI is the internal representation of the `Asset` entity,
the first, fundamental change happens here.

The first step is to actually extend the `Asset` is to define a new map `privateProperties` containing the private properties.

Next, the ``Asset`` constructor needs to initialise the new `privateProperties` map.

Reading of the added `privateProperties` is achieved through the methods `getPrivateProperties`, `getPrivateProperty` and `getPrivatePropertyAsString`.

Besides the Constructor, the `Builder` also needs to reflect the changes in the `Asset` and allows the addition of a whole map with `privateProperties(Map<String, Object> privateProperties)`
or just a single key-value-pair with `privateProperty(String key, Object value)`.

### Models
Since the change decided in this decision record affects the data structure of the 'Asset' entity,
it will also affect the models representing the entity to the outside during API calls.

##### AssetRequestDto

First the abstract class `AssetRequestDto` needs to be adjusted.

Here the `privateProperties` map is added to the model together with a `@NotNull` constraint.

The `Builder` creating the `AssetRequestDto` also needs to be able to set the `privateProperties` with the help of a similarly named method.

Both `AssetUpdateRequestDto` and `AssetCreationRequestDto` inherit from this class  so these changes will propagate to them.

#### AssetUpdateRequestDto

Analog to the check for empty property keys in `AssetUpdateRequestDto` we also need to check for empty private property keys.

To reflect the behaviour of the SQL implementation, there is an additional check for identical keys in both `properties` and `privateProperties`.

Additionally, a method named `getPrivateProperties` is needed to access the `privateProperties` of the `AssetUpdateRequestDto`.

#### AssetCreationRequestDto

The changes inside `AssetCreationRequestDto` are duplicates of the changes inside `AssetUpdateRequestDto`.

A question that arises here is, if it would be possible to refactor both classes
and move the changes, including the original code for the `properties` map, to the
parent class `AssetRequestDto` they both inherit from.

#### AssetResponseDto
Three changes are necessary inside the `AssetResponseDto`.

First the map for the `privateProperties` needs to be created.

Next, a get method ``getPrivateProperties`` allows access to the map.

Finally, the `Builder` is extended to enable setting the `privateProperties` map during initialisation.

### Transformer
The `AssetRequestDtoToAssetTransformer` needs transform `privateProperties` from
`AssetRequestDto` to the in-memory `Asset`.

A corresponding call to the `privateProperties` method of the `Builder` solves this issue.

An identical change needs to be made for `AssetToAssetResponseDtoTransformer`,
`AssetUpdateRequestWrapperDtoToAssetTransformer`,
`DataAddressDtoToDataAddressTransformer` and `DataAddressToDataAddressDtoTransformer`.

### Database
To allow `privateProperties` to be used in the intended way, it needs to be possible to
query `Assets` with `privateProerties` as filter and to sort with the help of `privateProperties`.
In the original implementation of the `InMemoryAssetIndex` the filtering is achieved by using predicates.
The given criteria inside the `QuerySpec` are converted into predicates through the `AssetPredicateConverter`.

##### filtering
By adjusting the `AssetPredicateConverter` to search both properties and private properties, filtering for private properties
is achieved without any adjustments to the `QuerySpec`.

It is important to note, that the implementation searches the `properties` map first and only afterwards looks at the
`privateProperties` map.

##### sorting
Finally, the `queryAsset` method inside `InMemoryAssetInedex` is extended with sorting for `privateProperties` if no matching keys inside the properties are found.

#### SQL
To enable the distinction of `properties` and `privateProperties`  inside the postgresql database,
the ``schema`` of the `edc_asset_property` table is extended by a boolean value `property_is_private`, that marks private properties.

With this addition it is necessary, that a template string gets added inside the `AssetStatements`, which contains the name of the added value.
The Method ``getAssetPropertyColumnIsPrivate`` serves as getter for the string.

This Method is then used inside `BaseSqlDialectStatements` to generate a string template for a sql-statement inside the method `getInsertPropertyTemplate`.

With the addition of `privateProperties` a wrapper is needed to ensure no information is lost during the mapping of result sets from the database to java objects.
Through the ``SqlPropertyWrapper`` class, the boolean value `isPrivate` is encapsulated together with the property.

The ``SqlAssetIndex`` uses the `SqlPropertyWrapper` class to map the result set of the `edc_asset_property` with the method `mapPropertyResultSet`.

`isPrivate` of the ``SqlPropertyWrapper`` is then used  to filter the result set inside `findById`.
After filtering, the found ``properties`` and `privateProperties` are then added to their respective maps inside the `Asset` object that is returned.

Both methods `accept` and `updateAsset` inside ``SqlAssetIndex`` need to be adjusted to reflect then changes inside the `edc_asset_property` table.
To achieve this, the ``properties`` and `privateProperties` maps of the `Asset` object are read and added to the table with the `isPrivate` value set accordingly.
The change is identical in both `accept` and `updateAsset`.

#### Cosmos
The changes to include `privateProperties` inside Cosmos DB storage focus on the `AssetDocument` class.

Here a `privateProperties` map is added as attribute to `AssetDocument`. 

Analogous to the treatment of the existing `properties`, `privateProperties` are first sanitizes with the ``sanitizePrivateProperties`` method when getting stored.

In turn, the `privateProperties` are then unsanitized again with `restorePrivateProperties` when being read through the `getWrappedAsset` method.

### Privacy Protection
To ensure that `privateProperties` stay private, the transformers for the IDS data-protocol
will not be updated for the `privateProperties`.
This way, only the Data Management API will have access.