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

import java.net.URL;

public class StubDataLakeClient implements DataLakeClient {
    public StubDataLakeClient(URL downloadUrl, URL uploadUrl) {
        this.downloadUrl = downloadUrl;
        this.uploadUrl = uploadUrl;
    }

    private URL downloadUrl;

    private URL uploadUrl;

    @Override
    public URL getDownloadUrl(String path) {
        return downloadUrl;
    }

    @Override
    public URL getUploadUrl(String path) {
        return uploadUrl;
    }

    @Override
    public boolean isPresent(String path) {
        return true;
    }
}
