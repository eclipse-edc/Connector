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

    private URL url;

    public StubDataLakeClient(URL url) {
        this.url = url;
    }

    @Override
    public URL getPresignedUploadUrl(String path) {
        return url;
    }

    @Override
    public URL getPresignedDownloadUrl(String datalakePath) throws DataLakeException {
        return url;
    }

    @Override
    public boolean isPresent(String path) {
        return true;
    }
}
