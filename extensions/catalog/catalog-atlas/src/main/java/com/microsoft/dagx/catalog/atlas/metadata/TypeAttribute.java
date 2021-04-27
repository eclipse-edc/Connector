package com.microsoft.dagx.catalog.atlas.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TypeAttribute {
    @JsonProperty
    private String name;
    @JsonProperty
    private String type;
    @JsonProperty
    private boolean required;

    public TypeAttribute(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public TypeAttribute(){}

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
        return "AttributeDefDto{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", required=" + required +
                '}';
    }
}
