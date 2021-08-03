package org.eclipse.dataspaceconnector.schema.policy;

import org.eclipse.dataspaceconnector.schema.Schema;
import org.eclipse.dataspaceconnector.schema.SchemaAttribute;

public class PolicySchema extends Schema {
    public static String TYPE = "dataspaceconnector:policy";

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
