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

package org.eclipse.edc.verifiablecredentials.jwt;

import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryJtiValidationStore implements JtiValidationStore {
    private final Map<String, JtiValidationEntry> jtiValidationEntries = new ConcurrentHashMap<>();

    @Override
    public StoreResult<Void> storeEntry(JtiValidationEntry entry) {
        jtiValidationEntries.put(entry.tokenId(), entry);
        return StoreResult.success();
    }

    @Override
    public JtiValidationEntry findById(String id, boolean autoRemove) {
        return autoRemove ? jtiValidationEntries.remove(id) : jtiValidationEntries.get(id);
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        return jtiValidationEntries.remove(id) == null ?
                StoreResult.notFound("JTI Validation Entry with ID '%s' not found".formatted(id)) : StoreResult.success();
    }
}
