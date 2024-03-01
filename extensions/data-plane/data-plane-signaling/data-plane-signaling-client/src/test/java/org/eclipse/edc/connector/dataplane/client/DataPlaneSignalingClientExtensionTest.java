/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.client;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSignalingClientExtensionTest {

    @Test
    void verifyDataPlaneClientFactory(DataPlaneSignalingClientExtension extension) {

        var client = extension.dataPlaneClientFactory().createClient(createDataPlaneInstance());

        assertThat(client).isInstanceOf(DataPlaneSignalingClient.class);
    }


    private DataPlaneInstance createDataPlaneInstance() {
        return DataPlaneInstance.Builder.newInstance().url("http://any").build();
    }
}
