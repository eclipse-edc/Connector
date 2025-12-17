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

package org.eclipse.edc.test.runtime.signaling;

import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.logic.OnStart;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

public class SignalingDataPlaneRuntimeExtension implements ServiceExtension {

    @Setting(key = "signaling.dataplane.controlplane.endpoint")
    private String controlplaneEndpoint;
    @Setting(key = "web.http.port")
    private int httpPort;
    @Setting(key = "web.http.path")
    private String httpPath;

    @Inject
    private WebService webService;

    private Dataplane dataplane;

    @Override
    public void initialize(ServiceExtensionContext context) {
        dataplane = Dataplane.newInstance()
                .endpoint("http://localhost:%d%s/v1/dataflows".formatted(httpPort, httpPath))
                .transferType("Finite-PUSH")
                .onStart(new DataplaneOnStart())
                .build();
        webService.registerResource(dataplane.controller());
    }

    @Override
    public void start() {
        dataplane.registerOn(controlplaneEndpoint)
                .orElseThrow(e -> new RuntimeException("Cannot register dataplane on controlplane", e));
    }

    private static class DataplaneOnStart implements OnStart {
        @Override
        public Result<DataFlow> action(DataFlow dataFlow) {
            return Result.success(dataFlow);
        }
    }
}
