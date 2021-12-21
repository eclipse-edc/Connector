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

import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@IntegrationTest
public class AddressQueryOperationTest extends AbstractOperationTest {

    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = getRepository();
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

}
