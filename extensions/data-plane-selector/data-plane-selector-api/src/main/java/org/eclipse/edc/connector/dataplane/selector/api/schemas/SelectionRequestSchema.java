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

package org.eclipse.edc.connector.dataplane.selector.api.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

import static org.eclipse.edc.connector.dataplane.selector.api.SelectionRequest.SELECTION_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@Schema(example = SelectionRequestSchema.SELECTION_REQUEST_INPUT_EXAMPLE)
public record SelectionRequestSchema(
        @Schema(name = TYPE, example = SELECTION_REQUEST_TYPE)
        String type,
        String strategy,
        DataAddressSchema source,
        DataAddressSchema destination
) {
    public static final String SELECTION_REQUEST_INPUT_EXAMPLE = """
            {
                "source": {
                    "@type": "https://w3id.org/edc/v0.0.1/ns/DataAddress",
                    "type": "test-src1"
                },
                "destination": {
                    "@type": "https://w3id.org/edc/v0.0.1/ns/DataAddress",
                    "type": "test-dst2"
                },
                "strategy": "you_custom_strategy"
            }
            """;
}
