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

package org.eclipse.dataspaceconnector.common.testfixtures;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestUtilsTest {

    private static final int EPHEMERAL_PORT_MIN = 1024;
    private static final int EPHEMERAL_PORT_MAX = 65535;


    @Test
    void getFreePort() {
        assertThat(TestUtils.getFreePort()).isGreaterThan(EPHEMERAL_PORT_MIN).isLessThan(EPHEMERAL_PORT_MAX);
    }

    @Test
    void getFreePort_lowerBound() {
        assertThat(TestUtils.getFreePort(5000)).isGreaterThanOrEqualTo(5000).isLessThan(EPHEMERAL_PORT_MAX);

        assertThatThrownBy(() -> TestUtils.getFreePort(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TestUtils.getFreePort(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TestUtils.getFreePort(EPHEMERAL_PORT_MAX)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getFreePort_withUpperAndLowerBound() {
        assertThat(TestUtils.getFreePort(5000, 7000)).isGreaterThanOrEqualTo(5000).isLessThan(7000);

        assertThatThrownBy(() -> TestUtils.getFreePort(5000, 4999)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TestUtils.getFreePort(5000, 5000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TestUtils.getFreePort(5000, EPHEMERAL_PORT_MAX + 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TestUtils.getFreePort(5000, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getFreePort_whenOccupied() throws IOException {
        var port = TestUtils.getFreePort(EPHEMERAL_PORT_MIN);

        try (var socket = new ServerSocket(port)) {
            assertThat(TestUtils.getFreePort(EPHEMERAL_PORT_MIN)).describedAs("Next free port").isGreaterThan(socket.getLocalPort());
        }
    }
}