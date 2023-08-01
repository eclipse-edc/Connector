# Default Datasource configuration

## Decision

All SQL store extensions, if the specific datasource name is not configured should fall back to the default one.

## Rationale

Each SQL store implementation currently must have at least one mandatory configuration
parameter `edc.datasource.<entity>.name` and then in case additional parameters as `url`, `user`, `password` and so on.

This is powerful for fine-grained configuration and deployment. For simpler cases and scenario all the stores should
default to a common datasource name, that will be easier to configure.

## Approach

All the SQL store extensions will default to a common datasource name defined
in `DataSourceRegistry#DEFAULT_DATASOURCE` (**default**)
and then users can configure a single datasource like this:

```
edc.datasource.default.user=
edc.datasource.default.password=
edc.datasource.default.url=
...
```

