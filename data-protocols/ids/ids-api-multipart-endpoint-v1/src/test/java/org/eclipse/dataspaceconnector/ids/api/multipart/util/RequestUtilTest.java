/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DescriptionRequestMessageImpl;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUtilTest {

    private static final int FROM = 0;
    private static final int TO = 10;

    private static final String PROPERTY = "property";
    private static final String VALUE = "value";
    private static final String EQUALS_SIGN = "=";
    private static final String FILTER_EXPRESSION = "filterExpression";
    private static final String OFFSET = "offset";
    private static final String LIMIT = "limit";

    @Test
    public void shouldExtractQuerySpec() {
        // given
        var message = (DescriptionRequestMessageImpl) new DescriptionRequestMessageBuilder().build();
        Map<String, Object> specsMap = new HashMap<>();

        specsMap.put(OFFSET, FROM);
        specsMap.put(LIMIT, TO);
        specsMap.put(FILTER_EXPRESSION, List.of(Map.of("operandLeft", PROPERTY, "operator", EQUALS_SIGN, "operandRight", VALUE)));
        message.setProperty(QuerySpec.QUERY_SPEC, specsMap);

        var expectedSpec = QuerySpec.Builder.newInstance().filter(PROPERTY + EQUALS_SIGN + VALUE).range(new Range(FROM, TO)).build();

        // when
        var result = RequestUtil.getQuerySpec(message, new ObjectMapper());

        // then
        assertThat(result).isEqualTo(expectedSpec);
    }

    @Test
    public void shouldExtractQuerySpec_defaultWhenMissing() {
        // given
        var message = (DescriptionRequestMessageImpl) new DescriptionRequestMessageBuilder().build();

        // when
        var result = RequestUtil.getQuerySpec(message, new ObjectMapper());

        // then
        assertThat(result).isEqualTo(QuerySpec.none());
    }
}