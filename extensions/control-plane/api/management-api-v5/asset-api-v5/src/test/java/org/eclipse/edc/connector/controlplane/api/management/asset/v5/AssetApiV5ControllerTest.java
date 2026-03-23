/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.api.management.asset.v5;

import org.eclipse.edc.connector.controlplane.api.management.asset.AssetApiControllerTest;

public class AssetApiV5ControllerTest extends AssetApiControllerTest {
    @Override
    protected String versionPath() {
        return "v5alpha";
    }

    @Override
    protected Object controller() {
        return new AssetApiV5Controller(assetService, transformerRegistry, validator, monitor, authorizationService);
    }
}
