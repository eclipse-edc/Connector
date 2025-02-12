/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.util.reflection.PathItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class JsonArrayTranslatorTest {

    private final JsonArrayTranslator translator = new JsonArrayTranslator("array");

    @Test
    void getLeftOperand() {
        var result = translator.getLeftOperand(PathItem.parse("array"), Object.class);
        assertThat(result).isEqualTo("array");
    }

    @Test
    void getLeftOperand_invalidPath() {
        assertThatThrownBy(() -> translator.getLeftOperand(PathItem.parse("someobject.array"), Object.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toWhereClause() {
        var operator = new SqlOperator("??", Object.class);
        var criterion = criterion("array", "contains", "value");

        var result = translator.toWhereClause(PathItem.parse("array"), criterion, operator);

        assertThat(result.sql()).isEqualTo("array::jsonb ?? ?");
        assertThat(result.parameters()).containsExactly("value");
    }

    @Test
    void toWhereClause_rightOperandIsList_invalid() {
        var operator = new SqlOperator("??", Object.class);
        var criterion = criterion("array", "contains", List.of("val1", "val2"));

        assertThatThrownBy(() -> translator.toWhereClause(PathItem.parse("array"), criterion, operator))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    void toWhereClause_invalidOperator() {
        var operator = new SqlOperator("=", Object.class);
        var criterion = criterion("array", "contains", "value");


        assertThatThrownBy(() -> translator.toWhereClause(PathItem.parse("array"), criterion, operator))
                .isInstanceOf(IllegalArgumentException.class);
    }
}