package org.eclipse.edc.transfer.demo.protocols.http;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transfer.demo.protocols.stream.StreamSession;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.net.URL;

/**
 * Publishes to an HTTP endpoint.
 */
public class HttpStreamSession implements StreamSession {
    private URL endpointURL;
    private String destinationToken;
    private OkHttpClient httpClient;

    public HttpStreamSession(URL endpointURL, String destinationToken, OkHttpClient httpClient) {
        this.endpointURL = endpointURL;
        this.destinationToken = destinationToken;
        this.httpClient = httpClient;
    }

    @Override
    public void publish(byte[] data) {
        try {
            var body = RequestBody.create(data, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(endpointURL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Authorization", destinationToken)
                    .post(body)
                    .build();

            try (var response = httpClient.newCall(request).execute()) {
                if (response.code() != 200) {
                    throw new EdcException("Invalid response received from destination: " + response.code());
                }
            }

        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
