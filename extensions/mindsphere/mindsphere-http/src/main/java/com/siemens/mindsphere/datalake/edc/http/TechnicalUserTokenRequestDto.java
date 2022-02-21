package com.siemens.mindsphere.datalake.edc.http;

public class TechnicalUserTokenRequestDto {
    public TechnicalUserTokenRequestDto(String hostTenant, String userTenant, String appName, String appVersion) {
        this.hostTenant = hostTenant;
        this.userTenant = userTenant;
        this.appName = appName;
        this.appVersion = appVersion;
        this.grant_type = "client_credentials";
    }

    private String grant_type;
    private String appName;
    private String appVersion;
    private String hostTenant;
    private String userTenant;

    public String getGrant_type() {
        return grant_type;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getHostTenant() {
        return hostTenant;
    }

    public String getUserTenant() {
        return userTenant;
    }

    @Override
    public String toString() {
        return "TechnicalUserTokenRequestDto{" +
                "grant_type='" + grant_type + '\'' +
                ", appName='" + appName + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", hostTenant='" + hostTenant + '\'' +
                ", userTenant='" + userTenant + '\'' +
                '}';
    }
}
