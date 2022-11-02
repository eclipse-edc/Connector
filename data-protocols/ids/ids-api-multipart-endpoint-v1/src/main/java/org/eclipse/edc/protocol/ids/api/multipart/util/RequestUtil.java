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

package org.eclipse.edc.protocol.ids.api.multipart.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;

import static java.util.Optional.ofNullable;

public class RequestUtil {

    /**
     * Extracts an arbitrary property from a {@link DescriptionRequestMessage}
     *
     * @param message The message
     * @param objectMapper The objectMapper
     * @return either the property parsed into specific type, or the default value
     */
    public static QuerySpec getQuerySpec(@NotNull DescriptionRequestMessage message, ObjectMapper objectMapper) {
        return ofNullable(message.getProperties())
            .map(map -> map.get(QuerySpec.QUERY_SPEC))
            .map(specEntry -> objectMapper.convertValue(specEntry, QuerySpec.class))
            .orElse(QuerySpec.none());
    }
}