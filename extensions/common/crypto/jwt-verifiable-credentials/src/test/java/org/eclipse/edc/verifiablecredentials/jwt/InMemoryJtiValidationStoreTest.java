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

import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStoreTestBase;

class InMemoryJtiValidationStoreTest extends JtiValidationStoreTestBase {

    private final InMemoryJtiValidationStore store = new InMemoryJtiValidationStore();

    @Override
    protected JtiValidationStore getStore() {
        return store;
    }
}