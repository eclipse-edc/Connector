/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.security;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;

/**
 * Handles generation of a {@link KeyPair} from the public, private key alias
 */
@ExtensionPoint
public interface KeyPairFactory {

    /**
     * Creates the {@link KeyPair}
     *
     * @param publicKeyAlias  public key alias.
     * @param privateKeyAlias private key alias.
     * @return {@link Result} of the fetching and parsing of the {@link KeyPair} from the aliases.
     */
    Result<KeyPair> fromConfig(@NotNull String publicKeyAlias, @NotNull String privateKeyAlias);

    /**
     * Create a default keypair. (suitable for testing)
     *
     * @return {@link KeyPair}
     */
    KeyPair defaultKeyPair();

}
