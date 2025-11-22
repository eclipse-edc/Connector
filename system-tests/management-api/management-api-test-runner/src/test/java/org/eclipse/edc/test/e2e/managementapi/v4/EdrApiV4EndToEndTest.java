/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.test.e2e.managementapi.v4;

import jakarta.json.JsonArray;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.ManagementEndToEndTestContext;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class EdrApiV4EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {


        @BeforeEach
        void beforeEach(EndpointDataReferenceStore store) {
            var all = store.query(QuerySpec.max()).getContent();
            all.forEach(edr -> store.delete(edr.getTransferProcessId()));
        }

        @Test
        void queryEdrEntries_noQuerySpec(ManagementEndToEndTestContext context, EndpointDataReferenceStore store) {
            var id = UUID.randomUUID().toString();
            var entry = createEdrEntry(id).build();
            store.save(entry, createDataAddress());

            context.baseRequest()
                    .contentType(JSON)
                    .post("/v4beta/edrs/request")
                    .then()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("[0].'@id'", equalTo(entry.getId()))
                    .body("[0].transferProcessId", equalTo(entry.getTransferProcessId()))
                    .body("[0].agreementId", equalTo(entry.getAgreementId()))
                    .body("[0].assetId", equalTo(entry.getAssetId()))
                    .body("[0].contractNegotiationId", equalTo(entry.getContractNegotiationId()))
                    .body("[0].providerId", equalTo(entry.getProviderId()));
        }

        @Test
        void queryEdrEntries_sortByCreatedDate(ManagementEndToEndTestContext context, EndpointDataReferenceStore store) {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            var id3 = UUID.randomUUID().toString();
            var createdAtTime = new AtomicLong(1000L);
            Stream.of(id1, id2, id3).forEach(id -> store.save(createEdrEntry(id).createdAt(createdAtTime.getAndIncrement()).build(), createDataAddress()));

            var query = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "QuerySpec")
                    .add("sortField", "createdAt")
                    .add("sortOrder", "DESC")
                    .add("limit", 100)
                    .add("offset", 0)
                    .build();

            var result = context.baseRequest()
                    .contentType(JSON)
                    .body(query)
                    .post("/v4beta/edrs/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(3))
                    .extract()
                    .as(List.class);

            assertThat(result)
                    .extracting(cd -> ((LinkedHashMap<?, ?>) cd).get(ID))
                    .containsExactlyElementsOf(List.of(id3, id2, id1));
        }


        @Test
        void shouldRetrieveEdrDataAddress(ManagementEndToEndTestContext context, EndpointDataReferenceStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createEdrEntry(id).build(), createDataAddress());

            var actual = store.findById(id);

            assertThat(actual.getId()).matches(id);

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v4beta/edrs/%s/dataaddress".formatted(id))
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body(TYPE, is("DataAddress"))
                    .body("type", is("TestType"));
        }

        @Test
        void delete(ManagementEndToEndTestContext context, EndpointDataReferenceStore store) {
            var id = UUID.randomUUID().toString();
            var entity = createEdrEntry(id).build();
            store.save(entity, createDataAddress());

            context.baseRequest()
                    .delete("/v4beta/edrs/" + id)
                    .then()
                    .statusCode(204);

            var actual = store.findById(id);

            assertThat(actual).isNull();
        }


        private EndpointDataReferenceEntry.Builder createEdrEntry(String id) {
            return EndpointDataReferenceEntry.Builder.newInstance()
                    .id(id)
                    .agreementId("agreement-" + id)
                    .transferProcessId(id)
                    .assetId("asset-" + id)
                    .contractNegotiationId("negotiation-" + id)
                    .providerId("provider-" + id)
                    .participantContextId("participantContextId")
                    .createdAt(System.currentTimeMillis());
        }

        private DataAddress createDataAddress() {
            return DataAddress.Builder.newInstance().type("TestType")
                    .property("key1", "value1")
                    .property("key2", "value2")
                    .build();
        }

        private JsonArray jsonLdContext() {
            return createArrayBuilder()
                    .add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                    .build();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(postgres::config)
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();

    }

}
