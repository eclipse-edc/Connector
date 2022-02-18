package com.siemens.mindsphere.datalake.edc.http.provision;

public class DestinationUrlProvisionResponse {
    private final String path;
    private final String url;

    public DestinationUrlProvisionResponse(String path, String url) {
        this.path = path;
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }
}
