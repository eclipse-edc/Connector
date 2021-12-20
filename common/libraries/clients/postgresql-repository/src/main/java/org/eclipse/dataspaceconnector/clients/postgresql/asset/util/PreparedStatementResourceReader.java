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

public class PreparedStatementResourceReader {

    private static final String ADDRESS_CREATE = "address_create.sql";
    private static final String ADDRESS_DELETE = "address_delete.sql";
    private static final String ADDRESS_EXISTS = "address_exists.sql";
    private static final String ADDRESS_PROPERTIES_SELECT_BY_ADDRESS_ID = "address_properties_select_by_address_id.sql";
    private static final String ADDRESS_PROPERTIES_SELECT_BY_K_V = "address_properties_select_by_k_v.sql";
    private static final String ADDRESS_PROPERTY_CREATE = "address_property_create.sql";
    private static final String ADDRESS_PROPERTY_DELETE = "address_property_delete.sql";
    private static final String ADDRESS_PROPERTY_UPDATE = "address_property_update.sql";
    private static final String ADDRESS_QUERY = "address_query.sql";
    private static final String ADDRESS_SELECT_ALL = "address_select_all.sql";
    private static final String ASSET_CREATE = "asset_create.sql";
    private static final String ASSET_DELETE = "asset_delete.sql";
    private static final String ASSET_EXISTS = "asset_exists.sql";
    private static final String ASSET_QUERY = "asset_query.sql";
    private static final String ASSET_SELECT_ALL = "asset_select_all.sql";
    private static final String ASSET_PROPERTY_CREATE = "asset_property_create.sql";
    private static final String ASSET_PROPERTY_DELETE = "asset_property_delete.sql";
    private static final String ASSET_PROPERTY_UPDATE = "asset_property_update.sql";
    private static final String ASSET_PROPERTIES_SELECT_BY_K_V = "asset_properties_select_by_k_v.sql";
    private static final String ASSET_PROPERTIES_SELECT_BY_ASSET_ID = "asset_properties_select_by_asset_id.sql";

    public static String readAddressCreate() {
        return readContent(ADDRESS_CREATE);
    }

    public static String readAddressDelete() {
        return readContent(ADDRESS_DELETE);
    }

    public static String readAddressExists() {
        return readContent(ADDRESS_EXISTS);
    }

    public static String readAddressQuery() {
        return readContent(ADDRESS_QUERY);
    }

    public static String readAddressSelectAll() {
        return readContent(ADDRESS_SELECT_ALL);
    }

    public static String readAddressPropertyCreate() {
        return readContent(ADDRESS_PROPERTY_CREATE);
    }

    public static String readAddressPropertyDelete() {
        return readContent(ADDRESS_PROPERTY_DELETE);
    }

    public static String readAddressPropertyUpdate() {
        return readContent(ADDRESS_PROPERTY_UPDATE);
    }

    public static String readAddressPropertiesSelectByKv() {
        return readContent(ADDRESS_PROPERTIES_SELECT_BY_K_V);
    }

    public static String readAddressPropertiesSelectByAddressId() {
        return readContent(ADDRESS_PROPERTIES_SELECT_BY_ADDRESS_ID);
    }

    public static String readAssetCreate() {
        return readContent(ASSET_CREATE);
    }

    public static String readAssetDelete() {
        return readContent(ASSET_DELETE);
    }

    public static String readAssetExists() {
        return readContent(ASSET_EXISTS);
    }

    public static String readAssetQuery() {
        return readContent(ASSET_QUERY);
    }

    public static String readAssetSelectAll() {
        return readContent(ASSET_SELECT_ALL);
    }

    public static String readAssetPropertyCreate() {
        return readContent(ASSET_PROPERTY_CREATE);
    }

    public static String readAssetPropertyDelete() {
        return readContent(ASSET_PROPERTY_DELETE);
    }

    public static String readAssetPropertyUpdate() {
        return readContent(ASSET_PROPERTY_UPDATE);
    }

    public static String readAssetPropertiesSelectByKv() {
        return readContent(ASSET_PROPERTIES_SELECT_BY_K_V);
    }

    public static String readAssetPropertiesSelectByAssetId() {
        return readContent(ASSET_PROPERTIES_SELECT_BY_ASSET_ID);
    }

    private static String readContent(String resourceName) {
        try (InputStream inputStream = PreparedStatementResourceReader.class.getClassLoader().getResourceAsStream(resourceName)) {
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
