/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.secret.spi.observe;

import org.eclipse.edc.spi.observe.Observable;
import org.eclipse.edc.spi.types.domain.secret.Secret;

/**
 * Interface implemented by listeners registered to observe secret state changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface SecretListener {

    /**
     * Called after a {@link Secret} was created.
     *
     * @param secret the secret that has been created.
     */
    default void created(Secret secret) {

    }

    /**
     * Called after a {@link Secret} was deleted.
     *
     * @param secret the secret that has been deleted.
     */
    default void deleted(Secret secret) {

    }

    /**
     * Called after a {@link Secret} was updated
     *
     * @param secret The new (already updated) secret.
     */
    default void updated(Secret secret) {

    }

}
