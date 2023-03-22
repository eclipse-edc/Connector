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
 *       ZF Friedrichshafen AG - add management api configurations
 *       Fraunhofer Institute for Software and Systems Engineering - added IDS API context
 *
 */

package org.eclipse.edc.test.system.local;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.test.system.utils.FileTransferConfiguration;
import org.eclipse.edc.test.system.utils.TransferTestRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.tempDirectory;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_IDS_API_DATA;

@EndToEndTest
public class FileTransferIntegrationTest extends FileTransferEdcRuntime {

    @Test
    public void transferFile_success() throws Exception {

        var destinationPath = new File(tempDirectory(), "output.txt").getAbsolutePath();

        // Arrange
        // Create a file with test data on provider file system.
        var fileContent = "FileTransfer-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));


        var runner = new TransferTestRunner(new FileTransferConfiguration(CONSUMER_CONNECTOR_MANAGEMENT_URL, PROVIDER_IDS_API_DATA, destinationPath));

        runner.executeTransfer();

        // Assert
        var copiedFilePath = Path.of(destinationPath);
        assertThat(copiedFilePath)
                .withFailMessage("Destination file %s not created", copiedFilePath)
                .exists();
        var actualFileContent = Files.readString(copiedFilePath);
        assertThat(actualFileContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(fileContent);
    }
}
