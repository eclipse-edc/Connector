package com.microsoft.dagx.schema;

public abstract class DataSchema extends Schema {
    //marker class to allow filtering etc.

    public DataSchema() {
        super();
        attributes.add(new SchemaAttribute("keyName", true));
    }
}
