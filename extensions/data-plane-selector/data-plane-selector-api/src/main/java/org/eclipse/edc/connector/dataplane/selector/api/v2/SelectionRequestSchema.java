/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.api.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.edc.api.model.ApiCoreSchema;

import static org.eclipse.edc.connector.dataplane.selector.api.model.SelectionRequest.SELECTION_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@Schema(example = SelectionRequestSchema.SELECTION_REQUEST_INPUT_EXAMPLE)
public record SelectionRequestSchema(
        @Schema(name = TYPE, example = SELECTION_REQUEST_TYPE)
        String type,
        String strategy,
        String transferType,
        ApiCoreSchema.DataAddressSchema source,
        ApiCoreSchema.DataAddressSchema destination
) {
    public static final String SELECTION_REQUEST_INPUT_EXAMPLE = """
            {
                "@context": {
                    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                },
                "source": {
                    "@type": "https://w3id.org/edc/v0.0.1/ns/DataAddress",
                    "type": "test-src1"
                },
                "destination": {
                    "@type": "https://w3id.org/edc/v0.0.1/ns/DataAddress",
                    "type": "test-dst2"
                },
                "strategy": "you_custom_strategy",
                "transferType": "you_custom_transfer_type"
            }
            """;
}
