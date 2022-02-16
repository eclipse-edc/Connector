/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class AssetControllerTest {

    private AssetController assetController;

    @BeforeEach
    void setUp() {
        var monitor = mock(Monitor.class);
        assetController = new AssetController(monitor);
    }

    @Test
    void createAsset() {
        //Todo: implement
    }

    @Test
    void createAsset_alreadyExists() {
        //Todo: implement
    }

    @Test
    void getAllAssets() {
        //Todo: implement
    }

    @Test
    void getAssetById() {
        //Todo: implement
    }

    @Test
    void getAssetById_notExists() {
        //Todo: implement
    }

    @Test
    void deleteAsset() {
        //Todo: implement
    }

    @Test
    void deleteAsset_notExists() {
        //Todo: implement
    }

}
