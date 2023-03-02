# Refactoring of the EDC entity stores

## Decision

The connector's entity stores will be refactored in the following aspects:

- all methods that modify the entity (`save`, `update`, `delete`) must return a `Result` (or an appropriate subclass)
- "upsert" semantics will be removed
- the explicitly passed entity ID will be removed from `update()` methods

## Rationale

Explicitly returning a `Result` makes the success or failure much more readable and clear than returning `null` or the
object. Users would almost always have to consult the documentation to learn the exact semantics. With a `Result`, that
becomes inherently obvious.

Similarly, although the merits of "upsert" semantics is certainly debatable, it is much more intuitive to either "
create" _or_ "update" an object. It also avoids unintended object creation, and puts the control over what is being
created back in the caller's purview.

Finally, having the explicit object ID together with the object itself in a method signature is deemed spurious and not
necessary. It should be pointed out, that - at the (REST) API level - passing the object ID separately remains very much
in place, but at the service level the method should just be e.g. `update(Asset asset)`.

In addition to being superfluous, having the object ID as a separate argument would arguably increase the amount of
checks
(and thus: tests) necessary at potentially multiple levels: equality, non-nullity, etc.

## Approach

### Add `Result` as return value

For this, it may be necessary to re-use/extend/rename the `ServiceResult` and utilize that, or devise a similar class.

- `create()`: return `Result.success(newObject)` if not exists, otherwise return `Result.conflict(message)`.
- `update()`: return `Result.success(updatedObject)` if exists, otherwise return `Result.notFound(message)`.
- `delete()`: return `Result.success(deletedObject)` if exists, otherwise return `Result.notFound(message)`.

All other business-related checks are to be performed at the service level.

### Remove `"upsert"` semantics

Some stores currently implement upsert semantics, which blurs the lines somewhat, so we should remove it. Thus, as
stated in the previous section the following semantic will apply:

- create: fail if already exists
- update: fail if _not_ exists

### Remove separate entity ID

While API layers may decide to accept the entity ID and payload in different objects, for example to make HTTP
parameters easily usable in REST, at the service layer and below we only accept the entity itself. That means that any
object transformation must take care of "merging" the ID and the payload into the actual entity.

### A word on entity validation

- API layer: formal object validation (`id != null`, etc. ) is performed by DTO validation through `jakarta.validation`
  annotations. Any API must guarantee formally valid objects.
- service layer: we can therefor limit validation to business rules, e.g. that an Asset can only be deleted if it
  is not yet referenced by a contract, etc.
- persistence layer: at the store layer we can perform logical validation: for example we can only update an entity if
  it already exists, or we can only create an object if it _doesn't_ yet exist. 