/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.azure.blob;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageResourceDefinitionTest {

    @Test
    void toBuilder_verifyEqualResourceDefinition() {
        var definition = ObjectStorageResourceDefinition.Builder.newInstance()
                .id("id")
                .transferProcessId("tp-id")
                .accountName("account")
                .containerName("container")
                .build();
        var builder = definition.toBuilder();
        var rebuiltDefinition = builder.build();

        assertThat(rebuiltDefinition).usingRecursiveComparison().isEqualTo(definition);
    }

}
