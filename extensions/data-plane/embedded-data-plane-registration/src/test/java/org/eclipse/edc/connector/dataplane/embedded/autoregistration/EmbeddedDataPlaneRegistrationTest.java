/*
 *  Copyright (c) 2024 Zub4t  (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Zub4t  (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.embedded.autoregistration;


import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
public class EmbeddedDataPlaneRegistrationTest {

    @Test
    void testName(EmbeddedDataPlaneRegistration extension) {
        Assertions.assertNotNull(extension.name());
        Assertions.assertEquals("Embedded DataPlane Auto Registration", extension.name());
    }
    @Test
    void embeddedDataPlaneRegistration_shouldThrowEdcException(EmbeddedDataPlaneRegistration extension, ServiceExtensionContext context) {
        assertThrows(org.eclipse.edc.spi.EdcException.class, () -> {
            extension.initialize(context);
            var dataPlaneInstance = extension.createDataPlane(ConfigFactory.fromMap(Collections.emptyMap()));

        });
    }
    @Test
    void embeddedDataPlaneRegistration_shouldNotAddDataPlane(EmbeddedDataPlaneRegistration extension, ServiceExtensionContext context) {
        var dataPlaneManager = mock(DataPlaneManager.class);
        context.registerService(DataPlaneManager.class,dataPlaneManager);
        extension.initialize(context);
        var dataPlaneInstance = extension.createDataPlane(ConfigFactory.fromMap(Collections.emptyMap()));
        assertThat(dataPlaneInstance).isNull();
    }
    @Test
    void embeddedDataPlaneRegistration_shouldAddDataPlane(EmbeddedDataPlaneRegistration extension, ServiceExtensionContext context) {
        var dataPlaneManager = mock(DataPlaneManager.class);
        context.registerService(DataPlaneManager.class,dataPlaneManager);

        List<String> keys = List.of(
                EmbeddedDataPlaneConfig.DESTINATION_TYPES_SUFFIX,
                EmbeddedDataPlaneConfig.SOURCE_TYPES_SUFFIX,
                EmbeddedDataPlaneConfig.URL_SUFFIX,
                EmbeddedDataPlaneConfig.PROPERTIES_SUFFIX);

        List<String> values = List.of("HttpProxy,HttpData","HttpData","http://localhost:19192/control/transfer","{\"publicApiUrl\": \"http://localhost:19291/public/\"}");
        Map<String, String> map = IntStream.range(0, keys.size())
                .boxed()
                .collect(HashMap::new, (m, i) -> m.put(keys.get(i), values.get(i)), Map::putAll);

        extension.initialize(context);
        var dataPlaneInstance = extension.createDataPlane(ConfigFactory.fromMap(map));
        assertThat(dataPlaneInstance).isInstanceOf(DataPlaneInstance.class);
    }
}