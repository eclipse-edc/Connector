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

import static org.assertj.core.api.Assertions.assertThat;

class TestUtilsTest {

    private static final int EPHEMERAL_PORT_MIN = 1024;
    private static final int EPHEMERAL_PORT_MAX = 65535;

    @Test
    void findsRandomPort() {
        assertThat(TestUtils.findUnallocatedServerPort()).isBetween(EPHEMERAL_PORT_MIN, EPHEMERAL_PORT_MAX);
    }
}