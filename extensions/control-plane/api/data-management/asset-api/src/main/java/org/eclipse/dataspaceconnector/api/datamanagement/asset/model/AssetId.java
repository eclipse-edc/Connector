/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Wrapper for a {@link Asset#getId()}. Used to format a simple string as JSON.
 */
public class AssetId {
    private final String id;

    public AssetId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}