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

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.logic.OnPrepare;
import org.eclipse.dataplane.logic.OnStart;
import org.eclipse.dataplane.logic.OnTerminate;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

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
                .transferType("NonFinite-PUSH")
                .onPrepare(new DataplaneOnPrepare())
                .onStart(new DataplaneOnStart())
                .onStarted(Result::success)
                .onCompleted(Result::success)
                .onTerminate(new DataplaneOnTerminate())
                .build();
        webService.registerResource(dataplane.controller());
        webService.registerResource(new ReceiveDataController(context.getMonitor()));
    }

    @Override
    public void start() {
        dataplane.registerOn(controlplaneEndpoint)
                .orElseThrow(e -> new RuntimeException("Cannot register dataplane on controlplane", e));
    }

    private class DataplaneOnStart implements OnStart {
        @Override
        public Result<DataFlow> action(DataFlow dataFlow) {
            if (dataFlow.getDataAddress() == null) {
                return Result.failure(new InvalidRequestException("DataAddress should not be null for PUSH transfers"));
            }

            if (dataFlow.getTransferType().equals("NonFinite-PUSH")) {
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                    var destinationUri = URI.create(dataFlow.getDataAddress().endpoint());
                    var request = HttpRequest.newBuilder(destinationUri).POST(HttpRequest.BodyPublishers.ofString("test-data")).build();
                    HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding());
                }, 0, 200, TimeUnit.MILLISECONDS);

                return Result.success(dataFlow);
            }

            if (dataFlow.getTransferType().equals("Finite-PUSH")) {
                var destinationUri = URI.create(dataFlow.getDataAddress().endpoint());
                var request = HttpRequest.newBuilder(destinationUri).POST(HttpRequest.BodyPublishers.ofString("test-data")).build();
                HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .whenComplete((response, throwable) -> {
                            if (throwable == null) {
                                var statusCode = response.statusCode();
                                if (statusCode >= 200 && statusCode < 300) {
                                    notifyCompleted(dataFlow);
                                } else {
                                    dataplane.notifyErrored(dataFlow.getId(), new RuntimeException("Destination endpoint responded with " + statusCode));
                                }
                            } else {
                                dataplane.notifyErrored(dataFlow.getId(), throwable);
                            }
                        });

                return Result.success(dataFlow);
            }

            return Result.failure(new RuntimeException("TransferType %s not supported".formatted(dataFlow.getTransferType())));
        }

        private void notifyCompleted(DataFlow dataFlow) {
            var retryPolicy = RetryPolicy.builder().withMaxRetries(5).withDelay(Duration.ofSeconds(1)).build();
            Failsafe.with(retryPolicy).run(context -> {
                if (dataplane.notifyCompleted(dataFlow.getId()).failed()) {
                    throw new RuntimeException("Notification failed");
                }
            });
        }
    }

    private class DataplaneOnPrepare implements OnPrepare {
        @Override
        public Result<DataFlow> action(DataFlow dataFlow) {
            var destination = new DataAddress("Finite-PUSH", "http", "http://localhost:%d%s/receive".formatted(httpPort, httpPath), emptyList());
            dataFlow.setDataAddress(destination);
            return Result.success(dataFlow);
        }
    }

    private class DataplaneOnTerminate implements OnTerminate {
        @Override
        public Result<DataFlow> action(DataFlow dataFlow) {
            return Result.success(dataFlow);
        }
    }
}
