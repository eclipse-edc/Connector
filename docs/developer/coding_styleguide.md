# Coding styleguide

This document outlines some of the principles in the EDC code base. Most of them don't have a strong technical reasoning
but merely contribute to code that is homogenous, easily readable and coherent.

Not all of these rules can be enforced by a strict standard such as "never use X" or "always do Y", sometimes whether a
rule was violated or not is determined using a reviewer's best judgement.

That and the fact that custom Checkstyle rules are cumbersome to set up is the reason why this document exists.

## Avoid using `final`

Declaring method parameters, local variables etc `final` offers little benefit but makes the code more convoluted and
less readable. However, declaring class members `final` is OK.

## Use `default` methods sparingly

Default methods on interfaces can be useful for backwards compatibility, for declaring functional interfaces and for
default behaviour, for example having one overload call the other with default arguments.

In most cases it is better to avoid declaring the interface method `default` and have implementors of the interface
declare empty methods, than to declare all interface's methods as `default`.

As a rule of thumb there should be a good reason to declare a method as `default`, rather than the opposite. Those
reasons include:

- backwards compatibility
- allow an interface to be a `@FunctionalInterface`
- implement default behavior
- allow for optional implementation of methods

## When to use `abstract` classes

We use abstract classes when we want to share common properties and functionality between subclasses. Good examples for
this are base classes like `Command` or `Event`. Be aware though that using abstract classes to model extension points
will severely limit the inheritor's modelling possibilities, as Java does not have multiple inheritance, and should
therefore be avoided.

## When to use `Optional`

Optionals are great for fluent code blocks, but they should not be used as

- instance members
- method arguments
- (public) method return types

Using them in those situations is error-prone and can be detrimental for compiler optimization. Also, passing
an `Optional` to a method to perform conditional analysis inside the method is counter-productive and slightly slower
than null-checks.
