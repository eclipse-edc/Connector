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

import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.edc.junit.testfixtures.TestUtils.tempDirectory;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_IDS_API;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_IDS_API_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.IDS_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_ASSET_FILE;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_IDS_API;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_IDS_API_PORT;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_MANAGEMENT_PORT;

/**
 * Class providing a consumer and provider EdcRuntimeExtension used to test a file transfer.
 */
public abstract class FileTransferEdcRuntime {

    public static final String PROVIDER_ASSET_PATH = format("%s/%s.txt", tempDirectory(), PROVIDER_ASSET_FILE);

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:file-transfer-provider",
            "provider",
            Map.of(
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "edc.test.asset.path", PROVIDER_ASSET_PATH,
                    "web.http.path", PROVIDER_CONNECTOR_PATH,
                    "web.http.management.port", String.valueOf(PROVIDER_MANAGEMENT_PORT),
                    "web.http.management.path", PROVIDER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "edc.ids.id", "urn:connector:provider",
                    "ids.webhook.address", PROVIDER_IDS_API));
    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:file-transfer-consumer",
            "consumer",
            Map.of(
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "web.http.path", CONSUMER_CONNECTOR_PATH,
                    "web.http.management.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
                    "web.http.management.path", CONSUMER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "edc.ids.id", "urn:connector:consumer",
                    "ids.webhook.address", CONSUMER_IDS_API));


}
