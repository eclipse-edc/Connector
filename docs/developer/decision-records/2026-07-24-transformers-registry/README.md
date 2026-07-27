# Change the type-transformer registry from a list to a map

## Decision

`TypeTransformerRegistryImpl` will store registered transformers in a map rather than a list.

The registry will use a nested map, keyed first by input type and then by output type:

```java
Map<Class<?>, Map<Class<?>, TypeTransformer<?, ?>>> transformers
```
A registration is identified by the pair formed by `TypeTransformer#getInputType()` and
`TypeTransformer#getOutputType()`. Registering a transformer with an existing pair replaces the
previous transformer for that pair. The last registration therefore wins.

Registries created with `forContext` remain isolated from their parent registries. A context
registry first resolves its own transformer and falls back to its parent only when it has no match,
so a context-specific registration can override a default registration without changing the
parent registry.

## Rationale

The registry currently stores transformers in an `ArrayList` and returns a compatible transformer
during lookup using `findAny()`.

Registering a transformer for a pair already handled by a default transformer does not reliably
replace the default; the default remains eligible for selection. The selected transformer depends
on the registry's encounter order, which is an implicit and unreliable override mechanism.

The map-based implementation ensures that the last registration for a given input/output pair is
the one returned by `transformerFor`. To override a default transformer, an extension must
register its transformer for the same pair after the default transformer has been registered.

## Approach

1. Replace the list in `TypeTransformerRegistryImpl` with a map keyed by registered input and
	output types.
2. Make `register` replace the entry for an already registered type pair.
3. Update `transformerFor` to retain compatible-supertype matching and parent-context fallback.
   Conceptually, lookup will follow this logic:

   ```java
   var candidates = registeredInputTypes.stream()
	   .filter(inputType -> inputType.isInstance(input))
	   .filter(inputType -> hasTransformer(inputType, outputType))
	   .toList();

   var mostSpecific = candidates.stream()
	   .filter(candidate -> candidates.stream()
		   .noneMatch(other -> !candidate.equals(other)
			   && candidate.isAssignableFrom(other)))
	   .toList();

   if (mostSpecific.size() > 1) {
       throw new EdcException("Ambiguous transformers");
   }

   return mostSpecific.isEmpty() ? parentTransformer() : transformerFor(mostSpecific.getFirst());
   ```
4. Add tests for same-registry replacement, context override, parent fallback, and supertype
   matching, including ambiguous compatible types.

The public `TypeTransformerRegistry` API remains unchanged.

### Consequences

1. A later registration for the same input/output pair intentionally replaces the earlier one.
2. Extensions can override defaults by registering in the applicable registry context.
3. When several compatible input types match, the registry selects the subtype that is more specific
	than the others. If several matching input types are unrelated and therefore equally specific,
	the lookup fails with an `EdcException`.
	For example, `List -> Output` is selected over `Collection -> Output` for an `ArrayList` input.
	Conversely, an input implementing both `Serializable` and `Comparable` classes causes an `EdcException`
	when transformers are registered for both types and the same output.


### Target modules and mapping

| Module | Change |
| --- | --- |
| `core:common:lib:jsonld-lib` | Replace storage and lookup in `TypeTransformerRegistryImpl`. |
| `core:common:lib:jsonld-lib` | Extend `TypeTransformerRegistryImplTest` with replacement and lookup coverage. |
| `spi:core-spi` | No API change; retain the `TypeTransformerRegistry` contract. |