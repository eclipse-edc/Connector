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

package org.eclipse.edc.catalog.api.query;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.ApiCoreSchema;

@OpenAPIDefinition(
        info = @Info(description = "This represents the Federated Catalog API. It serves the cached Catalogs fetched from the data providers.",
                title = "Federated Catalog API", version = "v1"))
@Tag(name = "Federated Catalog")
public interface FederatedCatalogApi {
    @Operation(description = "Obtains all Catalog currently held by this cache instance",
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))),
            parameters = @Parameter(name = "flatten", description = "Whether the resulting root catalog should be 'flattened' or contain a hierarchy of catalogs"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of Catalog is returned, potentially empty",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogSchema.class)))),
                    @ApiResponse(responseCode = "500", description = "A Query could not be completed due to an internal error")
            }

    )
    JsonArray getCachedCatalog(JsonObject querySpec, boolean flatten);

    @Schema(name = "Catalog", description = "DCAT catalog", example = CatalogSchema.CATALOG_EXAMPLE)
    record CatalogSchema(
    ) {
        public static final String CATALOG_EXAMPLE = """
                {
                    "@id": "7df65569-8c59-4013-b1c0-fa14f6641bf2",
                    "@type": "dcat:Catalog",
                    "dcat:dataset": {
                        "@id": "bcca61be-e82e-4da6-bfec-9716a56cef35",
                        "@type": "dcat:Dataset",
                        "odrl:hasPolicy": {
                            "@id": "OGU0ZTMzMGMtODQ2ZS00ZWMxLThmOGQtNWQxNWM0NmI2NmY4:YmNjYTYxYmUtZTgyZS00ZGE2LWJmZWMtOTcxNmE1NmNlZjM1:NDY2ZTZhMmEtNjQ1Yy00ZGQ0LWFlZDktMjdjNGJkZTU4MDNj",
                            "@type": "odrl:Set",
                            "odrl:permission": {
                                "odrl:target": "bcca61be-e82e-4da6-bfec-9716a56cef35",
                                "odrl:action": {
                                    "odrl:type": "http://www.w3.org/ns/odrl/2/use"
                                },
                                "odrl:constraint": {
                                    "odrl:and": [
                                        {
                                            "odrl:leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
                                            "odrl:operator": {
                                                "@id": "odrl:gteq"
                                            },
                                            "odrl:rightOperand": "2023-07-07T07:19:58.585601395Z"
                                        },
                                        {
                                            "odrl:leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
                                            "odrl:operator": {
                                                "@id": "odrl:lteq"
                                            },
                                            "odrl:rightOperand": "2023-07-12T07:19:58.585601395Z"
                                        }
                                    ]
                                }
                            },
                            "odrl:prohibition": [],
                            "odrl:obligation": []
                        },
                        "dcat:distribution": [
                            {
                                "@type": "dcat:Distribution",
                                "dct:format": {
                                    "@id": "HttpData"
                                },
                                "dcat:accessService": "5e839777-d93e-4785-8972-1005f51cf367"
                            }
                        ],
                        "description": "description",
                        "id": "bcca61be-e82e-4da6-bfec-9716a56cef35"
                    },
                    "dcat:service": {
                        "@id": "5e839777-d93e-4785-8972-1005f51cf367",
                        "@type": "dcat:DataService",
                        "dct:terms": "connector",
                        "dcat:endpointURL": "http://localhost:16806/protocol"
                    },
                    "dspace:participantId": "urn:connector:provider",
                    "@context": {
                        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                        "dct": "http://purl.org/dc/terms/",
                        "edc": "https://w3id.org/edc/v0.0.1/ns/",
                        "dcat": "http://www.w3.org/ns/dcat#",
                        "odrl": "http://www.w3.org/ns/odrl/2/",
                        "dspace": "https://w3id.org/dspace/2025/1/"
                    }
                }
                """;
    }
}

