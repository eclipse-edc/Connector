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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import com.azure.cosmos.models.SqlParameter;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhereClauseTest {


    @Test
    void whereClause_withEquals() {
        var wc = new WhereClause(List.of(new Criterion("foo", "=", "bar")), "testobj");
        assertThat(wc.getWhere()).isEqualTo("WHERE testobj.foo = @foo");
        var listAssert = assertThat(wc.getParameters());
        listAssert.extracting(SqlParameter::getName).containsOnly("@foo");
    }

    @Test
    void whereClause_withIn() {
        var wc = new WhereClause(List.of(new Criterion("foo", "IN", List.of("bar1", "bar2"))), "testobj");
        assertThat(wc.getWhere()).isEqualTo("WHERE testobj.foo IN (@foo0, @foo1)");
        assertThat(wc.getParameters()).hasSize(2)
                .anyMatch(p -> p.getName().equals("@foo0") && p.getValue(String.class).equals("bar1"))
                .anyMatch(p -> p.getName().equals("@foo1") && p.getValue(String.class).equals("bar2"));
    }

    @Test
    void whereClause_withInvalidOperator() {
        assertThatThrownBy(() -> new WhereClause(List.of(new Criterion("foo", "BEGINSWITH", "bar3")), "testobj")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void whereClause_withLowercaseOperator() {
        var wc = new WhereClause(List.of(new Criterion("foo", "in", List.of("bar1", "bar2"))), "testobj");
        assertThat(wc.getWhere()).isEqualTo("WHERE testobj.foo in (@foo0, @foo1)");
        assertThat(wc.getParameters()).hasSize(2)
                .anyMatch(p -> p.getName().equals("@foo0") && p.getValue(String.class).equals("bar1"))
                .anyMatch(p -> p.getName().equals("@foo1") && p.getValue(String.class).equals("bar2"));
    }

    @Test
    void whereClause_withMultipleCriteria_withParams() {
        var wc = new WhereClause(List.of(
                new Criterion("foo", "in", List.of("bar1", "bar2")),
                new Criterion("foo", "=", "barbaz")),
                "testobj");
        assertThat(wc.getWhere()).isEqualTo("WHERE testobj.foo in (@foo0, @foo1) AND testobj.foo = @foo");
        assertThat(wc.getParameters()).hasSize(3)
                .anyMatch(p -> p.getName().equals("@foo0") && p.getValue(String.class).equals("bar1"))
                .anyMatch(p -> p.getName().equals("@foo1") && p.getValue(String.class).equals("bar2"))
                .anyMatch(p -> p.getName().equals("@foo") && p.getValue(String.class).equals("barbaz"));
    }

    @Test
    void whereClause_withMultipleInStatements() {
        var wc = new WhereClause(List.of(
                new Criterion("foo", "in", List.of("bar1", "bar2")),
                new Criterion("foo", "in", List.of("blubb", "blabb"))),
                "testobj");
        assertThat(wc.getWhere()).isEqualTo("WHERE testobj.foo in (@foo0, @foo1) AND testobj.foo in (@foo0, @foo1)");
        assertThat(wc.getParameters()).hasSize(4)
                .anyMatch(p -> p.getName().equals("@foo0") && p.getValue(String.class).equals("bar1"))
                .anyMatch(p -> p.getName().equals("@foo1") && p.getValue(String.class).equals("bar2"))
                .anyMatch(p -> p.getName().equals("@foo0") && p.getValue(String.class).equals("blubb"))
                .anyMatch(p -> p.getName().equals("@foo1") && p.getValue(String.class).equals("blabb"));
    }
}
