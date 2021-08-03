package org.eclipse.edc.schema;

public abstract class DataSchema extends Schema {
    //marker class to allow filtering etc.

    public DataSchema() {
        super();
        attributes.add(new SchemaAttribute("keyName", true));
        attributes.add(new SchemaAttribute("policyId", false));
    }
}
