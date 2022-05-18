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
