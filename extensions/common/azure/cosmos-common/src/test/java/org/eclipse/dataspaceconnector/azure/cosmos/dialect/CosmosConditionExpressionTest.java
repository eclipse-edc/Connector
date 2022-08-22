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

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CosmosConditionExpressionTest {


    private final String objectPrefix = "test";

    @Test
    void isValidExpression() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "in", List.of("bar")), objectPrefix);
        assertThat(expr.isValidExpression().succeeded()).isTrue();

    }

    @Test
    void isValidExpression_wrongOperand() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "in", "bar"), objectPrefix);
        assertThat(expr.isValidExpression().succeeded()).isFalse();

    }

    @Test
    void isValidExpression_invalidOperator() {
        var expr2 = new CosmosConditionExpression(new Criterion("foo", "is_subset_of", List.of("bar")), objectPrefix);
        assertThat(expr2.isValidExpression().succeeded()).isFalse();
    }

    @Test
    void toExpressionString_withList() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "in", List.of("bar", "baz")), objectPrefix);
        assertThat(expr.toExpressionString()).isEqualToIgnoringWhitespace("test.foo in (@foo0, @foo1)");
    }

    @Test
    void toExpressionString() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "=", "baz"), objectPrefix);
        assertThat(expr.toExpressionString()).isEqualToIgnoringWhitespace("test.foo = @foo");
    }

    @Test
    void toExpressionString_withList_noPrefix() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "in", List.of("bar", "baz")));
        assertThat(expr.toExpressionString()).isEqualToIgnoringWhitespace("foo in (@foo0, @foo1)");
    }

    @Test
    void toExpressionString_noPrefix() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "=", "baz"));
        assertThat(expr.toExpressionString()).isEqualToIgnoringWhitespace("foo = @foo");
    }

    @Test
    void toParameters() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "=", "baz"), objectPrefix);
        assertThat(expr.getParameters()).hasSize(1).allSatisfy(c -> {
            assertThat(c.getName()).isEqualTo("@foo");
            assertThat(c.getValue(String.class)).isEqualTo("baz");
        });
    }

    @Test
    void toParameters_list() {
        var expr = new CosmosConditionExpression(new Criterion("foo", "in", List.of("bar", "baz")), objectPrefix);
        assertThat(expr.getParameters()).hasSize(2)
                .anySatisfy(c -> {
                    assertThat(c.getName()).isEqualTo("@foo0");
                    assertThat(c.getValue(String.class)).isEqualTo("bar");
                })
                .anySatisfy(c -> {
                    assertThat(c.getName()).isEqualTo("@foo1");
                    assertThat(c.getValue(String.class)).isEqualTo("baz");
                });
    }
}