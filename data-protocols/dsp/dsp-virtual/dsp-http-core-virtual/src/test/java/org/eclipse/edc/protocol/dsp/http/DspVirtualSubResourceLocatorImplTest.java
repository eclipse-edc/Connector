/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DspVirtualSubResourceLocatorImplTest {

    private final DspVirtualSubResourceLocatorImpl locator = new DspVirtualSubResourceLocatorImpl();

    @Test
    void shouldReturnRegisteredResource() {
        var resource = new Object();

        locator.registerSubResource("catalog", "2025-1", resource);

        assertThat(locator.getSubResource("catalog", "2025-1")).isSameAs(resource);
    }

    @Test
    void shouldReturnNull_whenResourceNameNotRegistered() {
        assertThat(locator.getSubResource("unknown", "2025-1")).isNull();
    }

    @Test
    void shouldReturnNull_whenVersionNotRegisteredForName() {
        locator.registerSubResource("catalog", "2025-1", new Object());

        assertThat(locator.getSubResource("catalog", "2024-1")).isNull();
    }

    @Test
    void shouldKeepResourcesIndependentByName() {
        var catalog = new Object();
        var negotiation = new Object();

        locator.registerSubResource("catalog", "2025-1", catalog);
        locator.registerSubResource("negotiations", "2025-1", negotiation);

        assertThat(locator.getSubResource("catalog", "2025-1")).isSameAs(catalog);
        assertThat(locator.getSubResource("negotiations", "2025-1")).isSameAs(negotiation);
    }

    @Test
    void shouldKeepResourcesIndependentByVersion() {
        var v1 = new Object();
        var v2 = new Object();

        locator.registerSubResource("catalog", "2024-1", v1);
        locator.registerSubResource("catalog", "2025-1", v2);

        assertThat(locator.getSubResource("catalog", "2024-1")).isSameAs(v1);
        assertThat(locator.getSubResource("catalog", "2025-1")).isSameAs(v2);
    }

    @Test
    void shouldOverwriteResource_whenRegisteredTwiceWithSameNameAndVersion() {
        var first = new Object();
        var second = new Object();

        locator.registerSubResource("catalog", "2025-1", first);
        locator.registerSubResource("catalog", "2025-1", second);

        assertThat(locator.getSubResource("catalog", "2025-1")).isSameAs(second);
    }

}
