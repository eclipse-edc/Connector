/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DataRequestTest {

    private static final Faker FAKER = new Faker();
    private static final String DATA_ADDRESS_TYPE_NAME = FAKER.lorem().word();
    private static final String DATA_ADDRESS_KEY_NAME = FAKER.lorem().word();
    private static final String DATA_ADDRESS_PROPERTY_KEY = FAKER.lorem().word();
    private static final String DATA_ADDRESS_PROPERTY_VALUE = FAKER.lorem().word();
    private static final String TRANSFER_TYPE_CONTENT_TYPE = FAKER.lorem().word();
    private static final String DATA_REQUEST_ID = FAKER.lorem().word();
    private static final String DATA_REQUEST_PROCESS_ID = FAKER.lorem().word();
    private static final String DATA_REQUEST_CONNECTOR_ADDRESS = FAKER.lorem().word();
    private static final String DATA_REQUEST_PROTOCOL = FAKER.lorem().word();
    private static final String DATA_REQUEST_CONNECTOR_ID = FAKER.lorem().word();
    private static final String DATA_REQUEST_CONTRACT_ID = FAKER.lorem().word();
    private static final String DATA_REQUEST_ASSET_ID = FAKER.lorem().word();
    private static final String DATA_REQUEST_DESTINATION_TYPE = FAKER.lorem().word();
    private static final String DATA_REQUEST_PROPERTIES_KEY = FAKER.lorem().word();
    private static final String DATA_REQUEST_PROPERTIES_VALUE_ORIGINAL = FAKER.lorem().word();
    private static final String DATA_REQUEST_PROPERTIES_VALUE_CHANGED = FAKER.lorem().word();

    @Test
    void verifyNoDestination() {
        var id = UUID.randomUUID().toString();
        var asset = Asset.Builder.newInstance().build();

        assertThrows(IllegalArgumentException.class, () -> DataRequest.Builder.newInstance().id(id).assetId(asset.getId()).build());
    }

    @Test
    void verifyCopy() {
        DataRequest dataRequest = newSampleDataRequest();

        var copy = dataRequest.copy();

        assertDataRequestsAreEqual(dataRequest, copy);
    }

    @Test
    void verifyDeepCopy() {
        DataRequest dataRequest = newSampleDataRequest();

        var copy = dataRequest.copy();

        var copyProperties = copy.getProperties();
        copyProperties.put(DATA_REQUEST_PROPERTIES_KEY, DATA_REQUEST_PROPERTIES_VALUE_CHANGED);

        assertThat(dataRequest.getProperties().get(DATA_REQUEST_PROPERTIES_KEY)).isEqualTo(DATA_REQUEST_PROPERTIES_VALUE_ORIGINAL);
    }

    @Test
    void verifyToBuilder() {
        DataRequest dataRequest = newSampleDataRequest();

        var copy = dataRequest.toBuilder().build();

        assertDataRequestsAreEqual(dataRequest, copy);
    }

    private DataRequest newSampleDataRequest() {
        var properties = new HashMap<String, String>(1);
        properties.put(DATA_REQUEST_PROPERTIES_KEY, DATA_REQUEST_PROPERTIES_VALUE_ORIGINAL);

        var dataAddress = DataAddress.Builder
                .newInstance()
                .type(DATA_ADDRESS_TYPE_NAME)
                .keyName(DATA_ADDRESS_KEY_NAME)
                .property(DATA_ADDRESS_PROPERTY_KEY, DATA_ADDRESS_PROPERTY_VALUE)
                .build();

        var transferType = TransferType.Builder.transferType()
                .isFinite(false)
                .contentType(TRANSFER_TYPE_CONTENT_TYPE)
                .build();

        return DataRequest.Builder
                .newInstance()
                .id(DATA_REQUEST_ID)
                .processId(DATA_REQUEST_PROCESS_ID)
                .connectorAddress(DATA_REQUEST_CONNECTOR_ADDRESS)
                .protocol(DATA_REQUEST_PROTOCOL)
                .connectorId(DATA_REQUEST_CONNECTOR_ID)
                .contractId(DATA_REQUEST_CONTRACT_ID)
                .assetId(DATA_REQUEST_ASSET_ID)
                .destinationType(DATA_REQUEST_DESTINATION_TYPE)
                .dataDestination(dataAddress)
                .managedResources(false)    // Set sample value to false because default is true.
                .properties(properties)
                .transferType(transferType)
                .build();
    }

    private void assertDataRequestsAreEqual(DataRequest dataRequest, DataRequest copy) {
        assertThat(copy).usingRecursiveComparison().isEqualTo(dataRequest);
        assertThat(copy.getDataDestination().getProperty(DATA_ADDRESS_PROPERTY_KEY))
                .isEqualTo(dataRequest.getDataDestination().getProperty(DATA_ADDRESS_PROPERTY_KEY));
        assertThat(copy.getProperties().get(DATA_REQUEST_PROPERTIES_KEY))
                .isEqualTo(dataRequest.getProperties().get(DATA_REQUEST_PROPERTIES_KEY));
    }
}
