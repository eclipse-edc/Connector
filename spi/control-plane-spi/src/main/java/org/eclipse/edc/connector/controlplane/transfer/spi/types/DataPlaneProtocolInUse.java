/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

/**
 * This service is needed to avoid breaking changes in the TransferProcess behavior during the introduction of the new
 * Data Plane Signaling protocol.
 *
 * @deprecated will be removed together with the Legacy Data Plane signaling protocol.
 */
@Deprecated(since = "0.16.0")
public class DataPlaneProtocolInUse {
    private boolean isLegacy = false;

    public void setLegacy(boolean legacy) {
        isLegacy = legacy;
    }

    public boolean isLegacy() {
        return isLegacy;
    }
}
