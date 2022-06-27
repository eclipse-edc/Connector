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
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.service;

import org.eclipse.dataspaceconnector.spi.observe.Observable;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Interface implemented by listeners registered to observe asset state changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface AssetListener {

    /**
     * Called after a {@link Asset} was created.
     *
     * @param asset the asset that has been created.
     */
    default void created(Asset asset) {

    }

    /**
     * Called after a {@link Asset} was deleted.
     *
     * @param asset the asset that has been deleted.
     */
    default void deleted(Asset asset) {

    }

}
