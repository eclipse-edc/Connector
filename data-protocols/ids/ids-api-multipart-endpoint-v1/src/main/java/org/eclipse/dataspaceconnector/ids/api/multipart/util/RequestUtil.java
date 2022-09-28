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

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class RequestUtil {

    /**
     * Extracts an arbitrary property from a {@link DescriptionRequestMessage}
     *
     * @param message The message
     * @return either the property parsed into specific type, or the default value
     */
    public static QuerySpec getQuerySpec(@NotNull DescriptionRequestMessage message) {
        return ofNullable(message.getProperties())
            .map(map -> (Map) map.get(QuerySpec.QUERY_SPEC))
            .map(spec -> QuerySpec.Builder.newInstance().filter(getFilter(spec)).range(getRange(spec)).build())
            .orElse(QuerySpec.none());
    }

    private static Range getRange(Map map) {
        return ofNullable(map.get(CatalogRequest.RANGE))
            .map(v -> mapToRange((Map) v))
            .orElse(new Range());
    }

    private static List<Criterion> getFilter(Map map) {
        return ofNullable(map.get(QuerySpec.FILTER_EXPRESSION))
            .map(v -> mapToListOfCriteria((List<Map>) v))
            .orElse(Collections.emptyList());
    }

    private static Range mapToRange(Map map) {
        return new Range((int) map.get(Range.FROM), (int) map.get(Range.TO));
    }

    private static List<Criterion> mapToListOfCriteria(List<Map> maps) {
        return maps.stream().map(map -> new Criterion(map.get("operandLeft"), (String) map.get("operator"), map.get("operandRight")))
            .collect(Collectors.toList());
    }
}