# Object mutability and copying

## Decision

Objects _may_ implement a `copy()` method, which creates a deep copy, and a `toBuilder()` method, which returns a
`Builder` that operates on the original instance.

## Rationale

Mutable objects may need to be modified over their lifetime. To keep in line with the builder pattern, those objects may
implement a `toBuilder()` method, that operates on the original instance thus making them mutable.

To avoid unexpected runtime behaviour, all `copy()` implementations must create a deep copy of the original instance.
This typically involves copying the entire object graph.

## Approach

For the `toBuilder()` method, the Builder must have a parameterized constructor, similar to how we already do it for
inherited builders. The new `Builder` operates/modifies the original instance.

```java
public class TheObject {

    // other methods

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private final TheObject instance;

        private Builder(TheObject instance) {
            this.instance = instance;
        }
        // other builder methods
    }
}  
```

The `copy()` method must create a copy of every field, potentially invoking `copy()` of its member objects. Note that
`AnotherObject#copy()` must also create a deep copy of `AnotherObject`. The original instance is left unmodified.

```java
public class TheObject {

    private String aString;
    private AnotherObject anotherObject;
    private int anInt;

    public TheObject copy() {
        return TheObject.Builder.newInstance()
                .aString(this.aString)
                .anotherObject(this.anotherObject.copy())
                .anInt(this.anInt)
                .build();
    }

    //Builder not shown
}
```

In the code base we currently have a bit of a mix of various implementations. These need to be harmonized as well.