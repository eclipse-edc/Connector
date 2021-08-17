package org.eclipse.dataspaceconnector.samples.identity.registrationservice.crawler;

public class DidEntry {
    private String didSuffix;
    private String timestamp;
    private String[] type;

    public String getDidSuffix() {
        return didSuffix;
    }

    public void setDidSuffix(String didSuffix) {
        this.didSuffix = didSuffix;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String[] getType() {
        return type;
    }

    public void setType(String[] type) {
        this.type = type;
    }
}
