/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.spi.issuerservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * JUnit extension that sets up a mock Issuer Service.
 * Provides a IssuerService instance for use in tests.
 */
public class IssuerServiceEndToEndExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private final WireMockServer wireMockServer;
    private final LazySupplier<IssuerService> issuerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IssuerServiceEndToEndExtension() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        issuerService = new LazySupplier<>(() -> new IssuerService(wireMockServer.port()));
    }


    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        wireMockServer.stop();
    }

    public IssuerService getIssuerService() {
        return issuerService.get();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        wireMockServer.start();
        var didDocumentJson = objectMapper.writeValueAsString(issuerService.get().getDidDocument());
        wireMockServer.stubFor(get("/issuer/did.json")
                .willReturn(okJson(didDocumentJson)));

    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(IssuerService.class);
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(IssuerService.class)) {
            return getIssuerService();
        }
        return null;
    }

}
