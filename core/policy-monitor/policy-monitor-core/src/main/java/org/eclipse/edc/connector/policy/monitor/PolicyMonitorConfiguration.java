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

package org.eclipse.edc.connector.policy.monitor;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

import java.time.Duration;

/**
 * Configuration for the policy monitor watchdog.
 */
@Settings
public record PolicyMonitorConfiguration(

        @Setting(
                description = "The number of policy monitor entries to be evaluated on every watchdog run.",
                key = "batch-size",
                defaultValue = "20"
        )
        int batchSize,

        @Setting(
                description = "Time period between policy monitor evaluations in ISO-8061 duration format.",
                key = "period",
                defaultValue = "PT1H"
        )
        Duration period
) {
}
