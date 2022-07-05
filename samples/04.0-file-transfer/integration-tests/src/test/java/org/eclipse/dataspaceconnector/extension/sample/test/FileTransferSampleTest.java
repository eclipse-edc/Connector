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
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

package org.eclipse.dataspaceconnector.extension.sample.test;


import org.eclipse.dataspaceconnector.common.util.junit.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.util.Map;

@EndToEndTest
public class FileTransferSampleTest {

    static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.0-file-transfer/consumer/config.properties";
    static final String PROVIDER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.0-file-transfer/provider/config.properties";
    static final String TRANSFER_FILE_PATH = "samples/04.0-file-transfer/filetransfer.json";
    // Reuse an already existing file for the test. Could be set to any other existing file in the repository.
    static final String SAMPLE_ASSET_FILE_PATH = TRANSFER_FILE_PATH;
    static final String DESTINATION_FILE_PATH = "samples/04.0-file-transfer/consumer/requested.test.txt";
    @RegisterExtension
    static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:provider",
            "provider",
            Map.of(
                    // Override 'edc.samples.04.asset.path' implicitly set via property 'edc.fs.config'.
                    "edc.samples.04.asset.path", FileTransferSampleTestUtils.getFileFromRelativePath(SAMPLE_ASSET_FILE_PATH).getAbsolutePath(),
                    "edc.fs.config", FileTransferSampleTestUtils.getFileFromRelativePath(PROVIDER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:consumer",
            "consumer",
            Map.of(
                    "edc.fs.config", FileTransferSampleTestUtils.getFileFromRelativePath(CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    static final File DESTINATION_FILE = FileTransferSampleTestUtils.getFileFromRelativePath(FileTransferSampleTest.DESTINATION_FILE_PATH);
    static final File SAMPLE_ASSET_FILE = FileTransferSampleTestUtils.getFileFromRelativePath(FileTransferSampleTest.SAMPLE_ASSET_FILE_PATH);

    /**
     * Run all sample steps in one single test.
     * Note: Sample steps cannot be separated into single tests because {@link EdcRuntimeExtension}
     * runs before each single test.
     */
    @Test
    void runSampleSteps() throws Exception {
        var testUtils = new FileTransferSampleTestUtils();

        testUtils.assertTestPrerequisites();

        testUtils.initiateContractNegotiation();
        testUtils.lookUpContractAgreementId();
        testUtils.requestTransferFile();
        testUtils.assertDestinationFileContent();

        testUtils.cleanTemporaryTestFiles();
    }
}
