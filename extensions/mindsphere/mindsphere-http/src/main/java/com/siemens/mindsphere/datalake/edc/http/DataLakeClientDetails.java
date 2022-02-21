package com.siemens.mindsphere.datalake.edc.http;

public class DataLakeClientDetails {
    public DataLakeClientDetails(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private String clientId;

    public String getClientId() {
        return clientId;
    }

    private String clientSecret;

    public String getClientSecret() {
        return clientSecret;
    }
}
