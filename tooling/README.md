# Automatic documentation generator

The module processor uses the annotation processor APIs to hook into the Java Compiler API
in order to generate documentation material out of annotated java code.

## Module overview

1. `module-domain`: contains all domain objects, that represent technical concepts such as modules, services and
   configuration settings. It is mainly used by the processor to build an in-memory model of the project structure.
2. `module-processor`: contains the actual annotation processor and resolver classes that resolve annotation values
   during compilation.
3. `module-processor-extension-test`: test project that verifies the correct function of the extension and
   service introspection.
4. `module-processor-spi-test`: test project that verifies the correct function of the module introspection.