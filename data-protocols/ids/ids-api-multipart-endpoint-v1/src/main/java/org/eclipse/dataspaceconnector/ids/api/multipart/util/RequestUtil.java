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

package org.eclipse.dataspaceconnector.ids.api.multipart.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

public class RequestUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Extracts an arbitrary property from a {@link DescriptionRequestMessage}
     *
     * @param message The message
     * @return either the property parsed into specific type, or the default value
     */
    public static Range getRange(@NotNull DescriptionRequestMessage message) {
        return ofNullable(message.getProperties().get(CatalogRequest.RANGE))
            .map(v -> MAPPER.convertValue(v, Range.class))
            .orElse(new Range());
    }

    public static List<Criterion> getFilter(@NotNull DescriptionRequestMessage message) {
        return ofNullable(message.getProperties().get(CatalogRequest.FILTER))
            .map(v -> MAPPER.convertValue(v, new TypeReference<List<Criterion>>() {}))
            .orElse(Collections.emptyList());
    }
}
