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

@Settings
public record CatalogCrawlerConfiguration(

        @Setting(
                description = "Determines whether catalog crawling is globally enabled or disabled",
                key = CATALOG_CRAWLER_ENABLED_SETTING,
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

    public static final String CATALOG_CRAWLER_ENABLED_SETTING = "edc.catalog.cache.execution.enabled";

}
