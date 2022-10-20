/*
 *  Copyright (c) 2020, 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.client.spi.transferprocess;

import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

/**
 * Noop Transfer process client
 */
public class NoopTransferProcessClient implements TransferProcessApiClient {
    @Override
    public void completed(DataFlowRequest request) {

    }

    @Override
    public void failed(DataFlowRequest request, String reason) {

    }
}
