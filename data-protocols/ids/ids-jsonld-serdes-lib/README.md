# IDS JSON-LD Serializer & Deserializer Library

This library provides implementations for (de-)serialization of IDS Infomodel objects. The `Jsonld`
class provides a customized `ObjectMapper` to serialize and deserialize JSON-LD strings.

## Background

Data-describing ontologies, as e.g. the Information Model of the International Data Spaces (IDS) are
often expressed in [RDF](https://www.w3.org/RDF/). RDF is characterized by its ability to describe a
linkage between objects while addressing the exact types of each attribute. There are different ways
to technically map and process RDF. One of them is the use of [JSON-LD](https://json-ld.org/).

A popular Java library for the de/serialization of JSON objects is the Jackson `ObjectMapper`. However,
since type information is lost in JSON, objects and attributes need to be serialized in JSON-LD format.
This requires some adjustments, e.g. the addition of context and type information.

### Scope

This extension can be used for serializing and deserializing JSON-LD Strings across the whole project
by registering the customized `ObjectMapper` to the EDC `TypeManager`.

```java
var typeManager = new TypeManager();
typeManager.registerContext("ids", JsonLdService.getObjectMapper());

TypeManagerUtil.registerIdsClasses(typeManager);

return typeManager.getMapper("ids");
```

### Use Cases

This extension is used in the `:data-protocols:ids` modules, as IDS communication protocols, as of now,
require the support of JSON-LD when communicating with other systems in an IDS ecosystem.

## Technical Details

### Interfaces

The `JsonldService` provides a customized `ObjectMapper` that can be used as-is, or be modified.
It comes with methods that can process custom context information or subtypes.

Classes, that should use the `JsonLdSerializer` need to be registered as in the following:

```java
typeManager.registerSerializer("ids", Artifact.class, new JsonLdSerializer<>(Artifact.class, IdsConstants.CONTEXT));
```
