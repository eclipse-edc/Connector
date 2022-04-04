/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

/**
 * Provide utils for testing data plane transfer client.
 */
public class TestFixtures {

    public static DataFlowRequest createDataFlowRequest() {
        return DataFlowRequest.Builder.newInstance()
                .trackable(true)
                .id("123")
                .processId("456")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
    }
}
