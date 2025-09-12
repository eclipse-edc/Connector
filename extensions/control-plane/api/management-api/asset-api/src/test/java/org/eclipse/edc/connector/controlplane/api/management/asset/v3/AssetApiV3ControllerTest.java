/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.asset.v3;

import org.eclipse.edc.connector.controlplane.api.management.asset.BaseAssetApiControllerTest;
import org.eclipse.edc.junit.annotations.ApiTest;

@ApiTest
class AssetApiV3ControllerTest extends BaseAssetApiControllerTest {


    @Override
    protected Object controller() {
        return new AssetApiV3Controller(service, transformerRegistry, monitor, validator, participantContextSupplier);
    }
    
    @Override
    protected String versionPath() {
        return "v3";
    }
}
