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

package org.eclipse.edc.connector.policy.monitor.spi;

import org.eclipse.edc.spi.entity.StateEntityManager;

/**
 * Iterates over ongoing transfers, verifying their policies and completing when the policy can no longer be evaluated.
 */
public interface PolicyMonitorManager extends StateEntityManager {

    /**
     * Start to monitor a transfer process to ensure that
     *
     * @param transferProcessId the transfer process id
     * @param contractId the contract id
     */
    void startMonitoring(String transferProcessId, String contractId);
}
