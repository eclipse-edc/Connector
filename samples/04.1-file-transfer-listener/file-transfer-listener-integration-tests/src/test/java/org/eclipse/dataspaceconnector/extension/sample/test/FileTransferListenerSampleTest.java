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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.extension.sample.test.FileTransferSampleTestCommon.getFileFromRelativePath;

@EndToEndTest
public class FileTransferListenerSampleTest {

    static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.1-file-transfer-listener/consumer/config.properties";
    static final String PROVIDER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.0-file-transfer/provider/config.properties";
    // Reuse an already existing file for the test. Could be set to any other existing file in the repository.
    static final String SAMPLE_ASSET_FILE_PATH = "samples/04.1-file-transfer-listener/README.md";
    static final String DESTINATION_FILE_PATH = "samples/04.1-file-transfer-listener/requested.test.txt";
    // marker.txt is a fixed name and always in the same directory as the file defined with DESTINATION_FILE_PATH.
    static final String MARKER_FILE_PATH = "samples/04.1-file-transfer-listener/marker.txt";
    static final File MARKER_FILE = getFileFromRelativePath(MARKER_FILE_PATH);
    static final String MARKER_FILE_CONTENT = "Transfer complete";
    @RegisterExtension
    static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:provider",
            "provider",
            Map.of(
                    // Override 'edc.samples.04.asset.path' implicitly set via property 'edc.fs.config'.
                    "edc.samples.04.asset.path", getFileFromRelativePath(SAMPLE_ASSET_FILE_PATH).getAbsolutePath(),
                    "edc.fs.config", getFileFromRelativePath(PROVIDER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.1-file-transfer-listener:consumer",
            "consumer",
            Map.of(
                    "edc.fs.config", getFileFromRelativePath(CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    private final FileTransferSampleTestCommon common = new FileTransferSampleTestCommon(SAMPLE_ASSET_FILE_PATH, DESTINATION_FILE_PATH);

    /**
     * Run all sample steps in one single test.
     * Note: Sample steps cannot be separated into single tests because {@link EdcRuntimeExtension}
     * runs before each single test.
     */
    @Test
    void runSampleSteps() throws Exception {
        assertTestPrerequisites();

        common.initiateContractNegotiation();
        common.lookUpContractAgreementId();
        common.requestTransferFile();
        common.assertDestinationFileContent();
        assertFileContent(MARKER_FILE, MARKER_FILE_CONTENT);
    }

    @AfterEach
    protected void tearDown() {
        cleanTemporaryTestFiles();
    }

    /**
     * Assert that prerequisites are fulfilled before running the test.
     * This assertion checks only whether the file to be copied is not existing already.
     */
    void assertTestPrerequisites() {
        common.assertTestPrerequisites();

        assertThat(MARKER_FILE).doesNotExist();
    }

    /**
     * Remove files created while running the tests.
     * The copied file and the marker file will be deleted.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void cleanTemporaryTestFiles() {
        common.cleanTemporaryTestFiles();

        MARKER_FILE.delete();
    }

    /**
     * Assert that the marker file has been created at the expected location with the expected content.
     * This method waits a duration which is defined in {@link FileTransferSampleTestCommon#timeout}.
     */
    void assertFileContent(File markerFile, @NotNull String markerFileContent) {
        await().atMost(common.timeout).pollInterval(common.pollInterval).untilAsserted(()
                -> assertThat(markerFile).hasContent(markerFileContent));
    }
}
