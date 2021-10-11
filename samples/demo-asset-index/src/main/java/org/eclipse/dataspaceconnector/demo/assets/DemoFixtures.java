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

package org.eclipse.dataspaceconnector.demo.assets;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.List;

/**
 * It is beyond me why we need a setup this unnecessarily complicated.
 * PLEASE change this to just generate Assets.
 */
final class DemoFixtures {

    public static List<Asset> getAssets() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
