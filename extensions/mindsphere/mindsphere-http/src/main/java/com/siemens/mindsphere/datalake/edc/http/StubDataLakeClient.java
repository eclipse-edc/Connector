package com.siemens.mindsphere.datalake.edc.http;

import java.net.URL;

public class StubDataLakeClient implements DataLakeClient {

    private URL url;

    public StubDataLakeClient(URL url) {
        this.url = url;
    }

    @Override
    public URL getUrl(String path) {
        return url;
    }

    @Override
    public boolean isPresent(String path) {
        return true;
    }
}
