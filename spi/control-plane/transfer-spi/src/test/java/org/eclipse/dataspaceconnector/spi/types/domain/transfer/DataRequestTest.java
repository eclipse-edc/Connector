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

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;


class DataRequestTest {

    @Test
    void verifyNoDestination() {
        var id = UUID.randomUUID().toString();
        var asset = Asset.Builder.newInstance().build();

        assertThrows(IllegalArgumentException.class, () -> DataRequest.Builder.newInstance().id(id).assetId(asset.getId()).build());
    }

}
