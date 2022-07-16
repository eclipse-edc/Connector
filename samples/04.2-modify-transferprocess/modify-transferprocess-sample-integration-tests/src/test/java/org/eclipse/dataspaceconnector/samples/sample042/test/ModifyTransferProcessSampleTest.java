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

package org.eclipse.dataspaceconnector.samples.sample042.test;

import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.extension.sample.test.FileTransferSampleTestCommon;
import org.eclipse.dataspaceconnector.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.extension.sample.test.FileTransferSampleTestCommon.getFileFromRelativePath;

@EndToEndTest
public class ModifyTransferProcessSampleTest {

    static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.2-modify-transferprocess/consumer/config.properties";
    static final String EXPECTED_ID_PROPERTY_VALUE = "tp-sample-04.2";
    static final String EXPECTED_STATE_PROPERTY_VALUE = "ERROR";
    static final String EXPECTED_ERROR_DETAIL_PROPERTY_VALUE = "timeout by watchdog";
    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.2-modify-transferprocess:consumer",
            "consumer",
            Map.of(
                    "edc.fs.config", getFileFromRelativePath(CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );
    static final Duration DURATION = Duration.ofSeconds(15);
    static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    final FileTransferSampleTestCommon testCommon = new FileTransferSampleTestCommon("", ""); // no sample asset transfer in this sample

    /**
     * Requests transfer processes from data management API and check for expected changes on the transfer process.
     */
    @Test
    void runSample() {
        await().atMost(DURATION).pollInterval(POLL_INTERVAL)
                .untilAsserted(() ->
                        assertThat(testCommon.getTransferProcess())
                                .usingRecursiveComparison()
                                .ignoringExpectedNullFields()
                                .isEqualTo(TransferProcessDto.Builder.newInstance()
                                        .id(EXPECTED_ID_PROPERTY_VALUE)
                                        .state(EXPECTED_STATE_PROPERTY_VALUE)
                                        .errorDetail(EXPECTED_ERROR_DETAIL_PROPERTY_VALUE)
                                        .build()));
    }
}
