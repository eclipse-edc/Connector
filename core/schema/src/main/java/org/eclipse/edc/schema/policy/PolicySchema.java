package org.eclipse.edc.schema.policy;

import org.eclipse.edc.schema.Schema;
import org.eclipse.edc.schema.SchemaAttribute;

public class PolicySchema extends Schema {
    public static String TYPE = "edc:policy";

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
