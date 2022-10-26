/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.provision.oauth2;

import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Oauth2SecretTokenTest {
    @Test
    void serdes() {
        var typeManager = new TypeManager();
        var token = new Oauth2SecretToken("token");

        var json = typeManager.writeValueAsString(token);
        var deserialized = typeManager.readValue(json, Oauth2SecretToken.class);

        assertThat(deserialized.getToken()).isEqualTo("token");
    }
}
