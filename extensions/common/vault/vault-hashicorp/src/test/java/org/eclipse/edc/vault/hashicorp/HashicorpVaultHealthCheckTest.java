/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.model.HealthResponse;
import org.eclipse.edc.vault.hashicorp.model.HealthResponsePayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class HashicorpVaultHealthCheckTest {

    private HashicorpVaultHealthCheck healthCheck;

    // mocks
    private Monitor monitor;
    private HashicorpVaultClient client;

    @BeforeEach
    void setup() {
        monitor = Mockito.mock(Monitor.class);
        client = Mockito.mock(HashicorpVaultClient.class);

        healthCheck = new HashicorpVaultHealthCheck(client, monitor);
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 409, 472, 473, 501, 503, 999 })
    void testResponseFromCode(int code) {

        Mockito.when(client.getHealth())
                .thenReturn(HealthResponse.Builder.newInstance().payload(new HealthResponsePayload()).code(code).build());

        var result = healthCheck.get();

        if (code == 200) {
            Mockito.verify(monitor, Mockito.times(1)).debug(Mockito.anyString());
            Assertions.assertTrue(result.succeeded());
        } else {
            Assertions.assertTrue(result.failed());
            Mockito.verify(monitor, Mockito.times(1)).warning(Mockito.anyString());
        }
    }

    @Test
    void testResponseFromException() {
        Mockito.when(client.getHealth()).thenThrow(new EdcException("foo-bar"));

        var result = healthCheck.get();
        Assertions.assertFalse(result.succeeded());
    }
}
