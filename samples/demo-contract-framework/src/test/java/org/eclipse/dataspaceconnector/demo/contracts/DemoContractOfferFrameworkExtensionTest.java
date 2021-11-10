/*
 *  Copyright (c) 2021 BMW Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       BMW Group - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.contracts;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
public class DemoContractOfferFrameworkExtensionTest {

    private static final String HTTP_PORT = "9999";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerSystemExtension(ConfigurationExtension.class, testConfiguration());
    }

    @Test
    void retrieveContractOffers(OkHttpClient httpClient, TypeManager typeManager) {
        ConnectorClient client = new ConnectorClient(httpClient, typeManager);
        // for the 'id' we must actually use the constant from Asset, otherwise an exception gets raised during
        // the adding of the asset to the index
        client.indexAsset(Map.of(Asset.PROPERTY_ID, "anId", "path", "/a/valid/path"));

        List<Map<String, Object>> contractOffers = client.getAllContractOffers();

        assertThat(contractOffers).hasSize(1).element(0)
                .extracting("assets").asList().element(0)
                .extracting("properties").extracting(Asset.PROPERTY_ID)
                .isEqualTo("anId");
    }

    @NotNull
    private ConfigurationExtension testConfiguration() {
        return key -> {
            if ("web.http.port".equals(key)) {
                return HTTP_PORT;
            }
            return null;
        };
    }

    private static class ConnectorClient {
        private final OkHttpClient httpClient;
        private final TypeManager typeManager;
        private final String url = String.format("http://localhost:%s/api", HTTP_PORT);

        public ConnectorClient(OkHttpClient httpClient, TypeManager typeManager) {
            this.httpClient = httpClient;
            this.typeManager = typeManager;
        }

        public void indexAsset(Map<String, String> requestData) {
            RequestBody requestBody = RequestBody.create(typeManager.writeValueAsBytes(requestData), MediaType.get("application/json"));
            Request request = new Request.Builder().url(url + "/assets")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.code()).isEqualTo(200);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public List<Map<String, Object>> getAllContractOffers() {
            Request request = new Request.Builder().url(url + "/offers")
                    .get().build();

            try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.code()).isEqualTo(200);
                return typeManager.readValue(response.body().string(), new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
