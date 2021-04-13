package com.microsoft.dagx.transfer.nifi.api;

public class Component {
    public String id;
    public String name;
    public String type;
    public String state;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }
}
