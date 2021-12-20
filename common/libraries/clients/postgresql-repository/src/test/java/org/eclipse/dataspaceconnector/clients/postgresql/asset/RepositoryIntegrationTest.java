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

package org.eclipse.dataspaceconnector.clients.postgresql.asset;

import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient;
import org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClientImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.ExistsMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.PropertyMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.types.Property;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.PreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.TestPreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactory;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryConfig;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryImpl;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RepositoryIntegrationTest {

    @Container
    @SuppressWarnings("rawtypes")
    private static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:9.6.12");

    private Repository repository;
    private PostgresqlClient postgresqlClient;

    @BeforeEach
    final void setupPostgresAndClient() throws SQLException {

        ConnectionFactoryConfig connectionFactoryConfig = ConnectionFactoryConfig.Builder.newInstance()
                .uri(URI.create(POSTGRES_CONTAINER.getJdbcUrl()))
                .userName(POSTGRES_CONTAINER.getUsername())
                .password(POSTGRES_CONTAINER.getPassword())
                .build();

        ConnectionFactory connectionFactory = new ConnectionFactoryImpl(connectionFactoryConfig);

        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .build();

        ConnectionPool connectionPool = new CommonsConnectionPool(connectionFactory, commonsConnectionPoolConfig);

        PostgresqlClient client = new PostgresqlClientImpl(connectionPool);

        client.execute(TestPreparedStatementResourceReader.getTablesCreate());

        this.postgresqlClient = client;
        this.repository = new RepositoryImpl(client);
    }

    @AfterEach
    final void teardownPostgresAndClient() throws Exception {
        postgresqlClient.execute(TestPreparedStatementResourceReader.getTablesDelete());
        postgresqlClient.close();
    }

    @Test
    public void testAssetAndPropertyCreation() throws SQLException {

        Asset asset = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        DataAddress address = DataAddress.Builder.newInstance()
                .property("foo", "bar")
                .property("type", "foo")
                .build();

        String addressId = asset.getId();

        repository.create(asset, address);

        List<Property> assetProperties = postgresqlClient.execute(new PropertyMapper(),
                PreparedStatementResourceReader.readAssetPropertiesSelectByAssetId(), asset.getId());
        List<Property> addressProperties = postgresqlClient.execute(new PropertyMapper(),
                PreparedStatementResourceReader.readAddressPropertiesSelectByAddressId(), addressId);
        List<Boolean> assetExists = postgresqlClient.execute(new ExistsMapper(),
                PreparedStatementResourceReader.readAssetExists(), asset.getId());
        List<Boolean> addressExists = postgresqlClient.execute(new ExistsMapper(),
                PreparedStatementResourceReader.readAddressExists(), addressId);

        Assertions.assertTrue(assetExists.size() == 1 && assetExists.get(0));
        Assertions.assertTrue(addressExists.size() == 1 && assetExists.get(0));
        Assertions.assertEquals(3, assetProperties.size());
        Assertions.assertEquals(2, addressProperties.size());
        assertThat(assetProperties)
                .contains(new Property(Asset.PROPERTY_ID, asset.getId()))
                .contains(new Property(Asset.PROPERTY_CONTENT_TYPE, asset.getContentType()))
                .contains(new Property(Asset.PROPERTY_VERSION, asset.getVersion()));

        assertThat(addressProperties)
                .contains(new Property("foo", "bar"))
                .contains(new Property("type", "foo"));
    }

    @Test
    public void testSelectAllAssetsQuery() throws SQLException {

        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(asset1, dataAddress);
        repository.create(asset2, dataAddress);

        Criterion selectAll = new Criterion("*", "=", "*");
        List<Asset> assets = repository.queryAssets(Collections.singletonList(selectAll));

        org.assertj.core.api.Assertions.assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId())
                .contains(asset2.getId());
    }

    @Test
    public void testSelectAllAddressesQuery() throws SQLException {

        DataAddress dataAddress1 = DataAddress.Builder.newInstance().type("foo").build();
        DataAddress dataAddress2 = DataAddress.Builder.newInstance().type("bar").build();

        repository.create(createUniqueAsset(), dataAddress1);
        repository.create(createUniqueAsset(), dataAddress2);

        Criterion selectAll = new Criterion("*", "=", "*");
        List<DataAddress> addresses = repository.queryAddress(Collections.singletonList(selectAll));

        org.assertj.core.api.Assertions.assertThat(addresses.stream().map(DataAddress::getType).collect(Collectors.toUnmodifiableList()))
                .contains(dataAddress1.getType())
                .contains(dataAddress2.getType());
    }

    @Test
    public void testSelectAssetsById() throws SQLException {

        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(asset1, dataAddress);
        repository.create(asset2, dataAddress);

        Criterion select = new Criterion(Asset.PROPERTY_ID, "=", asset1.getId());
        List<Asset> assets = repository.queryAssets(Collections.singletonList(select));

        org.assertj.core.api.Assertions.assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId());
    }

    @Test
    public void testAddressByByAssetId() throws SQLException {
        Asset asset = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(asset, dataAddress);

        Criterion select = new Criterion("asset_id", "=", asset.getId());
        List<DataAddress> addresses = repository.queryAddress(Collections.singletonList(select));

        org.assertj.core.api.Assertions.assertThat(addresses.stream().map(DataAddress::getType).collect(Collectors.toUnmodifiableList()))
                .contains(dataAddress.getType())
                .size().isEqualTo(1);
    }

    @Test
    public void testSelectMultipleAssets() throws SQLException {

        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(asset1, dataAddress);
        repository.create(asset2, dataAddress);

        Criterion select1 = new Criterion(Asset.PROPERTY_CONTENT_TYPE, "=", "pdf");
        Criterion select2 = new Criterion(Asset.PROPERTY_VERSION, "=", "1.0.0");
        List<Asset> assets = repository.queryAssets(Arrays.asList(select1, select2));

        org.assertj.core.api.Assertions.assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId())
                .contains(asset2.getId());
    }

    @Test
    public void testSelectMultipleAddressesQuery() throws SQLException {

        DataAddress dataAddress1 = DataAddress.Builder.newInstance().type("selected")
                .keyName("foo").build();
        DataAddress dataAddress2 = DataAddress.Builder.newInstance().type("selected")
                .keyName("bar").build();
        DataAddress dataAddress3 = DataAddress.Builder.newInstance().type("skipped")
                .keyName("foobar").build();

        repository.create(createUniqueAsset(), dataAddress1);
        repository.create(createUniqueAsset(), dataAddress2);
        repository.create(createUniqueAsset(), dataAddress3);

        Criterion selectAll = new Criterion("type", "=", "selected");
        List<DataAddress> addresses = repository.queryAddress(Collections.singletonList(selectAll));

        org.assertj.core.api.Assertions.assertThat(addresses.stream().map(DataAddress::getKeyName).collect(Collectors.toUnmodifiableList()))
                .contains(dataAddress1.getKeyName())
                .contains(dataAddress2.getKeyName())
                .doesNotContain(dataAddress3.getKeyName());
    }

    @Test
    public void testAssetUpdate() throws SQLException {

        Asset base = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .property("foo", "bar")
                .build();

        Asset asset = Asset.Builder.newInstance()
                .id(base.getId())
                .contentType("pdf")
                .version("1.1.0")
                .name("updatedAsset")
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance().type("foo").build();

        repository.create(base, dataAddress);
        repository.update(asset);

        List<Property> properties = postgresqlClient.execute(new PropertyMapper(),
                PreparedStatementResourceReader.readAssetPropertiesSelectByAssetId(), asset.getId());
        List<Boolean> exists = postgresqlClient.execute(new ExistsMapper(),
                PreparedStatementResourceReader.readAssetExists(), asset.getId());

        Assertions.assertTrue(exists.size() == 1 && exists.get(0));
        Assertions.assertEquals(4, properties.size());
        assertThat(properties)
                .contains(new Property(Asset.PROPERTY_ID, asset.getId()))
                .contains(new Property(Asset.PROPERTY_CONTENT_TYPE, asset.getContentType()))
                .contains(new Property(Asset.PROPERTY_NAME, asset.getName()))
                .contains(new Property(Asset.PROPERTY_VERSION, asset.getVersion()));
    }


    @Test
    public void testAddressUpdate() throws SQLException {

        Asset asset = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .property("foo", "bar")
                .build();

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type("type")
                .property("delete", "me")
                .build();

        DataAddress dataAddress2 = DataAddress.Builder.newInstance()
                .type("update-me")
                .property("add", "me")
                .build();

        repository.create(asset, dataAddress);
        repository.update(asset, dataAddress2);

        List<Property> properties = postgresqlClient.execute(new PropertyMapper(),
                PreparedStatementResourceReader.readAddressPropertiesSelectByAddressId(), asset.getId());
        List<Boolean> exists = postgresqlClient.execute(new ExistsMapper(),
                PreparedStatementResourceReader.readAddressExists(), asset.getId());

        Assertions.assertTrue(exists.size() == 1 && exists.get(0));
        Assertions.assertEquals(2, properties.size());
        assertThat(properties)
                .contains(new Property("type", "update-me"))
                .contains(new Property("add", "me"));
    }

    @Test
    public void testAssetDelete() throws SQLException {

        Asset asset = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        DataAddress address = DataAddress.Builder.newInstance().type("foo").build();

        String addressId = asset.getId();

        repository.create(asset, address);
        repository.delete(asset);

        List<Property> assetProperties = postgresqlClient.execute(new PropertyMapper(),
                PreparedStatementResourceReader.readAssetPropertiesSelectByAssetId(), asset.getId());
        List<Boolean> assetExists = postgresqlClient.execute(new ExistsMapper(),
                PreparedStatementResourceReader.readAssetExists(), asset.getId());
        List<Property> addressProperties = postgresqlClient.execute(new PropertyMapper(),
                PreparedStatementResourceReader.readAddressPropertiesSelectByAddressId(), addressId);
        List<Boolean> addressExists = postgresqlClient.execute(new ExistsMapper(),
                PreparedStatementResourceReader.readAddressExists(), addressId);

        Assertions.assertFalse(assetExists.size() == 1 && assetExists.get(0));
        Assertions.assertFalse(addressExists.size() == 1 && assetExists.get(0));
        Assertions.assertEquals(0, assetProperties.size());
        Assertions.assertEquals(0, addressProperties.size());
    }

    private Asset createUniqueAsset() {
        return Asset.Builder.newInstance().id(UUID.randomUUID().toString()).build();
    }
}
