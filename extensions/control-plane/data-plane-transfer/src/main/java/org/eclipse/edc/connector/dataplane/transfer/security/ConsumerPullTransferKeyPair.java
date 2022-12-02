/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer.security;

import org.eclipse.edc.connector.dataplane.transfer.spi.security.KeyPairWrapper;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;

public class ConsumerPullTransferKeyPair implements KeyPairWrapper {

    private final KeyPair keyPair;

    public ConsumerPullTransferKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @NotNull
    @Override
    public KeyPair get() {
        return keyPair;
    }
}
