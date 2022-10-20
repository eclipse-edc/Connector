/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.store.sql.contractdefinition.schema.postgres;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.sql.translation.JsonFieldMapping;

/**
 * Maps fields of a {@link Criterion} onto the corresponding SQL schema (=
 * column names)
 */
class CriterionMapping extends JsonFieldMapping {

    CriterionMapping() {
        super("criteria");
    }

    @Override
    public String getStatement(String canonicalPropertyName) {
        var statementBuilder = new StringBuilder("criteria");
        if (canonicalPropertyName == null) {
            statementBuilder.append("::text");
            return statementBuilder.toString();
        }
        var tokens = canonicalPropertyName.split("\\.");

        int length = tokens.length;
        for (int i = 0; i < length - 1; i++) {
            statementBuilder.append("-> ");
            statementBuilder.append("'").append(tokens[i]).append("' ");
        }

        statementBuilder.append(" ->> ");
        statementBuilder.append("'").append(tokens[length - 1]).append("'");
        return statementBuilder.toString();
    }
}
