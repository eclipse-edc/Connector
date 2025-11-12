# JDK Logger Monitor

> This extension has been deprecated and it could be removed after 0.17.0 release

This extension provides a `Logger` which is an implementation of edc `Monitor` interface. It is based on `java.util.logging` package so it doesn't introduce any additional dependencies. As this extension is based on `MonitorExtension` which means this logger extension will load during the runtime initialization and all monitor logs (including edc core framework) will be forwarded to this logger.

## Usages

To use this logger extension just add this monitor-jdk-logger extension package in your project dependency.

## Logging Configuration

As this extension usages `java.util.logging` so to specify logger handler, format, level etc. provide a java properties file e.g. logging.properties

```text
handlers = java.util.logging.ConsoleHandler
.level = INFO
java.util.logging.ConsoleHandler.level = ALL
java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
java.util.logging.SimpleFormatter.format = %1$tF %1$tT %4$s : %5$s %n
```

To use this file pass it as jvm arguments `-Djava.util.logging.config.file=logging.properties`

## Know limitation

A class level logger cannot be created as current Monitor interface methods does not have class as method arguments . So instead a global or extension level can be used.
