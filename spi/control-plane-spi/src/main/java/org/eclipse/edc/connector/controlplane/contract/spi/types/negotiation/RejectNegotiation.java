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

/**
 * Request for rejecting a provider contract negotiation held for manual approval.
 * <p>
 * It carries no rejection reason on purpose: the reason sent to the counter-party is fixed, so that no provider-internal
 * information is disclosed.
 */
public record RejectNegotiation() {

    public static final String REJECT_NEGOTIATION_TYPE_TERM = "RejectNegotiation";
    public static final String REJECT_NEGOTIATION_TYPE = EDC_NAMESPACE + REJECT_NEGOTIATION_TYPE_TERM;

}
