/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class InputStreamDataSourceTest {

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void verifyRead() throws IOException {
        var data = "bar".getBytes();
        var source = new InputStreamDataSource("foo", new ByteArrayInputStream(data));

        assertThat(source.openPartStream().findFirst().get().openStream().readAllBytes()).isEqualTo(data);
    }
}
