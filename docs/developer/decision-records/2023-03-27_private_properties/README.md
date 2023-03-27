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


## Approach

---
Example approach on the basis of the `Asset`.

### SPI
Since the `Asset` file inside the SPI is the internal representation of the `Asset` entity,
the first, fundamental change happens here.

The first step is to actually extend the `Asset` is to define a new map containing the private properties.

`private final Map<String, Object> privateProperties;`

Next, the constructor needs to initialise the new map.
```java
protected Asset() {
    properties = new HashMap<>();
    privateProperties = new HashMap<>();
}
```
Reading of the added `privateProperties` is achieved through the following methods.
No JSON Ignore

```java
public Map<String, Object> getPrivateProperties() {
    return privateProperties;
}

public Object getPrivateProperty(String key) {
    return privateProperties.get(key);
}

private String getPrivatePropertyAsString(String key) {
    var val = getPrivateProperty(key);
    return val != null ? val.toString() : null;
}
```

Afterwards, the `Builder` needs to reflect the changes in the `Asset`
```java        
public B privateProperties(Map<String, Object> privateProperties) {
    Objects.requireNonNull(privateProperties);
    entity.privateProperties.putAll(privateProperties);
    return self();
}

public B privateProperty(String key, Object value) {
    entity.privateProperties.put(key, value);
    return self();
}
```

### Models
Since the change decided in this decision record affects the data structure of the 'Asset' entity,
it will also affect the models representing the entity to the outside during API calls.

##### AssetRequestDto

First the abstract class `AssetRequestDto` needs to be adjusted.

This code adds the `privateProperties` map to the model.
```java
@NotNull(message = "privateProperties cannot be null")
protected Map<String, Object> privateProperties;
```

The `Builder` creating the `AssetRequestDto` also needs to be able to set the `privateProperties`.
```java
public B privateProperties(Map<String, Object> privateProperties) {
    dto.privateProperties = privateProperties;
    return self();
}
```

Both `AssetUpdateRequestDto` and `AssetCreationRequestDto` inherit from this class
so these changes will propagate to them.

#### AssetUpdateRequestDto

Analog to the check for empty property keys in `AssetUpdateRequestDto`
we also need to check for empty private property keys.

```java
@JsonIgnore
@AssertTrue(message = "no empty property keys")
public boolean isValid() {
    boolean validPrivate = privateProperties != null && privateProperties.keySet().stream().noneMatch(it -> it == null || it.isBlank());
    boolean validPublic = properties != null && properties.keySet().stream().noneMatch(it -> it == null || it.isBlank());
    return validPrivate && validPublic
}
```
Additionally, a Method is needed to access the `privateProperties` of the `AssetUpdateRequestDto`.

```java
public Map<String, Object> getPrivateProperties() {
    return privateProperties;
}
```

#### AssetCreationRequestDto

The changes inside `AssetCreationRequestDto` are duplicates of the changes inside
`AssetUpdateRequestDto`.

A question that arises here is, if it would be possible to refactor both classes
and move the changes, including the original code for the `properties` map, to the
parent class `AssetRequestDto` they both inherit from.

#### AssetResponseDto
Three changes are necessary inside the `AssetResponseDto`.

First the map for the `privateProperties` needs to be created.
```java
private Map<String, Object> privateProperties;
```

Next, a get method allows access to the map.
```java    
public Map<String, Object> getPrivateProperties() {
    return privateProperties;
}
```

Finally, the `Builder` is extended to enable setting the `privateProperties` map during initialisation.
```java  
public Builder privateProperties(Map<String, Object> privateProperties) {
    dto.privateProperties = privateProperties;
    return this;
}
```

### Transformer
The `AssetRequestDtoToAssetTransformer` needs transfer `privateProperties` from
`AssetRequestDto` to the in-memory `Asset`.

```java  
@Override
public @Nullable Asset transform(@NotNull AssetCreationRequestDto object, @NotNull TransformerContext context) {
    return Asset.Builder.newInstance()
            .id(object.getId())
            .properties(object.getProperties())
            .privateProperties(object.getPrivateProperties())
            .build();
}
```

An identical change needs to be made for `AssetToAssetResponseDtoTransformer`,
`AssetUpdateRequestWrapperDtoToAssetTransformer`,
`DataAddressDtoToDataAddressTransformer` and `DataAddressToDataAddressDtoTransformer`.

### Database
#### Storage


#### Query
To allow `privateProperties` to be used in the intended way, it needs to be possible to
query `Assets` with `privateProerties` as filter.



### Privacy Protection
To ensure that `privateProperties` stay private, the transformers for the IDS data-protocol
will not be updated for the `privateProperties`.
This way, only the Data Management API will have access.

### Testing