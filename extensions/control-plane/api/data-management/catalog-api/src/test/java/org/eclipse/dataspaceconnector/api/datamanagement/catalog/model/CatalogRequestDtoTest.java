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

package org.eclipse.dataspaceconnector.api.datamanagement.catalog.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogRequestDtoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void verifySerdes() throws JsonProcessingException {
        var dto = CatalogRequestDto.Builder.newInstance()
                .providerUrl("https://some.provider/path")
                .limit(420)
                .offset(69)
                .sortField("someField")
                .sortOrder(SortOrder.DESC)
                .filter(List.of(new Criterion("foo", "=", "bar"), new Criterion("bar", "<", "baz")))
                .build();

        var json = MAPPER.writeValueAsString(dto);
        assertThat(json).isNotNull();

        var deser = MAPPER.readValue(json, CatalogRequestDto.class);
        assertThat(deser).usingRecursiveComparison().isEqualTo(dto);
    }
}