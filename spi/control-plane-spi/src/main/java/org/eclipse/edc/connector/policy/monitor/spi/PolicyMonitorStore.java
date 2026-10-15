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

import org.eclipse.edc.spi.persistence.StateEntityStore;

public interface PolicyMonitorStore extends StateEntityStore<PolicyMonitorEntry> {

    /**
     * Setting that defines how long a runtime keeps the lease on a policy monitor entry while processing it, in ISO-8601
     * duration format.
     */
    String LEASE_DURATION_SETTING = "edc.policy.monitor.lease-duration";

    /**
     * Default value of {@link #LEASE_DURATION_SETTING}.
     */
    String LEASE_DURATION_DEFAULT_VALUE = "PT60S";
}
