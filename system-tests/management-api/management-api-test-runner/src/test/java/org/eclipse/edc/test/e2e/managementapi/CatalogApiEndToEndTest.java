/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERATOR;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_QUERY_SPEC_TYPE;
import static org.hamcrest.Matchers.is;

//@EndToEndTest
public class CatalogApiEndToEndTest extends BaseManagementApiEndToEndTest {

    // requests the catalog to itself, to save another connector.
    private final String providerUrl = "http://localhost:" + PROTOCOL_PORT + "/protocol";

    @Test
    void shouldReturnCatalog_withoutQuerySpec() {
        var requestBody = Json.createObjectBuilder()
                .add(TYPE, EDC_CATALOG_REQUEST_TYPE)
                .add(EDC_CATALOG_REQUEST_PROVIDER_URL, providerUrl)
                .add(EDC_CATALOG_REQUEST_PROTOCOL, "dataspace-protocol-http")
                .build();

        given()
                .port(PORT)
                .contentType(JSON)
                .body(requestBody)
                .basePath("/management/v2/catalog")
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(TYPE, is("dcat:Catalog"));
    }

    @Test
    void shouldReturnCatalog_withQuerySpec() {
        var criteria = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add(TYPE, CRITERION_TYPE)
                        .add(CRITERION_OPERAND_LEFT, "key")
                        .add(CRITERION_OPERATOR, "=")
                        .add(CRITERION_OPERAND_RIGHT, "value")
                        .build()
                )
                .build();
        var querySpec = Json.createObjectBuilder()
                .add(TYPE, EDC_QUERY_SPEC_TYPE)
                .add(EDC_QUERY_SPEC_FILTER_EXPRESSION, criteria)
                .build();
        var requestBody = Json.createObjectBuilder()
                .add(TYPE, EDC_CATALOG_REQUEST_TYPE)
                .add(EDC_CATALOG_REQUEST_PROVIDER_URL, providerUrl)
                .add(EDC_CATALOG_REQUEST_PROTOCOL, "dataspace-protocol-http")
                .add(EDC_CATALOG_REQUEST_QUERY_SPEC, querySpec)
                .build();

        given()
                .port(PORT)
                .contentType(JSON)
                .body(requestBody)
                .basePath("/management/v2/catalog")
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(TYPE, is("dcat:Catalog"));
    }
}
