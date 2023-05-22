# Java 17 baseline

## Decision

We will use Java 17 as baseline version.

## Rationale

Java 11 active support [will end in September 2023](https://endoflife.date/java), and, following Java "new" release cycle, we should update the baseline
version to the current LTS from time to time.
Other OSS frameworks as [Spring already did that](https://spring.io/blog/2021/09/02/a-java-17-and-jakarta-ee-9-baseline-for-spring-framework-6)
already did that.

## Approach

Just update the `javaLanguageVersion` property of the gradle `BuildExtension`.
