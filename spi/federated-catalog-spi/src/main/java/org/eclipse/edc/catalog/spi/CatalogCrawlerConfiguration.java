/*
 *  Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.edc.catalog.spi;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

import java.util.Optional;

import static java.lang.String.format;

@Settings
public record CatalogCrawlerConfiguration(

        @Setting(
                description = "Determines whether catalog crawling is globally enabled or disabled",
                key = "edc.catalog.cache.execution.enabled",
                defaultValue = "true")
        boolean enabled,

        @Setting(
                description = "The number of crawlers (execution threads) that should be used. The engine will re-use crawlers when necessary.",
                key = "edc.catalog.cache.partition.num.crawlers",
                defaultValue = "2")
        int numCrawlers,

        @Setting(
                description = "The time to elapse between two crawl runs",
                key = "edc.catalog.cache.execution.period.seconds",
                defaultValue = "60")
        long periodSeconds,

        @Setting(
                description = "The initial delay for the cache crawler engine",
                key = "edc.catalog.cache.execution.delay.seconds",
                required = false,
                defaultValue = "0")
        int delaySeconds,

        @Setting(
                description = "How many retries will be executed in case of crawler failure",
                key = "edc.catalog.cache.retry.retries.max",
                defaultValue = "5")
        int maxRetries,

        @Setting(
                description = "How many seconds the crawler will await after a failure before retry",
                key = "edc.catalog.cache.retry.delay.seconds",
                defaultValue = "10")
        int retryDelaySeconds

) {
    private static final int LOW_EXECUTION_PERIOD_SECONDS_THRESHOLD = 10;

    public Optional<String> checkPeriodSeconds() {
        if (periodSeconds() < LOW_EXECUTION_PERIOD_SECONDS_THRESHOLD) {
            var message = format("An execution period of %d seconds is very low (threshold = %d). This might result in the work queue to be ever growing." +
                    " A longer execution period or more crawler threads (currently using %d) should be considered.", periodSeconds(), LOW_EXECUTION_PERIOD_SECONDS_THRESHOLD, numCrawlers());
            return Optional.of(message);
        }

        return Optional.empty();
    }
}
