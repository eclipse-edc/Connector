/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.provision.http.webhook;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleSecretTokenTest {

    @Test
    void getExpiration() {
        assertThat(new SimpleSecretToken("sometoken").getExpiration()).isEqualTo(0);
    }

    @Test
    void flatten() {
        var token = "test-token";
        var m = Map.of("token", token);

        //cannot use containsEntry due to the open value type on flatten()
        assertThat(new SimpleSecretToken(token).flatten()).usingRecursiveComparison().isEqualTo(m);

    }
}