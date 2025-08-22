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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.model;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public record TerminateTransfer(String reason) {

    public static final String TERMINATE_TRANSFER_TYPE_TERM = "TerminateTransfer";
    public static final String TERMINATE_TRANSFER_TYPE = EDC_NAMESPACE + TERMINATE_TRANSFER_TYPE_TERM;
    public static final String TERMINATE_TRANSFER_REASON = EDC_NAMESPACE + "reason";

}
