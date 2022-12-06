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

package org.eclipse.edc.connector.dataplane.store.sql.schema;

public class BaseSqlDataPlaneStatements implements DataPlaneStatements {


    @Override
    public String getFindByIdTemplate() {
        return String.format("SELECT * FROM %s WHERE %s = ?", getDataPlaneTable(), getProcessIdColumn());
    }


    @Override
    public String getInsertTemplate() {
        return String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                getDataPlaneTable(),
                getProcessIdColumn(),
                getStateColumn(),
                getCreatedAtColumn(),
                getUpdatedAtColumn()
        );
    }

    @Override
    public String getUpdateTemplate() {
        return String.format("UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                getDataPlaneTable(),
                getStateColumn(),
                getUpdatedAtColumn(),
                getProcessIdColumn());
    }
}
