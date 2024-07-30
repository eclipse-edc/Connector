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

package org.eclipse.edc.sql.bootstrapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlSchemaBootstrapperImplTest {


    private final SqlSchemaBootstrapperImpl bootstrapper = new SqlSchemaBootstrapperImpl();

    @Test
    void addStatementFromResource() {
        assertThatNoException().isThrownBy(() -> bootstrapper.addStatementFromResource("foosource", "test-schema.sql"));
    }

    @Test
    void addStatementFromResource_resourceNotFound() {
        assertThatThrownBy(() -> bootstrapper.addStatementFromResource("foosource", "nonexist.sql"))
                .isInstanceOf(NullPointerException.class);
    }


}