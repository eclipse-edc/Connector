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
import org.eclipse.edc.spi.types.domain.DataAddress;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public record DataAddressSchema(
        @Schema(name = TYPE, example = DataAddress.EDC_DATA_ADDRESS_TYPE)
        String type,
        @Schema(name = "type")
        String typeProperty
) {
}
