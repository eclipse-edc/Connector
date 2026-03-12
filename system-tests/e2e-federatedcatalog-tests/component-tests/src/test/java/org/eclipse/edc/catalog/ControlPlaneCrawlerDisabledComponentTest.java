/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.catalog;

import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.catalog.TestFunctions.queryCatalogApi;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Component test that validates the federated catalog crawler can be disabled
 * inside a standard control plane runtime (controlplane-base-bom) via
 * {@code edc.catalog.cache.execution.enabled=false}.
 * <p>
 * When disabled, the crawler should not crawl any targets, and the federated
 * catalog API should return an empty result set.
 */
@ComponentTest
public class ControlPlaneCrawlerDisabledComponentTest {

    @RegisterExtension
    protected static RuntimeExtension runtime = new RuntimePerMethodExtension(
            new EmbeddedRuntime("controlplane-crawler-disabled",
                    ":dist:bom:controlplane-base-bom",
                    ":extensions:common:iam:iam-mock")
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                            Map.entry("edc.catalog.cache.execution.enabled", "false"),
                            Map.entry("edc.catalog.cache.execution.period.seconds", "2"),
                            Map.entry("edc.catalog.cache.partition.num.crawlers", "10"),
                            Map.entry("edc.catalog.cache.execution.delay.seconds", "1"),
                            Map.entry("web.http.management.port", valueOf(TestFunctions.MANAGEMENT_PORT)),
                            Map.entry("web.http.management.path", TestFunctions.MANAGEMENT_BASE_PATH),
                            Map.entry("web.http.port", valueOf(getFreePort())),
                            Map.entry("web.http.path", "/api"),
                            Map.entry("web.http.protocol.port", valueOf(getFreePort())),
                            Map.entry("web.http.protocol.path", "/api/v1/dsp"),
                            Map.entry("web.http.control.port", valueOf(getFreePort())),
                            Map.entry("web.http.control.path", "/api/control"),
                            Map.entry("edc.participant.id", "test-participant")
                    )))
    );

    private final DspHttpRemoteMessageDispatcher dispatcher = mock();

    @Test
    @DisplayName("Verify crawler is inactive when edc.catalog.cache.execution.enabled=false")
    void crawlerDisabled_shouldNotCrawlAndReturnEmptyResults(RemoteMessageDispatcherRegistry reg, TargetNodeDirectory directory, JsonLd jsonLd) {
        // insert a target node and register a dispatcher — but neither should be invoked
        directory.insert(new TargetNode("test-node", "did:web:" + UUID.randomUUID(),
                "http://test-node.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1)));
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);

        // wait long enough for the crawler to have run if it were active (delay=1s, period=2s)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // verify the dispatcher was never called — the crawler did not crawl
        verifyNoInteractions(dispatcher);

        // verify the federated catalog API returns an empty result
        var catalogs = queryCatalogApi(jsonLd, jsonObject -> {
            throw new AssertionError("Should not have any results to transform");
        });
        assertThat(catalogs).isEmpty();
    }
}
