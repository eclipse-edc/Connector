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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.contract.negotiation.response;

import org.eclipse.dataspaceconnector.spi.result.Failure;

import static java.util.Collections.emptyList;

public class StatusFailure extends Failure {
    private final NegotiationResult.Status status;

    public StatusFailure(NegotiationResult.Status status) {
        super(emptyList());
        this.status = status;
    }

    public NegotiationResult.Status getStatus() {
        return status;
    }
}
