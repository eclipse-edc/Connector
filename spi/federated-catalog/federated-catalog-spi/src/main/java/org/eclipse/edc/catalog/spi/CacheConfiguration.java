/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.catalog.spi;

import org.eclipse.edc.catalog.spi.model.ExecutionPlan;
import org.eclipse.edc.catalog.spi.model.RecurringExecutionPlan;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Duration;
import java.util.Random;

import static java.lang.String.format;

/**
 * Object that provides configuration for the federated catalog.
 * All configuration values that do not allow for default values are resolved instantly, all others are resolved
 * lazily from the context.
 */
public class CacheConfiguration {

    @Setting("The time to elapse between two crawl runs")
    static final String EXECUTION_PLAN_PERIOD_SECONDS = "edc.catalog.cache.execution.period.seconds";
    @Setting("The number of crawlers (execution threads) that should be used. The engine will re-use crawlers when necessary.")
    static final String NUM_CRAWLER_SETTING = "edc.catalog.cache.partition.num.crawlers";
    @Setting("The initial delay for the cache crawler engine")
    static final String EXECUTION_PLAN_DELAY_SECONDS = "edc.catalog.cache.execution.delay.seconds";
    private static final int DEFAULT_EXECUTION_PERIOD_SECONDS = 60;
    private static final int LOW_EXECUTION_PERIOD_SECONDS_THRESHOLD = 10;
    private static final int DEFAULT_NUMBER_OF_CRAWLERS = 2;

    private final ServiceExtensionContext context;

    public CacheConfiguration(ServiceExtensionContext context) {
        this.context = context;
    }

    public int getNumCrawlers() {
        return context.getSetting(NUM_CRAWLER_SETTING, DEFAULT_NUMBER_OF_CRAWLERS);
    }

    public ExecutionPlan getExecutionPlan() {
        var periodSeconds = context.getSetting(EXECUTION_PLAN_PERIOD_SECONDS, DEFAULT_EXECUTION_PERIOD_SECONDS);
        var setting = context.getSetting(EXECUTION_PLAN_DELAY_SECONDS, null);
        int initialDelaySeconds;
        if ("random".equals(setting) || setting == null) {
            initialDelaySeconds = randomSeconds();
        } else {
            try {
                initialDelaySeconds = Integer.parseInt(setting);
            } catch (NumberFormatException ex) {
                initialDelaySeconds = 0;
            }
        }
        var monitor = context.getMonitor();
        if (periodSeconds < LOW_EXECUTION_PERIOD_SECONDS_THRESHOLD) {
            monitor.warning(format("An execution period of %d seconds is very low (threshold = %d). This might result in the work queue to be ever growing." +
                    " A longer execution period or more crawler threads (currently using %d) should be considered.", periodSeconds, LOW_EXECUTION_PERIOD_SECONDS_THRESHOLD, getNumCrawlers()));
        }
        return new RecurringExecutionPlan(Duration.ofSeconds(periodSeconds), Duration.ofSeconds(initialDelaySeconds), monitor);
    }

    private int randomSeconds() {
        var rnd = new Random();
        return 10 + rnd.nextInt(90);
    }
}
