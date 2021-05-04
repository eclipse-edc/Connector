package com.microsoft.dagx.schema;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SchemaRegistryImpl implements SchemaRegistry {

    private final Map<String, Schema> schemas;

    public SchemaRegistryImpl() {
        schemas = new HashMap<>();
    }

    @Override
    public void register(Schema schema){
        schemas.put(schema.getName(), schema);
    }

    @Override
    public Schema getSchema(String identifier){
        return schemas.get(identifier);
    }

    @Override
    public boolean hasSchema(String identifier){
        return schemas.containsKey(identifier);
    }

    @Override
    public Collection<Schema> getSchemas() {
        return schemas.values();
    }
}
