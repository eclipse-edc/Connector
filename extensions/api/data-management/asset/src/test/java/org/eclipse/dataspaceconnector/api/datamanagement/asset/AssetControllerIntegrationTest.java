/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.extension.jersey.CorsFilterConfiguration;
import org.eclipse.dataspaceconnector.extension.jersey.JerseyRestService;
import org.eclipse.dataspaceconnector.extension.jetty.JettyConfiguration;
import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.extension.jetty.PortMapping;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AssetControllerIntegrationTest {

    private static int port;
    ObjectMapper objectMapper;
    private OkHttpClient client;

    @BeforeAll
    static void prepareWebserver() {
        port = TestUtils.getFreePort();
        var monitor = mock(Monitor.class);
        var config = new JettyConfiguration(null, null);
        config.portMapping(new PortMapping("data", port, "/api/v1/data"));
        var jetty = new JettyService(config, monitor);

        var ctrl = new AssetController(monitor);
        var jerseyService = new JerseyRestService(jetty, new TypeManager(), mock(CorsFilterConfiguration.class), monitor);
        jetty.start();
        jerseyService.registerResource("data", ctrl);
        jerseyService.start();
    }

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        client = new OkHttpClient();
    }

    @Test
    void getAllAssets() throws IOException {
        var response = get(basePath());
        assertThat(response.code()).isEqualTo(200);
    }

    @Test
    void getSingleAsset() throws IOException {
        var id = "test-id";
        try (var response = get(basePath() + "/" + id)) {
            //assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    void getSingleAsset_notFound() throws IOException {
        var id = "test-id";
        try (var response = get(basePath() + "/" + id)) {
            //assertThat(response.code()).isEqualTo(400);
        }
    }

    @Test
    void postAsset() throws IOException {
        var assetDto = AssetDto.Builder.newInstance().properties(Collections.singletonMap("Asset-1", "An Asset")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("asset-1", "/localhost")).build();
        var assetEntryDto = AssetEntryDto.Builder.newInstance().assetDto(assetDto).dataAddress(dataAddress).build();
        var str = objectMapper.writeValueAsString(assetEntryDto);

        RequestBody requestBody = RequestBody.create(str, MediaType.parse("application/json"));

        try (var response = post(basePath(), requestBody)) {
            //assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    void postAssetId_alreadyExists() throws IOException {
        var assetDto = AssetDto.Builder.newInstance().properties(Collections.singletonMap("Asset-Existent", "An Asset")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("asset-1", "/localhost")).build();
        var assetEntryDto = AssetEntryDto.Builder.newInstance().assetDto(assetDto).dataAddress(dataAddress).build();
        var str = objectMapper.writeValueAsString(assetEntryDto);

        RequestBody requestBody = RequestBody.create(str, MediaType.parse("application/json"));

        try (var response = post(basePath(), requestBody)) {
            //assertThat(response.code()).isEqualTo(400);
        }
    }

    @Test
    void postAssetIsEmpty() throws IOException {
        var assetDto = AssetDto.Builder.newInstance().properties(Collections.singletonMap("Asset-empty", "")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("asset-1", "/localhost")).build();
        var assetEntryDto = AssetEntryDto.Builder.newInstance().assetDto(assetDto).dataAddress(dataAddress).build();
        var str = objectMapper.writeValueAsString(assetEntryDto);

        RequestBody requestBody = RequestBody.create(str, MediaType.parse("application/json"));

        try (var response = post(basePath(), requestBody)) {
            //assertThat(response.code()).isEqualTo(400);
        }
    }

    @Test
    void deleteAsset() throws IOException {
        var id = "test-id";
        try (var response = delete(basePath() + "/" + id)) {
            //assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    void deleteAssetId_notExists() throws IOException {
        var id = "test-id";
        try (var response = delete(basePath() + "/" + id)) {
            //assertThat(response.code()).isEqualTo(400);
        }
    }

    @NotNull
    private String basePath() {
        return "http://localhost:" + port + "/api/v1/data/assets";
    }

    @NotNull
    private Response get(String url) throws IOException {
        return client.newCall(new Request.Builder().get().url(url).build()).execute();
    }

    @NotNull
    private Response post(String url, RequestBody requestBody) throws IOException {
        return client.newCall(new Request.Builder().post(requestBody).url(url).build()).execute();
    }

    @NotNull
    private Response delete(String url) throws IOException {
        return client.newCall(new Request.Builder().delete().url(url).build()).execute();
    }
}
