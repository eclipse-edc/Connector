package com.microsoft.dagx.schema;

public class SchemaAttribute {
    private String name;
    private String type;
    private boolean required;
    private String serializeAs;

    public SchemaAttribute(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    protected SchemaAttribute() {

    }

    public SchemaAttribute(String name, boolean isRequired) {
        this(name, "string", isRequired);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", required=" + required +
                '}';
    }

    public String getSerializeAs() {
        return serializeAs;
    }

    public void setSerializeAs(String serializeAs) {
        this.serializeAs = serializeAs;
    }
}
