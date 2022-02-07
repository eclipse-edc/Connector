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

package org.eclipse.dataspaceconnector.spi.query;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagingSpecTest {

    @Test
    void verifyIllegalArguments() {
        assertThatThrownBy(() -> PagingSpec.Builder.newInstance().limit(-10).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PagingSpec.Builder.newInstance().limit(0).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PagingSpec.Builder.newInstance().offset(-10).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyDefaults() {
        var spec = PagingSpec.Builder.newInstance().build();
        var assertion = assertThat(spec);

        assertion.extracting(PagingSpec::getFilterExpression).isNull();
        assertion.extracting(PagingSpec::getLimit).isEqualTo(50);
        assertion.extracting(PagingSpec::getOffset).isEqualTo(0);
        assertion.extracting(PagingSpec::getSortOrder).isEqualTo(SortOrder.DESC);
        assertion.extracting(PagingSpec::getSortField).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "name=foo", "name = foo", "name =      foo", "name contains foo" })
    void verifyEqualsFilterExpressions(String equalityExp) {
        var spec = PagingSpec.Builder.newInstance().filter(equalityExp).build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("name", "contains", "foo"));
    }

    @Test
    void verifyFilterExpression() {
        var spec = PagingSpec.Builder.newInstance().filter("age < 14").build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("age", "<", "14"));

        var spec2 = PagingSpec.Builder.newInstance().filter("age     <   14").build();
        assertThat(spec2.getFilterExpression()).hasSize(1).containsOnly(new Criterion("age", "<", "14"));

        var spec3 = PagingSpec.Builder.newInstance().filter("description endsWith 'foobar'").build();
        assertThat(spec3.getFilterExpression()).hasSize(1).containsOnly(new Criterion("description", "endsWith", "'foobar'"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "age<14", "namecontainssomething", "id_like_14" })
    void verify_invalidFilterExpression(String expr) {
        assertThatThrownBy(() -> PagingSpec.Builder.newInstance().filter(expr).build()).isInstanceOf(IllegalArgumentException.class);
    }
}