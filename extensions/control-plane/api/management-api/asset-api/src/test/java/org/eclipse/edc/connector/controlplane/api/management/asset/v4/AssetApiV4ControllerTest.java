/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.asset.v4;

import org.eclipse.edc.connector.controlplane.api.management.asset.BaseAssetApiControllerTest;
import org.eclipse.edc.junit.annotations.ApiTest;

@ApiTest
class AssetApiV4ControllerTest extends BaseAssetApiControllerTest {


    @Override
    protected Object controller() {
        return new AssetApiV4Controller(service, transformerRegistry, monitor, validator, participantContextSupplier);
    }

    @Override
    protected String versionPath() {
        return "v4alpha";
    }

}
