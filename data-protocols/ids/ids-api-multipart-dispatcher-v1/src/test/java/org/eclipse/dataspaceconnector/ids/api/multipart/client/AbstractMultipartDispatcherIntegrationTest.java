/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartController;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EdcExtension.class)
abstract class AbstractMultipartDispatcherIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected IdentityService identityService;

    {
        OBJECT_MAPPER.registerModule(new JavaTimeModule()); // configure ISO 8601 time de/serialization
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // serialize dates in ISO 8601 format
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        OBJECT_MAPPER.setDateFormat(df);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        OBJECT_MAPPER.registerModule(module);
    }

    public static final String HEADER = "header";
    public static final String PAYLOAD = "payload";
    private static final AtomicReference<Integer> PORT = new AtomicReference<>();
    private static final List<Asset> ASSETS = new LinkedList<>();

    @BeforeEach
    protected void before(EdcExtension extension) {
        PORT.set(findUnallocatedServerPort());

        for (Map.Entry<String, String> entry : getSystemProperties().entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        var tokenResult = TokenResult.Builder.newInstance().token("token").build();
        var claimToken = ClaimToken.Builder.newInstance().claim("key", "value").build();
        var verificationResult = new VerificationResult(claimToken);

        identityService = EasyMock.createMock(IdentityService.class);
        EasyMock.expect(identityService.obtainClientCredentials(EasyMock.anyObject())).andReturn(tokenResult);
        EasyMock.expect(identityService.verifyJwtToken(EasyMock.anyObject(), EasyMock.anyObject())).andReturn(verificationResult);
        EasyMock.replay(identityService);

        extension.registerSystemExtension(ServiceExtension.class,
                new IdsApiMultipartDispatcherV1IntegrationTestServiceExtension(ASSETS, identityService));
    }

    @AfterEach
    void after() {
        ASSETS.clear();

        for (String key : getSystemProperties().keySet()) {
            System.clearProperty(key);
        }

        PORT.set(null);
    }

    protected void addAsset(Asset asset) {
        ASSETS.add(asset);
    }

    protected int getPort() {
        return PORT.get();
    }

    private static int findUnallocatedServerPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected String getUrl() {
        return String.format("http://localhost:%s/api%s", getPort(), MultipartController.PATH);
    }

    protected abstract Map<String, String> getSystemProperties();

    protected DynamicAttributeToken getDynamicAttributeToken() {
        return new DynamicAttributeTokenBuilder()._tokenValue_("fake").build();
    }

}
