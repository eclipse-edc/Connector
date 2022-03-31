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

package org.eclipse.dataspaceconnector.sql.asset.index;


import static java.lang.String.format;

public class PostgresSqlAssetQueries implements SqlAssetQueries {


    @Override
    public String getSqlAssetInsertClause() {
        return format("INSERT INTO %s (%s) VALUES (?)", getAssetTable(), getAssetColumnId());
    }

    @Override
    public String getSqlDataAddressInsertClause() {
        return format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                getDataAddressTable(),
                getAssetColumnId(),
                getDataAddressColumnProperties());
    }

    @Override
    public String getSqlPropertyInsertClause() {
        return format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                getAssetPropertyTable(),
                getAssetColumnId(),
                getAssetPropertyColumnName(),
                getAssetPropertyColumnValue(),
                getAssetPropertyColumnType());
    }

    @Override
    public String getSqlAssetCountByIdClause() {
        return format("SELECT COUNT(*) AS %s FROM %s WHERE %s = ?",
                getCountVariableName(),
                getAssetTable(),
                getAssetColumnId());
    }

    @Override
    public String getSqlPropertyFindByIdClause() {
        return format("SELECT * FROM %s WHERE %s = ?",
                getAssetPropertyTable(),
                getAssetColumnId());
    }

    @Override
    public String getSqlDataAddressFindByIdClause() {
        return format("SELECT * FROM %s WHERE %s = ?",
                getDataAddressTable(),
                getAssetColumnId());
    }

    @Override
    public String getSqlAssetListClause() {
        return format("SELECT * FROM %s AS a", getAssetTable());
    }

    @Override
    public String getSqlAssetDeleteByIdClause() {
        return format("DELETE FROM %s WHERE %s = ?", getAssetTable(), getAssetColumnId());
    }

    @Override
    public String getSqlDataAddressDeleteByIdClause() {
        return format("DELETE FROM %s WHERE %s = ?", getDataAddressTable(), getAssetColumnId());
    }

    @Override
    public String getSqlPropertyDeleteByIdClause() {
        return format("DELETE FROM %s WHERE %s = ?", getAssetPropertyTable(), getAssetColumnId());
    }

    @Override
    public String getCountVariableName() {
        return "COUNT";
    }

    @Override
    public String getQuerySubSelectClause() {
        return format("EXISTS (SELECT 1 FROM %s AS eap WHERE eap.%s = a.%s AND eap.%s = ? AND eap.%s", getAssetPropertyTable(), getAssetColumnId(), getAssetColumnId(), getAssetPropertyColumnName(),
                getAssetPropertyColumnValue());
    }
}
