/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema;

import org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema.postgres.DataspaceProfileMapping;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlDialectStatements implements DataspaceProfileStoreStatements {

    protected final SqlOperatorTranslator operatorTranslator;

    public BaseSqlDialectStatements(SqlOperatorTranslator operatorTranslator) {
        this.operatorTranslator = operatorTranslator;
    }

    @Override
    public String getSelectTemplate() {
        return format("SELECT * FROM %s", getProfileTable());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getNameColumn())
                .column(getProtocolVersionColumn())
                .column(getPathColumn())
                .column(getBindingColumn())
                .column(getNamespaceColumn())
                .jsonColumn(getJsonLdContextsUrlColumn())
                .column(getCreatedAtColumn())
                .insertInto(getProfileTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .column(getProtocolVersionColumn())
                .column(getPathColumn())
                .column(getBindingColumn())
                .column(getNamespaceColumn())
                .jsonColumn(getJsonLdContextsUrlColumn())
                .update(getProfileTable(), getNameColumn());
    }

    @Override
    public String getDeleteTemplate() {
        return executeStatement().delete(getProfileTable(), getNameColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        return new SqlQueryStatement(getSelectTemplate(), querySpec, new DataspaceProfileMapping(this), operatorTranslator);
    }
}
