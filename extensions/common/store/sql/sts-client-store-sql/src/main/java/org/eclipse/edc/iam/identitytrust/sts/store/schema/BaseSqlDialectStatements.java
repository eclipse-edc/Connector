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

package org.eclipse.edc.iam.identitytrust.sts.store.schema;

import org.eclipse.edc.iam.identitytrust.sts.store.schema.postgres.StsClientMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements StsClientStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement().delete(getStsClientTable(), getIdColumn());
    }

    @Override
    public String getFindByTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getStsClientTable(), getIdColumn());
    }

    @Override
    public String getFindByClientIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getStsClientTable(), getClientIdColumn());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getNameColumn())
                .column(getClientIdColumn())
                .column(getDidColumn())
                .column(getSecretAliasColumn())
                .column(getPrivateKeyAliasColumn())
                .column(getPublicKeyReferenceColumn())
                .column(getCreatedAtColumn())
                .insertInto(getStsClientTable());
    }


    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getNameColumn())
                .column(getClientIdColumn())
                .column(getDidColumn())
                .column(getSecretAliasColumn())
                .column(getPrivateKeyAliasColumn())
                .column(getPublicKeyReferenceColumn())
                .column(getCreatedAtColumn())
                .update(getStsClientTable(), getIdColumn());

    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = format("SELECT * FROM %s", getStsClientTable());
        return new SqlQueryStatement(select, querySpec, new StsClientMapping(this), operatorTranslator);
    }

}
