/*
 *  Copyright (c) 2021, 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package com.siemens.mindsphere.datalake.edc.http;

public class TechnicalUserTokenRequestDto {
    public TechnicalUserTokenRequestDto(String hostTenant, String userTenant, String appName, String appVersion) {
        this.hostTenant = hostTenant;
        this.userTenant = userTenant;
        this.appName = appName;
        this.appVersion = appVersion;
        this.grantType = "client_credentials";
    }

    private String grantType;
    private String appName;
    private String appVersion;
    private String hostTenant;
    private String userTenant;

    public String getGrantType() {
        return grantType;
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
                "grant_type='" + grantType + '\'' +
                ", appName='" + appName + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", hostTenant='" + hostTenant + '\'' +
                ", userTenant='" + userTenant + '\'' +
                '}';
    }
}
