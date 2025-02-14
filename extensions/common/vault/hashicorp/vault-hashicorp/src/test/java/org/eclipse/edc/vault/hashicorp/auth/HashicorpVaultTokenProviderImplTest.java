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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashicorpVaultTokenProviderImplTest {
    
    @Test
    void vaultToken_shouldReturnToken() {
        var token = "token123";
        var tokenProvider = new HashicorpVaultTokenProviderImpl(token);
        
        assertThat(tokenProvider.vaultToken()).isEqualTo(token);
    }
    
    @Test
    void constructor_tokenNull_shouldThrowException() {
        assertThatThrownBy(() -> new HashicorpVaultTokenProviderImpl(null))
                .isInstanceOf(NullPointerException.class);
    }
}
