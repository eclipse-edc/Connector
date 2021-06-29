/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataCatalogEntry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

import static com.microsoft.dagx.spi.transfer.response.ResponseStatus.ERROR_RETRY;
import static com.microsoft.dagx.spi.transfer.response.ResponseStatus.FATAL_ERROR;
import static java.lang.String.format;

public class NifiDataFlowController implements DataFlowController {
    public static final String NIFI_CREDENTIALS = "nifi.credentials";
    private static final String CONTENTLISTENER = "/contentListener";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final String SOURCE_FILE_ACCESS_KEY_NAME = "keyName";

    private final String baseUrl;
    private final TypeManager typeManager;
    private final Monitor monitor;
    private final Vault vault;
    private final OkHttpClient httpClient;
    private final NifiTransferEndpointConverter converter;

    public NifiDataFlowController(NifiTransferManagerConfiguration configuration, TypeManager typeManager, Monitor monitor, Vault vault, OkHttpClient httpClient, NifiTransferEndpointConverter converter) {
        baseUrl = configuration.getFlowUrl();
        this.typeManager = typeManager;
        this.monitor = monitor;
        this.vault = vault;
        this.httpClient = createUnsecureClient(httpClient);
        this.converter = converter;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        // handle everything for now
        return true;
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {

        DataAddress destinationAddress = dataRequest.getDataDestination();
        if (destinationAddress == null) {
            return new DataFlowInitiateResponse(FATAL_ERROR, "Data target is null");
        }

        String basicAuthCreds = vault.resolveSecret(NIFI_CREDENTIALS);
        if (basicAuthCreds == null) {
            return new DataFlowInitiateResponse(FATAL_ERROR, "NiFi vault credentials were not found");
        }

        DataEntry dataEntry = dataRequest.getDataEntry();
        DataCatalogEntry catalog = dataEntry.getCatalogEntry();
        var sourceAddress = catalog.getAddress();
        // the "keyName" entry should always be there, regardless of the source storage system
        var sourceKeyName = sourceAddress.getKeyName();

        if (sourceKeyName == null) {
            return new DataFlowInitiateResponse(FATAL_ERROR, "No 'keyName' property was found for the source file (ID=" + dataEntry.getId() + ")!");
        }

        if (destinationAddress.getKeyName() == null) {
            return new DataFlowInitiateResponse(FATAL_ERROR, "No 'keyName' property was found for the destination file (ID=" + dataEntry.getId() + ")!");
        }

        var source = converter.convert(sourceAddress);
        var dest = converter.convert(destinationAddress);


        Request request = createTransferRequest(dataRequest.getId(), source, dest, basicAuthCreds);

        try (Response response = httpClient.newCall(request).execute()) {
            int code = response.code();
            if (code != 200) {
                monitor.severe(format("Error initiating transfer request with Nifi. Code was: %d. Request id was: %s", code, dataRequest.getId()));
                return new DataFlowInitiateResponse(FATAL_ERROR, "Error initiating NiFi transfer");
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return emptyBodyError(dataRequest);
            }
            String message = responseBody.string();
            if (message.length() == 0) {
                return emptyBodyError(dataRequest);
            }

            @SuppressWarnings("unchecked") Map<String, Object> values = typeManager.readValue(message, Map.class);

            return DataFlowInitiateResponse.OK;
        } catch (IOException e) {
            monitor.severe("Error initiating data transfer request: " + dataRequest.getId(), e);
            return new DataFlowInitiateResponse(ERROR_RETRY, "Error initiating transfer");
        }
    }

    @NotNull
    private Request createTransferRequest(String requestId, NifiTransferEndpoint source, NifiTransferEndpoint destination, String basicAuthCredentials) {


        String url = baseUrl + CONTENTLISTENER;
        var nifiPayload = new NifiPayload(requestId, source, destination);
        String requestBody = typeManager.writeValueAsString(nifiPayload);
        return new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON))
                .addHeader("Authorization", basicAuthCredentials)
                .build();
    }

    @NotNull
    private DataFlowInitiateResponse emptyBodyError(DataRequest dataRequest) {
        monitor.severe(format("Error initiating transfer request with Nifi. Empty message body returned. Request id was: %s", dataRequest.getId()));
        return new DataFlowInitiateResponse(FATAL_ERROR, "Error initiating transfer");
    }

    /**
     * Creates an HTTP client that foregoes all SSL certificate validation. The original client is NOT modified, rather, a
     * new OkHttpClient is used.
     * <p>
     * ### THIS IS INSECURE!! DO NOT USE IN PRODUCTION CODE!!!! ###
     */
    @Deprecated
    private OkHttpClient createUnsecureClient(OkHttpClient httpClient) {
        try {
            // Create a trust manager that does not validate certificate chains
            X509TrustManager x509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            };
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    x509TrustManager
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();


            return httpClient.newBuilder()
                    .sslSocketFactory(sslSocketFactory, x509TrustManager)
                    .hostnameVerifier((hostname, session) -> true)
                    .build();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new DagxException("Error making the http client unsecure!", e);
        }

    }
}
