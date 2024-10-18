/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.web.jersey.feature;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamicResourceFeatureTest {

    @Test
    void configure() {
        var feature = new DynamicResourceFeature(Map.of(Target.class, List.of(new Feature())));
        var resourceInfo = mock(ResourceInfo.class);
        var featureContext = mock(FeatureContext.class);

        when(resourceInfo.getResourceClass()).then((a) -> Target.class);

        feature.configure(resourceInfo, featureContext);

        verify(featureContext).register(isA(Feature.class));

    }

    record Feature() {
    }

    record Target() {
    }
}
