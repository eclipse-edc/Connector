/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.catalog.transform.CatalogError;
import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_ERROR;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_PROPERTY_REASON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromCatalogErrorTransformerTest {

    private JsonObjectFromCatalogErrorTransformer transformer;

    private TransformerContext context = mock(TransformerContext.class);

    private DspHttpStatusCodeMapper statusCodeMapper = mock(DspHttpStatusCodeMapper.class);

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCatalogErrorTransformer(statusCodeMapper);
    }

    @Test
    void transErrorToResponseWithId() {
        when(statusCodeMapper.mapErrorToStatusCode(any(InvalidRequestException.class))).thenReturn(400);

        var transferError = new CatalogError(new InvalidRequestException("testError"));

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CATALOG_ERROR);
        assertThat(result.getJsonString(DSPACE_CATALOG_PROPERTY_CODE).getString()).isEqualTo("400");
        assertThat(result.get(DSPACE_CATALOG_PROPERTY_REASON)).isNotNull();

    }

    @Test
    void transferErrorWithoutReason() {
        when(statusCodeMapper.mapErrorToStatusCode(any(Exception.class))).thenReturn(500);

        var transferError = new CatalogError(new Exception());

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();

        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CATALOG_ERROR);
        assertThat(result.getJsonString(DSPACE_CATALOG_PROPERTY_CODE).getString()).isEqualTo("500");
        assertThat(result.containsKey(DSPACE_SCHEMA + "reason")).isFalse();
    }
}
