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

package org.eclipse.edc.connector.controlplane.policy.spi.store;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Resolves {@linkplain Policy} objects that are part of a contract agreement.
 */
@ExtensionPoint
public interface PolicyArchive {
    /**
     * Returns a policy for a given id.
     */
    Policy findPolicyForContract(String contractId);

    String getAgreementIdForContract(String contractId);
}
