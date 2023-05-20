/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.test.system.local;

import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonObject;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_ASSET_FILE;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_ASSET_ID;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_CONNECTOR_MANAGEMENT_URL;


public class BlobTransferUtils {

    private static final String ASSETS_PATH = "/v2/assets";
    private static final String POLICIES_PATH = "/v2/policydefinitions";
    private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";

    private BlobTransferUtils() {
    }

    public static String createAsset(String accountName, String containerName) {
        Map<String, Object> dataAddressProperties = Map.of(
                "type", AzureBlobStoreSchema.TYPE,
                AzureBlobStoreSchema.ACCOUNT_NAME, accountName,
                AzureBlobStoreSchema.CONTAINER_NAME, containerName,
                AzureBlobStoreSchema.BLOB_NAME, PROVIDER_ASSET_FILE,
                "keyName", format("%s-key1", accountName)
        );

        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add("asset", createObjectBuilder()
                        .add(ID, PROVIDER_ASSET_ID)
                        .add("properties", createObjectBuilder()
                                .add("name", PROVIDER_ASSET_ID)
                                .add("contenttype", "text/plain")
                                .add("version", "1.0"))
                )
                .add("dataAddress", createObjectBuilder()
                        .add("properties", createObjectBuilder(dataAddressProperties)))
                .build();

        return seedProviderData(ASSETS_PATH, requestBody);
    }

    @NotNull
    public static String createPolicy() {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "PolicyDefinitionDto")
                .add("policy", noConstraintPolicy())
                .build();

        return seedProviderData(POLICIES_PATH, requestBody);
    }

    public static String createContractDefinition(String policyId) {

        var criteria = AssetSelectorExpression.Builder.newInstance()
                .constraint(EDC_NAMESPACE + "id",
                        "=",
                        PROVIDER_ASSET_ID)
                .build();

        var contractDefinition = Map.of(
                "id", "1",
                "accessPolicyId", policyId,
                "contractPolicyId", policyId,
                "criteria", criteria.getCriteria(),
                "validity", TimeUnit.HOURS.toSeconds(1)
        );

        return seedProviderData(CONTRACT_DEFINITIONS_PATH, contractDefinition);
    }

    private static String seedProviderData(String path, Object requestBody) {
        return givenProviderBaseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }

    private static RequestSpecification givenProviderBaseRequest() {
        return given()
                .baseUri(PROVIDER_CONNECTOR_MANAGEMENT_URL);
    }

    private static JsonObject noConstraintPolicy() {
        return createObjectBuilder()
                .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                .add(TYPE, "use")
                .build();
    }
}
