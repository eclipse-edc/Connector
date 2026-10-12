/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.agreement;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a request to retire a {@link ContractAgreement}, carrying the reason for the retirement.
 *
 * @param reason the reason why the agreement is being retired.
 */
public record RetireAgreement(String reason) {

    public static final String RETIRE_AGREEMENT_TYPE_TERM = "RetireAgreement";
    public static final String RETIRE_AGREEMENT_TYPE = EDC_NAMESPACE + RETIRE_AGREEMENT_TYPE_TERM;
    public static final String RETIRE_AGREEMENT_REASON = EDC_NAMESPACE + "reason";

}
