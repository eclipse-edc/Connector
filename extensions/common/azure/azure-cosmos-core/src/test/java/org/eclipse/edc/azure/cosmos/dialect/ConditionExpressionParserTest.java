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

import static org.assertj.core.api.Assertions.assertThat;

public class ConditionExpressionParserTest {
    private final String objectPrefix = "test";

    @Test
    void conditionExpression_isPathExpression_withoutTarget() {
        assertThat(new ConditionExpressionParser(null).parse(new Criterion("foo", "in", "bar"), objectPrefix)).isInstanceOf(CosmosPathConditionExpression.class);
    }

    @Test
    void conditionExpression_isPathExpression_withTarget() {
        assertThat(new ConditionExpressionParser(TestCollectionDocument.class).parse(new Criterion("foo", "in", "bar"), objectPrefix)).isInstanceOf(CosmosPathConditionExpression.class);
    }

    @Test
    void conditionExpression_isPathExpression_withTerminatorCollectionField() {
        assertThat(new ConditionExpressionParser(TestCollectionDocument.class).parse(new Criterion("embedded.collections", "=", "bar"), objectPrefix)).isInstanceOf(CosmosPathConditionExpression.class);
    }

    @Test
    void conditionExpression_isExistsExpression() {
        assertThat(new ConditionExpressionParser(TestCollectionDocument.class).parse(new Criterion("embedded.collections.name", "=", "bar"), objectPrefix)).isInstanceOf(CosmosExistsExpression.class);
    }


}
