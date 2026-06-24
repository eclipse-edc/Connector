/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public record TerminateNegotiation(String reason) {

    public static final String TERMINATE_NEGOTIATION_TYPE_TERM = "TerminateNegotiation";
    public static final String TERMINATE_NEGOTIATION_TYPE = EDC_NAMESPACE + TERMINATE_NEGOTIATION_TYPE_TERM;
    public static final String TERMINATE_NEGOTIATION_REASON = EDC_NAMESPACE + "reason";

}
