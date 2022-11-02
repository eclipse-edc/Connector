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

package org.eclipse.edc.azure.cosmos.dialect;

import org.eclipse.edc.azure.cosmos.TestCollectionDocument;
import org.eclipse.edc.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CosmosExistsExpressionTest {

    private final String objectPrefix = "test";

    @Test
    void isInvalidExpression_withNoMatchedFields() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("foo", "in", List.of("bar")), objectPrefix);
        assertThat(expr).isNull();

    }

    @Test
    void isInvalidExpression_withNoCollectionField() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("embedded", "=", "bar"), objectPrefix);
        assertThat(expr).isNull();

    }

    @Test
    void isInvalidExpression_withTerminatorCollectionField() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("embedded.collections", "=", "bar"), objectPrefix);
        assertThat(expr).isNull();

    }


    @Test
    void isValidExpression() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("embedded.collections.name", "in", List.of("bar")), objectPrefix);
        assertThat(expr).isNotNull();

    }

    @Test
    void isValidExpression_invalidOperator() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("embedded.collections.name", "is_subset_of", List.of("bar")), objectPrefix);
        assertThat(expr).isNotNull();
        assertThat(expr.isValidExpression().succeeded()).isFalse();
    }

    @Test
    void isValidExpression_wrongOperand() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("embedded.collections.name", "is_subset_of", List.of("bar")), objectPrefix);
        assertThat(expr).isNotNull();
        assertThat(expr.isValidExpression().succeeded()).isFalse();

    }

    @Test
    void toExpressionString() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("embedded.collections.name", "=", "baz"), objectPrefix);
        assertThat(expr).isNotNull();
        assertThat(expr.toExpressionString()).isEqualToIgnoringWhitespace("EXISTS(SELECT VALUE t FROM t IN test.embedded.collections WHERE t.name = @embedded_collections_name)");
    }

    @Test
    void toExpressionStringQuoted() {
        var expr = CosmosExistsExpression.parse(TestCollectionDocument.TestWrappedTarget.class, new Criterion("embedded.collections.value", "=", "baz"), objectPrefix);
        assertThat(expr).isNotNull();
        assertThat(expr.toExpressionString()).isEqualToIgnoringWhitespace("EXISTS(SELECT VALUE t FROM t IN test.embedded.collections WHERE t[\"value\"] = @embedded_collections_value)");
    }

}
