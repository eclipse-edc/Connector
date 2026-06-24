/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.spi.query;

import org.eclipse.edc.spi.message.Range;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuerySpecTest {

    @Test
    void verifyIllegalArguments() {
        assertThatThrownBy(() -> QuerySpec.Builder.newInstance().limit(-10).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuerySpec.Builder.newInstance().limit(0).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuerySpec.Builder.newInstance().offset(-10).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyDefaults() {
        var spec = QuerySpec.Builder.newInstance().build();
        var assertion = assertThat(spec);

        assertion.extracting(QuerySpec::getFilterExpression).isNotNull();
        assertion.extracting(QuerySpec::getLimit).isEqualTo(50);
        assertion.extracting(QuerySpec::getOffset).isEqualTo(0);
        assertion.extracting(QuerySpec::getSortOrder).isEqualTo(SortOrder.ASC);
        assertion.extracting(QuerySpec::getSortField).isNull();
    }

    @Test
    void verify_filterExprNull() {
        List<Criterion> filter = null;
        var qs = QuerySpec.Builder.newInstance().filter(filter).build();

        assertThat(qs.getFilterExpression()).isNotNull().isEmpty();
    }

    @Test
    void range_verifyCorrectConversion() {
        var spec = QuerySpec.Builder.newInstance()
                .range(new Range(37, 40))
                .build();

        assertThat(spec.getLimit()).isEqualTo(3);
        assertThat(spec.getOffset()).isEqualTo(37);

    }

    @Test
    void getRange_verifyCorrectConversion() {
        var spec = QuerySpec.Builder.newInstance()
                .limit(20)
                .offset(37)
                .build();

        var range = spec.getRange();
        assertThat(range.getFrom()).isEqualTo(37);
        assertThat(range.getTo()).isEqualTo(57);
    }

}
