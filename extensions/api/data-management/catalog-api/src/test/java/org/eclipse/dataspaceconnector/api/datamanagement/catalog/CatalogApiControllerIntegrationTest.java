/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.catalog;

import com.github.javafaker.Faker;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class CatalogApiControllerIntegrationTest {

    private static final Faker FAKER = Faker.instance();
    private final int port = getFreePort();
    private final String authKey = "123456";
    private final RemoteMessageDispatcher dispatcher = mock(RemoteMessageDispatcher.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        when(dispatcher.protocol()).thenReturn("ids-multipart");

        extension.registerSystemExtension(ServiceExtension.class, new TestExtension());
        extension.setConfiguration(Map.of(
                "web.http.data.port", String.valueOf(port),
                "web.http.data.path", "/api/v1/data",
                "edc.api.auth.key", authKey
        ));
    }

    @Test
    void getProviderCatalog() {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .assetId(UUID.randomUUID().toString())
                .build();
        var catalog = Catalog.Builder.newInstance().id("id").contractOffers(List.of(contractOffer)).build();
        when(dispatcher.send(any(), any(), any())).thenReturn(completedFuture(catalog));

        baseRequest()
                .queryParam("providerUrl", FAKER.internet().url())
                .get("/catalog")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", notNullValue())
                .body("contractOffers.size()", is(1));
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/data")
                .header("x-api-key", authKey)
                .when();
    }

    @Provides(ContractOfferService.class)
    private class TestExtension implements ServiceExtension {

        @Inject
        RemoteMessageDispatcherRegistry registry;

        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(ContractOfferService.class, mock(ContractOfferService.class));

            registry.register(dispatcher);
        }
    }
}
