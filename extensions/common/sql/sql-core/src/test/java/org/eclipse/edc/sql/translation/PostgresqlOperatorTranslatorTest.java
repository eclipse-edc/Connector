/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlOperatorTranslatorTest {

    private final PostgresqlOperatorTranslator translator = new PostgresqlOperatorTranslator();

    @Test
    void shouldTranslate_equal() {
        var operator = translator.translate("=");

        assertThat(operator.representation()).isEqualTo("=");
        assertThat(operator.rightOperandClass()).isEqualTo(Object.class);
    }

    @Test
    void shouldTranslate_notEqual() {
        var operator = translator.translate("!=");

        assertThat(operator.representation()).isEqualTo("!=");
        assertThat(operator.rightOperandClass()).isEqualTo(Object.class);
    }

    @Test
    void shouldTranslate_like() {
        var operator = translator.translate("like");

        assertThat(operator.representation()).isEqualTo("like");
        assertThat(operator.rightOperandClass()).isEqualTo(String.class);
    }

    @Test
    void shouldTranslate_ilike() {
        var operator = translator.translate("ilike");

        assertThat(operator.representation()).isEqualTo("ilike");
        assertThat(operator.rightOperandClass()).isEqualTo(String.class);
    }

    @Test
    void shouldTranslate_in() {
        var operator = translator.translate("in");

        assertThat(operator.representation()).isEqualTo("in");
        assertThat(operator.rightOperandClass()).isEqualTo(Collection.class);
    }

    @Test
    void shouldTranslate_contains() {
        var operator = translator.translate("contains");

        assertThat(operator.representation()).isEqualTo("??");
        assertThat(operator.rightOperandClass()).isEqualTo(Object.class);
    }

    @Test
    void shouldTranslate_lessThan() {
        var operator = translator.translate("<");

        assertThat(operator.representation()).isEqualTo("<");
        assertThat(operator.rightOperandClass()).isEqualTo(Object.class);
    }

    @Test
    void shouldReturnNull_whenOperatorNotSupported() {
        var operator = translator.translate("not-supported");

        assertThat(operator).isNull();
    }
}
