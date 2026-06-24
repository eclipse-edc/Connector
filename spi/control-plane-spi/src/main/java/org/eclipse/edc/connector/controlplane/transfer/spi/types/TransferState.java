/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Wrapper for {@link TransferProcessStates} formatted as String. Used to format a simple string as JSON.
 */
public record TransferState(String state) {

    public static final String TRANSFER_STATE_TYPE = EDC_NAMESPACE + "TransferState";
    public static final String TRANSFER_STATE_STATE = EDC_NAMESPACE + "state";

}
