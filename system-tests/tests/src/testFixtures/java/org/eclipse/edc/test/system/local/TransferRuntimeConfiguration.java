/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.system.local;

import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

/**
 * Configuration for Transfer Local Runtimes
 */
public class TransferRuntimeConfiguration {

    public static final String IDS_PATH = "/api/v1/ids";

    public static final String CONSUMER_PARTICIPANT_ID = "urn:connector:consumer";
    public static final String PROVIDER_PARTICIPANT_ID = "urn:connector:provider";

    public static final int CONSUMER_CONNECTOR_PORT = getFreePort();
    public static final int CONSUMER_MANAGEMENT_PORT = getFreePort();
    public static final String CONSUMER_CONNECTOR_PATH = "/api";
    public static final String CONSUMER_MANAGEMENT_PATH = "/api/v1/management";
    public static final String CONSUMER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + CONSUMER_MANAGEMENT_PORT + CONSUMER_MANAGEMENT_PATH;
    public static final int CONSUMER_IDS_API_PORT = getFreePort();
    public static final String CONSUMER_IDS_API = "http://localhost:" + CONSUMER_IDS_API_PORT;

    public static final int PROVIDER_CONNECTOR_PORT = getFreePort();
    public static final int PROVIDER_MANAGEMENT_PORT = getFreePort();
    public static final String PROVIDER_CONNECTOR_PATH = "/api";
    public static final String PROVIDER_MANAGEMENT_PATH = "/api/v1/management";
    public static final int PROVIDER_IDS_API_PORT = getFreePort();
    public static final String PROVIDER_IDS_API = "http://localhost:" + PROVIDER_IDS_API_PORT;

    public static final String PROVIDER_IDS_API_DATA = "http://localhost:" + PROVIDER_IDS_API_PORT + IDS_PATH + "/data";

    public static final String PROVIDER_ASSET_ID = "test-document";
    public static final long CONTRACT_VALIDITY = TimeUnit.HOURS.toSeconds(1);

    public static final String PROVIDER_ASSET_FILE = "text-document.txt";

    public static final String PROVIDER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + PROVIDER_MANAGEMENT_PORT + PROVIDER_MANAGEMENT_PATH;


}
