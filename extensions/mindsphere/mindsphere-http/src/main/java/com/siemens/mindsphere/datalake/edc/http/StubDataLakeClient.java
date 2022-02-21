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
