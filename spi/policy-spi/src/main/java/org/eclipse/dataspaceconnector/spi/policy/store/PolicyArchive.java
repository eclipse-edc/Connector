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

import java.util.stream.Stream;

/**
 * Resolves {@linkplain org.eclipse.dataspaceconnector.policy.model.Policy} objects, that are part of a contract agreement.
 * Thus, this archive only houses policies from "foreign" EDC instances.
 */

@FunctionalInterface
public interface PolicyArchive {
    /**
     * Returns a stream of distinct policies for a given ID.
     */
    Stream<Policy> findPolicyById(String policyId);
}
