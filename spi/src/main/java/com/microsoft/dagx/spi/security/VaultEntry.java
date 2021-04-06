package com.microsoft.dagx.spi.security;

public class VaultEntry {
    private String key;
    private String value;

    public VaultEntry(){
        //only used for serialization/deserialization
    }

    public VaultEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
