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

package org.eclipse.edc.edr.store.index.sql.schema;

import org.eclipse.edc.sql.translation.SqlOperatorTranslator;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements JtiValidationStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getJtiValidationTable(), getTokenIdColumn());
    }


    @Override
    public String getFindByTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getJtiValidationTable(), getTokenIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getTokenIdColumn())
                .column(getExpirationTimeColumn())
                .insertInto(getJtiValidationTable());
    }

}
