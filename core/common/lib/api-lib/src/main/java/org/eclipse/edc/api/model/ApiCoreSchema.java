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

package org.eclipse.edc.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;

public interface ApiCoreSchema {

    @Schema(name = "Criterion", example = CriterionSchema.CRITERION_EXAMPLE)
    record CriterionSchema(
            @Schema(name = TYPE, example = CRITERION_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            Object operandLeft,
            @Schema(requiredMode = REQUIRED)
            String operator,
            @Schema(requiredMode = REQUIRED)
            Object operandRight) {

        public static final String CRITERION_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "Criterion",
                    "operandLeft": "fieldName",
                    "operator": "=",
                    "operandRight": "some value"
                }
                """;
    }

    @Schema(name = "QuerySpec", example = QuerySpecSchema.QUERY_SPEC_EXAMPLE)
    record QuerySpecSchema(
            @Schema(name = TYPE, example = EDC_QUERY_SPEC_TYPE)
            String type,
            int offset,
            int limit,
            SortOrder sortOrder,
            String sortField,
            List<CriterionSchema> filterExpression
    ) {
        public static final String QUERY_SPEC_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "QuerySpec",
                    "offset": 5,
                    "limit": 10,
                    "sortOrder": "DESC",
                    "sortField": "fieldName",
                    "filterExpression": []
                }
                """;
    }

    @Schema(name = "IdResponse", example = IdResponseSchema.ID_RESPONSE_EXAMPLE)
    record IdResponseSchema(
            @Schema(name = ID)
            String id,
            long createdAt
    ) {
        public static final String ID_RESPONSE_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "id-value",
                    "createdAt": 1688465655
                }
                """;
    }

    @Schema(name = "ApiErrorDetail", example = ApiErrorDetailSchema.API_ERROR_EXAMPLE)
    record ApiErrorDetailSchema(
            String message,
            String type,
            String path,
            String invalidValue
    ) {
        public static final String API_ERROR_EXAMPLE = """
                {
                    "message": "error message",
                    "type": "ErrorType",
                    "path": "object.error.path",
                    "invalidValue": "this value is not valid"
                }
                """;
    }

    @Schema(name = "DataAddress", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
    record DataAddressSchema(
            @Schema(name = TYPE, example = DataAddress.EDC_DATA_ADDRESS_TYPE)
            String type,
            @Schema(name = "type")
            String typeProperty
    ) {
        public static final String DATA_ADDRESS_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "https://w3id.org/edc/v0.0.1/ns/DataAddress",
                    "type": "HttpData",
                    "baseUrl": "http://example.com"
                }
                """;
    }
}
