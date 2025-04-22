/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.protocol;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public interface DspRuntime {

    static EmbeddedRuntime createRuntimeWith(int protocolPort, String... additionalModules) {
        var baseModules = Stream.of(
                ":data-protocols:dsp:dsp-http-api-configuration",
                ":data-protocols:dsp:dsp-http-api-base-configuration",
                ":data-protocols:dsp:dsp-http-core",
                ":extensions:common:iam:iam-mock",
                ":core:control-plane:control-plane-aggregate-services",
                ":core:control-plane:control-plane-core",
                ":extensions:common:http",
                ":core:common:connector-core",
                ":core:common:runtime-core"
        );

        var modules = Stream.concat(baseModules, Arrays.stream(additionalModules)).toArray(String[]::new);

        return new EmbeddedRuntime("runtime", modules).configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                "web.http.protocol.path", "/protocol",
                "web.http.protocol.port", String.valueOf(protocolPort)
        )));
    }

}
