/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.jwt.validation.jti;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;

@ExtensionPoint
public interface JtiValidationStore {

    /**
     * Stores an entry, unless an entry with the same token ID exists that is not expired. An expired entry is replaced.
     * This is atomic, so that of several concurrent calls with the same token ID, only one succeeds.
     *
     * @param entry the entry to store
     * @return success if the entry was stored, a failure with reason {@link StoreFailure.Reason#ALREADY_EXISTS} if an entry
     *         with the same token ID exists that is not expired
     */
    StoreResult<Void> storeEntry(JtiValidationEntry entry);

    JtiValidationEntry findById(String id, boolean autoRemove);

    default JtiValidationEntry findById(String id) {
        return findById(id, true);
    }

    StoreResult<Void> deleteById(String id);

    StoreResult<Integer> deleteExpired();
}
