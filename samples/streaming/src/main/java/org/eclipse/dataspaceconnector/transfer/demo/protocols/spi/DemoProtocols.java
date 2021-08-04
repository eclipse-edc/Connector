/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi;

/**
 * Defines protocol constants.
 */
public interface DemoProtocols {

    String OBJECT_STORAGE = "dataspaceconnector:demo:os";

    String PULL_STREAM = "dataspaceconnector:demo:pullstream";

    String PUSH_STREAM_WS = "dataspaceconnector:demo:pushstream:ws";

    String PUSH_STREAM_HTTP = "dataspaceconnector:demo:pushstream:http";

    String ENDPOINT_ADDRESS = "endpointAddress";

    String DESTINATION_NAME = "destinationName";

}
