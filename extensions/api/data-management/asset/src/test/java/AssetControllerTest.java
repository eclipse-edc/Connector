/*
 * Copyright (c) 2022 Diego Gomez
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   Diego Gomez - Initial API and Implementation
 */

import static org.mockito.Mockito.mock;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.AssetController;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void createAssetAlreadyExists() {
        //Todo: implement
    }

    @Test
    void listAllAssets() {
        //Todo: implement
    }

    @Test
    void getAssetFromID() {
        //Todo: implement
    }

    @Test
    void getAssetFromIDNotExists() {
        //Todo: implement
    }

    @Test
    void removeAssetfromID() {
        //Todo: implement
    }

    @Test
    void removeAssetFromIDNotExists() {
        //Todo: implement
    }

}
