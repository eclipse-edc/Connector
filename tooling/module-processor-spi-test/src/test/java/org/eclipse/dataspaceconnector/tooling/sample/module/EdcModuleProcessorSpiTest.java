/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.tooling.sample.module;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.tooling.module.domain.EdcModule;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the sample extension module is introspected correctly.
 */
class EdcModuleProcessorSpiTest {

    private static final TypeReference<List<EdcModule>> TYPE_REFERENCE = new TypeReference<>() {
    };

    @Test
    void verify() throws IOException {
        var userdir = System.getProperty("user.dir"); //will point to the module's directory
        var file = userdir + File.separator + "build" + File.separator + "edc.json";
        var url = Paths.get(file).toUri().toURL();
        assertThat(url).isNotNull();

        try (var stream = url.openStream()) {
            var manifests = new ObjectMapper().readValue(stream, TYPE_REFERENCE);
            assertThat(manifests.size()).isEqualTo(1);

            var manifest = manifests.get(0);
            assertThat(manifest.getName()).isEqualTo(TestConstants.NAME);
            assertThat(manifest.getCategories()).contains("category");
            assertThat(manifest.getOverview()).isNotEmpty();

            var extensionPoints = manifest.getExtensionPoints();
            assertThat(extensionPoints.size()).isEqualTo(1);
            assertThat(extensionPoints.get(0).getService()).isEqualTo(ExtensionService.class.getName());

        }
    }
}
