/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.auth;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Provides a token for authentication against the HashiCorp vault.
 */
@FunctionalInterface
@ExtensionPoint
public interface HashicorpVaultTokenProvider {
    
    /**
     * Obtains and returns the authentication token for the HashiCorp vault.
     *
     * @return the authentication token
     */
    String vaultToken();
    
}
