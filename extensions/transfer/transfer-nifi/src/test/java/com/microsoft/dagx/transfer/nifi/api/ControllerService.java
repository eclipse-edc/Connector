package com.microsoft.dagx.transfer.nifi.api;

public class ControllerService {
    public String id;
    public String uri;
    public Component component;
    public Revision revision;

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public Component getComponent() {
        return component;
    }
}
