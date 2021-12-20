/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.clients.postgresql.asset.util;

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Sql {

    private static final String ASSET_CREATE = "asset_create.sql";
    private static final String ASSET_DELETE = "asset_delete.sql";
    private static final String ASSET_EXISTS = "asset_exists.sql";
    private static final String ASSET_QUERY = "asset_query.sql";
    private static final String ASSET_SELECT_ALL = "asset_select_all.sql";
    private static final String PROPERTY_CREATE = "property_create.sql";
    private static final String PROPERTY_DELETE = "property_delete.sql";
    private static final String PROPERTY_UPDATE = "property_update.sql";
    private static final String PROPERTIES_SELECT_BY_K_V = "properties_select_by_k_v.sql";
    private static final String PROPERTIES_SELECT_BY_ASSET_ID = "properties_select_by_asset_id.sql";

    public static String getAssetCreate() {
        return readContent(ASSET_CREATE);
    }

    public static String getAssetDelete() {
        return readContent(ASSET_DELETE);
    }

    public static String getAssetExists() {
        return readContent(ASSET_EXISTS);
    }

    public static String getAssetQuery() {
        return readContent(ASSET_QUERY);
    }

    public static String getAssetSelectAll() {
        return readContent(ASSET_SELECT_ALL);
    }

    public static String getPropertyCreate() {
        return readContent(PROPERTY_CREATE);
    }

    public static String getPropertyDelete() {
        return readContent(PROPERTY_DELETE);
    }

    public static String getPropertyUpdate() {
        return readContent(PROPERTY_UPDATE);
    }

    public static String getPropertiesSelectByKv() {
        return readContent(PROPERTIES_SELECT_BY_K_V);
    }

    public static String getPropertiesSelectByAssetId() {
        return readContent(PROPERTIES_SELECT_BY_ASSET_ID);
    }

    private static String readContent(String resourceName) {
        try (InputStream inputStream = Sql.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new EdcException(String.format("PostgreSQL-Asset: Resource not found  %s", resourceName));
            }

            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
