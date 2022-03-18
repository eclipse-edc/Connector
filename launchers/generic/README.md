# Generic Connector Launcher

This launcher contains a Dockerfile that can be used to run any packaged connector JAR.

For build, a mandatory build argument named `JAR` must be set, with the path to the connector runtime JAR file.

Configuration can be provided at runtime through environment variables.  The special environment variable `JVM_ARGS` may be used to set execution JVM arguments.