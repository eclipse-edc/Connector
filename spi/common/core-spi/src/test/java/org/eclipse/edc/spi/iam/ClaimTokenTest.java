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

package org.eclipse.edc.spi.iam;

import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimTokenTest {

    @Test
    void serdes() {
        var typeManager = new TypeManager();
        var claimToken = ClaimToken.Builder.newInstance().claim("claimKey", "claimValue").build();

        var json = typeManager.writeValueAsString(claimToken);
        var deserialized = typeManager.readValue(json, ClaimToken.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(claimToken);
    }
}
