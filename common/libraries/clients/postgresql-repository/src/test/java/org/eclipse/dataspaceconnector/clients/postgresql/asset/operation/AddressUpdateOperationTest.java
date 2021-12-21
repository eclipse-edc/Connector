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
import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.ExistsMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper.PropertyMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.types.Property;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.util.PreparedStatementResourceReader;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class AddressUpdateOperationTest extends AbstractOperationTest {

    private Repository repository;
    private PostgresqlClient postgresqlClient;

    @BeforeEach
    public void setup() {
        repository = getRepository();
        postgresqlClient = getClient();
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

}
