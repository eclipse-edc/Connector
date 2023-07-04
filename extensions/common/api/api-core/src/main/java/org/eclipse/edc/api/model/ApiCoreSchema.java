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

import java.util.List;

import static org.eclipse.edc.api.model.CriterionDto.CRITERION_TYPE;
import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public interface ApiCoreSchema {

    @Schema(example = CriterionSchema.CRITERION_EXAMPLE)
    record CriterionSchema(
            @Schema(name = TYPE, example = CRITERION_TYPE)
            String type,
            Object operandLeft,
            String operator,
            Object operandRight) {

        public static final String CRITERION_EXAMPLE = """
                {
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "operandLeft": "fieldName",
                    "operator": "=",
                    "operandRight": "some value"
                }
                """;
    }

    @Schema(example = QuerySpecSchema.QUERY_SPEC_EXAMPLE)
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
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "offset": 5,
                    "limit": 10,
                    "sortOrder": "DESC",
                    "sortField": "fieldName",
                    "criterion": []
                }
                """;
    }

    @Schema(example = IdResponseSchema.ID_RESPONSE_EXAMPLE)
    record IdResponseSchema(
            @Schema(name = ID)
            String id,
            long createdAt
    ) {
        public static final String ID_RESPONSE_EXAMPLE = """
                {
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "id-value",
                    "createdAt": 1688465655
                }
                """;
    }
}
