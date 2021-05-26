package com.microsoft.dagx.transfer.demo.protocols.http;

import com.microsoft.dagx.transfer.demo.protocols.stream.StreamSession;
import okhttp3.OkHttpClient;

import java.net.URL;

/**
 * Publishes to an HTTP endpoint.
 */
public class HttpStreamSession implements StreamSession {
    private URL endpointURL;
    private OkHttpClient httpClient;

    public HttpStreamSession(URL endpointURL, OkHttpClient httpClient) {
        this.endpointURL = endpointURL;
        this.httpClient = httpClient;
    }

    @Override
    public void publish(byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        // no-op
    }
}
