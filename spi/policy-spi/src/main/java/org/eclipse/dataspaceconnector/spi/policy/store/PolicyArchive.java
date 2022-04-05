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

package org.eclipse.dataspaceconnector.spi.policy.store;

import org.eclipse.dataspaceconnector.policy.model.Policy;

/**
 * Resolves {@linkplain org.eclipse.dataspaceconnector.policy.model.Policy} objects that are part of a contract agreement.
 */
@FunctionalInterface
public interface PolicyArchive {
    /**
     * Returns a policy for a given id.
     */
    Policy findPolicyForContract(String contractId);
}
