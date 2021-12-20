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

package org.eclipse.dataspaceconnector.clients.postgresql.asset.operation;

import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.ExistsMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.PropertyMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.serializer.EnvelopePacker;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.types.Property;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.PreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AddressUpdateOperation {
    private final PostgresqlClient postgresqlClient;

    public AddressUpdateOperation(@NotNull PostgresqlClient postgresClient) {
        this.postgresqlClient = Objects.requireNonNull(postgresClient);
    }

    public void invoke(@NotNull Asset asset, @NotNull DataAddress address) throws SQLException {
        Objects.requireNonNull(asset);
        Objects.requireNonNull(address);

        String sqlAddressExists = PreparedStatementResourceReader.readAddressExists();
        String sqlAssetExists = PreparedStatementResourceReader.readAddressExists();
        String sqlProperties = PreparedStatementResourceReader.readAddressPropertiesSelectByAddressId();
        String sqlPropertyCreate = PreparedStatementResourceReader.readAddressPropertyCreate();
        String sqlPropertyDelete = PreparedStatementResourceReader.readAddressPropertyDelete();
        String sqlPropertyUpdate = PreparedStatementResourceReader.readAddressPropertyUpdate();

        List<Boolean> existsAssetResult = postgresqlClient.execute(new ExistsMapper(), sqlAssetExists, asset.getId());
        boolean existsAsset = existsAssetResult.size() == 1 && existsAssetResult.get(0);
        if (!existsAsset) {
            throw new SQLException(String.format("Asset with id %s does not exist", asset.getId()));
        }

        List<Boolean> existsAddressResult = postgresqlClient.execute(new ExistsMapper(), sqlAddressExists, asset.getId());
        boolean existsAddress = existsAddressResult.size() == 1 && existsAddressResult.get(0);
        if (!existsAddress) {
            throw new SQLException(String.format("Address with asset id %s does not exist", asset.getId()));
        }

        postgresqlClient.doInTransaction(client -> {
            List<Property> storedProperties = client.execute(new PropertyMapper(), sqlProperties, asset.getId());

            List<Property> propertyToDelete = findPropertiesToDelete(address, storedProperties);
            List<Property> propertyToInsert = findPropertiesToInsert(address, storedProperties);
            List<Property> propertyToUpdate = findPropertiesToUpdate(address, storedProperties);

            for (var property : propertyToDelete) {
                client.execute(sqlPropertyDelete, asset.getId(), property.getKey());
            }
            for (var property : propertyToInsert) {
                String value = EnvelopePacker.pack(property.getValue());
                client.execute(sqlPropertyCreate, asset.getId(), property.getKey(), value);
            }
            for (var property : propertyToUpdate) {
                String value = EnvelopePacker.pack(property.getValue());
                client.execute(sqlPropertyUpdate, value, asset.getId(), property.getKey());
            }
        });
    }

    private List<Property> findPropertiesToUpdate(DataAddress address, List<Property> storedProperties) {
        List<Property> propertiesToUpdate = new ArrayList<>();
        Map<String, String> assetProperties = address.getProperties();

        for (Property storedProperty : storedProperties) {
            if (!assetProperties.containsKey(storedProperty.getKey())) {
                continue;
            }

            String value = assetProperties.get(storedProperty.getKey());

            if (!value.equals(storedProperty.getValue())) {
                propertiesToUpdate.add(new Property(storedProperty.getKey(), value));
            }
        }

        return propertiesToUpdate;
    }

    private List<Property> findPropertiesToDelete(DataAddress address, List<Property> storedProperties) {
        List<Property> propertiesToDelete = new ArrayList<>();
        Map<String, String> assetProperties = address.getProperties();

        for (Property storedProperty : storedProperties) {
            if (!assetProperties.containsKey(storedProperty.getKey())) {
                propertiesToDelete.add(storedProperty);
            }
        }

        return propertiesToDelete;
    }

    private List<Property> findPropertiesToInsert(DataAddress address, List<Property> storedProperties) {
        List<Property> propertiesToInsert = new ArrayList<>();
        Map<String, String> assetProperties = address.getProperties();

        for (var assetProperty : assetProperties.entrySet()) {
            if (storedProperties.stream()
                    .noneMatch(p -> p.getKey().equals(assetProperty.getKey()))) {
                propertiesToInsert.add(new Property(assetProperty.getKey(), assetProperty.getValue()));
            }
        }

        return propertiesToInsert;
    }
}
