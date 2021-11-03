/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;


class DataRequestTest {

    @Test
    void verifyNoDestination() {
        var id = UUID.randomUUID().toString();
        var asset = Asset.Builder.newInstance().build();

        assertThrows(IllegalArgumentException.class, () -> DataRequest.Builder.newInstance().id(id).asset(asset).build());
    }

}
