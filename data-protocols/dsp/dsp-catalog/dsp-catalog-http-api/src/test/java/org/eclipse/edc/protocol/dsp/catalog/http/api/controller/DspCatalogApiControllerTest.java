/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.api.controller;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.ResponseDecorator;
import org.eclipse.edc.protocol.dsp.spi.type.DspNamespace;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.DATASET_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DspCatalogApiControllerTest {

    abstract static class Tests extends RestControllerTestBase {

        protected final TypeTransformerRegistry transformerRegistry = mock();
        protected final CatalogProtocolService service = mock();
        protected final DspRequestHandler dspRequestHandler = mock();
        protected final ContinuationTokenManager continuationTokenManager = mock();

        @Test
        void getDataset_shouldGetResource() {
            when(dspRequestHandler.getResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON).build());

            baseRequest()
                    .get(DATASET_REQUEST + "/datasetId")
                    .then()
                    .statusCode(200)
                    .contentType(JSON);

            var captor = ArgumentCaptor.forClass(GetDspRequest.class);
            verify(dspRequestHandler).getResource(captor.capture());
            var request = captor.getValue();
            assertThat(request.getToken()).isEqualTo("auth");
            assertThat(request.getResultClass()).isEqualTo(Dataset.class);
            assertThat(request.getId()).isEqualTo("datasetId");
        }


        protected abstract String basePath();

        protected abstract DspNamespace namespace();

        private RequestSpecification baseRequest() {
            return given()
                    .baseUri("http://localhost:" + port)
                    .basePath(basePath())
                    .header(HttpHeaders.AUTHORIZATION, "auth")
                    .when();
        }

        @Nested
        class RequestCatalog {

            @Test
            void shouldCreateResource() {
                var requestBody = createObjectBuilder().add(TYPE, namespace().toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM)).build();
                var catalog = createObjectBuilder().add(JsonLdKeywords.TYPE, "catalog").build();
                when(transformerRegistry.transform(any(Catalog.class), eq(JsonObject.class))).thenReturn(Result.success(catalog));
                when(dspRequestHandler.createResource(any(), any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
                when(continuationTokenManager.createResponseDecorator(any())).thenReturn(mock());

                baseRequest()
                        .contentType(JSON)
                        .body(requestBody)
                        .post(CATALOG_REQUEST)
                        .then()
                        .statusCode(200)
                        .contentType(JSON);

                var captor = ArgumentCaptor.forClass(PostDspRequest.class);
                verify(dspRequestHandler).createResource(captor.capture(), isA(ResponseDecorator.class));
                var request = captor.getValue();
                assertThat(request.getInputClass()).isEqualTo(CatalogRequestMessage.class);
                assertThat(request.getResultClass()).isEqualTo(Catalog.class);
                assertThat(request.getExpectedMessageType()).isEqualTo(namespace().toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM));
                assertThat(request.getProcessId()).isNull();
                assertThat(request.getToken()).isEqualTo("auth");
                assertThat(request.getMessage()).isEqualTo(requestBody);
                verify(continuationTokenManager).createResponseDecorator("http://localhost:%d%s".formatted(port, basePath() + CATALOG_REQUEST));
            }

            @Test
            void shouldApplyContinuationToken_whenPassed() {
                var requestBody = createObjectBuilder().add(TYPE, namespace().toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM)).build();
                var catalog = createObjectBuilder().add(JsonLdKeywords.TYPE, "catalog").build();
                when(transformerRegistry.transform(any(Catalog.class), eq(JsonObject.class))).thenReturn(Result.success(catalog));
                when(dspRequestHandler.createResource(any(), any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
                when(continuationTokenManager.createResponseDecorator(any())).thenReturn(mock());
                var enrichedRequestBody = createObjectBuilder(requestBody).add("query", Json.createObjectBuilder()).build();
                when(continuationTokenManager.applyQueryFromToken(any(), any())).thenReturn(Result.success(enrichedRequestBody));

                baseRequest()
                        .contentType(JSON)
                        .body(requestBody)
                        .post(CATALOG_REQUEST + "?continuationToken=pagination-token")
                        .then()
                        .statusCode(200)
                        .contentType(JSON);

                var captor = ArgumentCaptor.forClass(PostDspRequest.class);
                verify(dspRequestHandler).createResource(captor.capture(), isA(ResponseDecorator.class));
                var request = captor.getValue();
                assertThat(request.getMessage()).isSameAs(enrichedRequestBody);
                verify(continuationTokenManager).applyQueryFromToken(requestBody, "pagination-token");
            }

            @Test
            void shouldReturnBadRequest_whenContinuationTokenIsNotValid() {
                var requestBody = createObjectBuilder().add(TYPE, namespace().toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM)).build();
                when(continuationTokenManager.applyQueryFromToken(any(), any())).thenReturn(Result.failure("error"));

                baseRequest()
                        .contentType(JSON)
                        .body(requestBody)
                        .post(CATALOG_REQUEST + "?continuationToken=pagination-token")
                        .then()
                        .statusCode(400);

                verifyNoInteractions(dspRequestHandler, transformerRegistry);
            }
        }
    }

    @ApiTest
    @Nested
    class DspCatalogApiControllerV08Test extends Tests {

        @Override
        protected String basePath() {
            return BASE_PATH;
        }

        @Override
        protected DspNamespace namespace() {
            return DspNamespace.V_08;
        }

        @Override
        protected Object controller() {
            return new DspCatalogApiController(service, dspRequestHandler, continuationTokenManager);
        }
    }

    @ApiTest
    @Nested
    class DspCatalogApiControllerV20241Test extends Tests {

        @Override
        protected String basePath() {
            return V_2024_1_PATH + BASE_PATH;
        }

        @Override
        protected DspNamespace namespace() {
            return DspNamespace.V_2024_1;
        }

        @Override
        protected Object controller() {
            return new DspCatalogApiController20241(service, dspRequestHandler, continuationTokenManager);
        }
    }
}
