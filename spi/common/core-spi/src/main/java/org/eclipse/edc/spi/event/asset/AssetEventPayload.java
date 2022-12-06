/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - expending Event classes
 *
 */

package org.eclipse.edc.spi.event.asset;

import org.eclipse.edc.spi.event.EventPayload;

/**
 *  Class as organizational between level to catch events of type Asset to catch them together in an Event Subscriber
 *  Contains data related to asset events
 *
 */
public class AssetEventPayload extends EventPayload {
    protected String assetId;

    public String getAssetId() {
        return assetId;
    }
}
