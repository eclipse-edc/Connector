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

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.model.CatalogRequestDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.datamanagement.catalog.TestFunctions.createCriterionDto;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class CatalogApiControllerIntegrationTest {

    private final int port = getFreePort();
    private final String authKey = "123456";
    private final RemoteMessageDispatcher dispatcher = mock(RemoteMessageDispatcher.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        when(dispatcher.protocol()).thenReturn("ids-multipart");

        extension.registerSystemExtension(ServiceExtension.class, new TestExtension());
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
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
                .asset(Asset.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .build();
        var catalog = Catalog.Builder.newInstance().id("id").contractOffers(List.of(contractOffer)).build();
        var emptyCatalog = Catalog.Builder.newInstance().id("id2").contractOffers(List.of()).build();
        when(dispatcher.send(any(), any(), any())).thenReturn(completedFuture(catalog))
                .thenReturn(completedFuture(emptyCatalog));

        baseRequest()
                .queryParam("providerUrl", "some.provider.url")
                .get("/catalog")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", notNullValue())
                .body("contractOffers.size()", is(1));
    }

    @Test
    void getProviderCatalog_shouldFailWithoutProviderUrl() {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .build();
        var catalog = Catalog.Builder.newInstance().id("id").contractOffers(List.of(contractOffer)).build();
        var emptyCatalog = Catalog.Builder.newInstance().id("id2").contractOffers(List.of()).build();
        when(dispatcher.send(any(), any(), any())).thenReturn(completedFuture(catalog))
                .thenReturn(completedFuture(emptyCatalog));

        baseRequest()
                .get("/catalog")
                .then()
                .statusCode(400)
                .body("message[0]", is("providerUrl must not be null"));
    }

    @Test
    void postCatalogRequest() {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .build();
        var catalog = Catalog.Builder.newInstance().id("id").contractOffers(List.of(contractOffer)).build();
        var emptyCatalog = Catalog.Builder.newInstance().id("id2").contractOffers(List.of()).build();
        when(dispatcher.send(any(), any(), any()))
                .thenReturn(completedFuture(catalog))
                .thenReturn(completedFuture(emptyCatalog));

        var requestDto = CatalogRequestDto.Builder.newInstance()
                .querySpec(QuerySpecDto.Builder.newInstance()
                        .limit(29)
                        .offset(13)
                        .filterExpression(List.of(createCriterionDto("fooProp", "", "bar"), createCriterionDto("bazProp", "in", List.of("blip", "blup", "blop"))))
                        .sortField("someField")
                        .sortOrder(SortOrder.DESC).build())
                .providerUrl("some.provider.url")

                .build();

        baseRequest()
                .contentType(JSON)
                .body(requestDto)
                .post("/catalog/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", notNullValue())
                .body("contractOffers.size()", is(1));
        var requestCaptor = ArgumentCaptor.forClass(CatalogRequest.class);
        verify(dispatcher).send(eq(Catalog.class), requestCaptor.capture(), any(MessageContext.class));
        var rq = requestCaptor.getValue().getQuerySpec();

        var query = requestDto.getQuerySpec();
        assertThat(rq.getOffset()).isEqualTo(query.getOffset());
        assertThat(rq.getLimit()).isEqualTo(query.getLimit());
        assertThat(rq.getSortField()).isEqualTo(query.getSortField());
        assertThat(rq.getSortOrder()).isEqualTo(query.getSortOrder());
        // technically we're comparing a Criterion and a CriterionDto, but they have the same fields, so recursive comp. works
        assertThat(rq.getFilterExpression()).usingRecursiveFieldByFieldElementComparator().isEqualTo(query.getFilterExpression());
    }

    @Test
    void postCatalogRequest_whenOnlyProviderUrl_shouldUseDefault() {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .build();
        var catalog = Catalog.Builder.newInstance().id("id").contractOffers(List.of(contractOffer)).build();
        var emptyCatalog = Catalog.Builder.newInstance().id("id2").contractOffers(List.of()).build();
        when(dispatcher.send(any(), any(), any()))
                .thenReturn(completedFuture(catalog))
                .thenReturn(completedFuture(emptyCatalog));

        var requestDto = CatalogRequestDto.Builder.newInstance()
                .providerUrl("some.provider.url")
                .build();


        baseRequest()
                .body(requestDto)
                .contentType("application/json")
                .post("/catalog/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", notNullValue())
                .body("contractOffers.size()", is(1));

        var requestCaptor = ArgumentCaptor.forClass(CatalogRequest.class);
        verify(dispatcher).send(eq(Catalog.class), requestCaptor.capture(), any(MessageContext.class));
        var rq = requestCaptor.getValue().getQuerySpec();
        assertThat(rq.getOffset()).isEqualTo(0);
        assertThat(rq.getLimit()).isEqualTo(50);
        assertThat(rq.getSortField()).isNull();
        assertThat(rq.getSortOrder()).isEqualTo(SortOrder.ASC);
        assertThat(rq.getFilterExpression()).isNotNull().isEmpty();
    }

    @Test
    void postCatalogRequest_whenNoBody_shouldReturn400() {
        baseRequest()
                .contentType("application/json")
                .post("/catalog/request")
                .then()
                .statusCode(400);
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
