package com.microsoft.dagx.schema;

import java.util.Collection;

public interface SchemaRegistry {
    public static final String FEATURE = "schema-registry";

    void register(Schema schema);

    Schema getSchema(String identifier);

    boolean hasSchema(String identifier);

    Collection<Schema> getSchemas();
}
