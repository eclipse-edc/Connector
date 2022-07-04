# Type Manager Support for Serialization Configuration

## Decision

As the EDC should integrate into several deployments and (possibly domain-specific) use cases, the
EDC runtime requires the ability to support the configuration of multiple serialization contexts.
This should be provided by the `TypeManager`:

- A default context will define base configuration and be inherited by all other contexts. The existing
  `TypeManager.getObjectMapper()` will return the default context.

- Serialization contexts will be created by invoking `TypeManager.getObjectMapper(String)`. If a context
  is not already present, one will be created; otherwise the existing one will be returned. The returned
  `ObjectMapper` can have configuration applied to it during runtime initialization.

- The `TypeManager` will track created contexts and `ObjectMapper` instances. This will allow type
  registrations with the default context to be propagated to other `ObjectMappers` after they have been
  created.

## Rationale

The EDC runtime requires the ability to support configuration of multiple serialization contexts. For
example, an API may require different serialization characteristics than another API. Related to this,
users may want to serialize additional metadata such as JSON-LD for certain types such as extensible
`Asset` properties or IDS messages.

## Approach

### Registering a serializer

Serializers can be registered as follows:

```
    typeManager.registerSerializer("foo", Bar.class, new JsonSerializer<>() {
        @Override
        public void serialize(Bar value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
            // handle serialization
        }
    });

    // ...

    var fooMapper = manager.getMapper("foo");
    var serialized = fooMapper.writeValueAsString(new Bar());
```

In the above example, a custom serializer is registered for the `Bar` type in the `foo` context.

### Writing a Decorating Serializer

A common use case for serialization contexts will be to decorate a serialized type with metadata.
The following is an example of a serializer that can be registered with the `TypeManager` to add
`@context` information for specific types. Please note that this example is user code and would
__not__ be provided by the EDC:

```
    public class DecoratingSerializer<T> extends JsonSerializer<T> {
        private Class<T> type;

        public DecoratingSerializer(Class<T> type) {

            this.type = type;
        }

        public void serialize(Object value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeStartObject();
            var javaType = provider.constructType(type);
            var beanDescription = provider.getConfig().introspect(javaType);
            var staticTyping = provider.isEnabled(MapperFeature.USE_STATIC_TYPING);
            var serializer = BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDescription, staticTyping);
            serializer.unwrappingSerializer(null).serialize(value, generator, provider);
            generator.writeObjectField("@context", "some data");
            generator.writeEndObject();
        }
    }
```

The serializer can be registered for types as follows:

```
    manager.registerSerializer("foo", Bar.class, new DecoratingSerializer<>(Bar.class));
    manager.registerSerializer("foo", Baz.class, new DecoratingSerializer<>(Baz.class));
```
