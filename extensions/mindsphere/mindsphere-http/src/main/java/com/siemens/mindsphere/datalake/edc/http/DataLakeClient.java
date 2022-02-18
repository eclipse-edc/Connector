package com.siemens.mindsphere.datalake.edc.http;

import java.net.URL;

public interface DataLakeClient {
    URL getDownloadUrl(String path);
    URL getUploadUrl(String path);
    boolean isPresent(String path);
}
