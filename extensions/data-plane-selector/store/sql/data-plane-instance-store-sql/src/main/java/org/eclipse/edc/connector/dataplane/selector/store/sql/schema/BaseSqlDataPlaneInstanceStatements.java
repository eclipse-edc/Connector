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

package org.eclipse.edc.connector.dataplane.selector.store.sql.schema;

public class BaseSqlDataPlaneInstanceStatements implements DataPlaneInstanceStatements {

    @Override
    public String getFindByIdTemplate() {
        return String.format("SELECT * FROM %s WHERE %s = ?", getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public String getAllTemplate() {
        return String.format("SELECT * FROM %s", getDataPlaneInstanceTable());
    }

    @Override
    public String getInsertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .jsonColumn(getDataColumn())
                .insertInto(getDataPlaneInstanceTable());
    }

    @Override
    public String getUpdateTemplate() {
        return executeStatement()
                .jsonColumn(getDataColumn())
                .update(getDataPlaneInstanceTable(), getIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return executeStatement()
                .delete(getDataPlaneInstanceTable(), getIdColumn());
    }
}
