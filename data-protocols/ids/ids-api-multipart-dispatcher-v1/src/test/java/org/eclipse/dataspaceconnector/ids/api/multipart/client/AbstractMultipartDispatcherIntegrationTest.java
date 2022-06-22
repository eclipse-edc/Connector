/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartController;
import org.eclipse.dataspaceconnector.ids.core.serialization.ObjectMapperFactory;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
abstract class AbstractMultipartDispatcherIntegrationTest {
    private static final AtomicReference<Integer> PORT = new AtomicReference<>();
    private static final AtomicReference<Integer> IDS_PORT = new AtomicReference<>();
    private static final List<Asset> ASSETS = new LinkedList<>();
    // TODO needs to be replaced by an objectmapper capable to understand IDS JSON-LD
    //      once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
    protected static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapperFactory().getObjectMapper();
    }

    protected IdentityService identityService;

    @AfterEach
    void after() {
        ASSETS.clear();

        for (String key : getSystemProperties().keySet()) {
            System.clearProperty(key);
        }

        PORT.set(null);
        IDS_PORT.set(null);
    }

    @BeforeEach
    protected void before(EdcExtension extension) {
        PORT.set(getFreePort());
        IDS_PORT.set(getFreePort());

        for (Map.Entry<String, String> entry : getSystemProperties().entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        var tokenResult = TokenRepresentation.Builder.newInstance().token("token").build();
        var claimToken = ClaimToken.Builder.newInstance().claim("key", "value").build();
        identityService = mock(IdentityService.class);
        when(identityService.obtainClientCredentials(any(), any())).thenReturn(Result.success(tokenResult));
        when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));

        extension.registerSystemExtension(ServiceExtension.class,
                new IdsApiMultipartDispatcherV1IntegrationTestServiceExtension(ASSETS, identityService));
    }

    protected void addAsset(Asset asset) {
        ASSETS.add(asset);
    }

    protected int getPort() {
        return PORT.get();
    }

    protected int getIdsPort() {
        return IDS_PORT.get();
    }

    protected String getUrl() {
        return String.format("http://localhost:%s/api/v1/ids%s", getIdsPort(), MultipartController.PATH);
    }

    protected abstract Map<String, String> getSystemProperties();
}
