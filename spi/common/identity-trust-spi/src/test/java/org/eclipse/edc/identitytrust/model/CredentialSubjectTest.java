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

package org.eclipse.edc.identitytrust.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialSubjectTest {

    @Test
    void build_noClaims() {
        assertThatThrownBy(() -> CredentialSubject.Builder.newInstance()
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CredentialSubject.Builder.newInstance()
                .claims(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}