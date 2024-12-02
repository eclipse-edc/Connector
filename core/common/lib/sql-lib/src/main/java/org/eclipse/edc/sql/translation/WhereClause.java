/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import java.util.List;

/**
 * Represent a where condition clause in a prepared statement, like "column_table = ?"
 */
public record WhereClause(String sql, Collection<Object> parameters) {

    public WhereClause(String sql, Object parameter) {
        this(sql, List.of(parameter));
    }
}
