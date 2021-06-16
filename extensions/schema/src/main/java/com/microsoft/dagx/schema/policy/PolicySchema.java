package com.microsoft.dagx.schema.policy;

import com.microsoft.dagx.schema.Schema;
import com.microsoft.dagx.schema.SchemaAttribute;

public class PolicySchema extends Schema {
    public static String TYPE = "dagx:policy";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute("namespace", false));
        attributes.add(new SchemaAttribute("serialized", true));
        attributes.add(new SchemaAttribute("validity-start", false));
        attributes.add(new SchemaAttribute("validity-end", false));
        attributes.add(new SchemaAttribute("policyName", false));
    }

    @Override
    public String getName() {
        return TYPE;
    }
}
