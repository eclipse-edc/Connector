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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer.spi.security;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;

@FunctionalInterface
@ExtensionPoint
public interface KeyPairWrapper {
    @NotNull
    KeyPair get();
}
