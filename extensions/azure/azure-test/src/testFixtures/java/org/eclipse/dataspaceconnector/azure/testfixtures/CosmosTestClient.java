/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.testfixtures;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

public interface CosmosTestClient {

    static CosmosClient createClient() {
        var cosmosKey = propOrEnv("COSMOS_KEY", null);
        if (!StringUtils.isNullOrBlank(cosmosKey)) {
            String endpoint = propOrEnv("COSMOS_URL", "https://edcintdev-cosmosdb.documents.azure.com:443/");
            return azureClient(cosmosKey, endpoint);
        } else {
            return localClient();
        }
    }

    private static CosmosClient azureClient(String cosmosKey, String cosmosUrl) {
        return new CosmosClientBuilder()
                .key(cosmosKey)
                .preferredRegions(List.of("westeurope"))
                .endpoint(cosmosUrl)
                .buildClient();
    }

    private static CosmosClient localClient() {
        try {
            var endpoint = "https://127.0.0.1:8081";

            trustCertificateFrom(endpoint);

            // Emulator key is a fixed value: https://github.com/Azure/azure-cosmos-db-emulator-docker
            String masterKey = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";

            return new CosmosClientBuilder()
                    .key(masterKey)
                    .endpoint(endpoint)
                    .buildClient();

        } catch (Exception e) {
            throw new EdcException("Error in creating cosmos local client.", e);
        }
    }

    private static void trustCertificateFrom(String endpoint) {
        var client = trustStorePopulatingHttpClient();
        var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body().close();
        } catch (Exception e) {
            throw new EdcException(format("Error getting certificate. Url: %s", endpoint), e);
        }
    }

    private static HttpClient trustStorePopulatingHttpClient() {
        var trustStorePopulator = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        setUpTrustStoreWithCertificates(certs);
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustStorePopulator, new SecureRandom());
            return HttpClient.newBuilder().sslContext(sslContext).build();
        } catch (Exception e) {
            throw new EdcException("Error initializing ssl context", e);
        }
    }

    private static void setUpTrustStoreWithCertificates(X509Certificate[] certs) {
        var trustStorePath =
                System.getProperty("javax.net.ssl.trustStore",
                        System.getProperty("java.home") + separator + "lib" + separator + "security" + separator + "cacerts");

        try (var stream = new FileInputStream(trustStorePath)) {
            var trustStorePassword = propOrEnv("javax.net.ssl.trustStorePassword", "changeit").toCharArray();

            var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(stream, trustStorePassword);

            var certCounter = 0;
            for (var cert : certs) {
                trustStore.setCertificateEntry("cosmosdb" + certCounter++, cert);
            }

            var newTrustStorePath = Files.createTempFile(null, null);
            try (var output = new FileOutputStream(newTrustStorePath.toFile())) {
                trustStore.store(output, trustStorePassword);
            }
            System.setProperty("javax.net.ssl.trustStore", newTrustStorePath.toString());
        } catch (Exception e) {
            throw new EdcException("Error initializing certificate store", e);
        }
    }

}