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

import java.util.Collection;

/**
 * Postgresql's implementation of the operator translator
 */
public class PostgresqlOperatorTranslator implements SqlOperatorTranslator {

    @Override
    public SqlOperator translate(String operator) {
        return switch (operator) {
            case "=" -> new SqlOperator("=", Object.class);
            case "like" -> new SqlOperator("like", String.class);
            case "in" -> new SqlOperator("in", Collection.class);
            case "contains" -> new SqlOperator("??", Object.class);
            default -> null;
        };
    }
}
