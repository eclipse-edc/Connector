package com.siemens.mindsphere.datalake.edc.http;

import java.io.IOException;

public class DataLakeException extends IOException {
    public DataLakeException(String message) {
        super(message);
    }

    public DataLakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
