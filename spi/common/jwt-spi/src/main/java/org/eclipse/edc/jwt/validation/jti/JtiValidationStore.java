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
import org.eclipse.edc.spi.result.StoreResult;

@ExtensionPoint
public interface JtiValidationStore {

    StoreResult<Void> storeEntry(JtiValidationEntry entry);

    JtiValidationEntry findById(String id, boolean autoRemove);

    default JtiValidationEntry findById(String id) {
        return findById(id, true);
    }

    StoreResult<Void> deleteById(String id);

    StoreResult<Integer> deleteExpired();
}
