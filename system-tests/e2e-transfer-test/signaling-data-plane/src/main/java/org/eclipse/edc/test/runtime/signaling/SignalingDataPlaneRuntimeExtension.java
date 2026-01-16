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
import org.eclipse.dataplane.logic.OnStarted;
import org.eclipse.dataplane.logic.OnTerminate;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
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
    @Inject
    private Monitor monitor;

    private Dataplane dataplane;
    private Map<String, ScheduledFuture<?>> ongoingNonFiniteTransfers = new HashMap<>();

    @Override
    public void initialize(ServiceExtensionContext context) {
        dataplane = Dataplane.newInstance()
                .endpoint("http://localhost:%d%s/v1/dataflows".formatted(httpPort, httpPath))
                .transferType("Finite-PUSH")
                .transferType("Finite-PULL")
                .transferType("NonFinite-PUSH")
                .transferType("NonFinite-PULL")
                .onPrepare(new DataplaneOnPrepare())
                .onStart(new DataplaneOnStart())
                .onStarted(new DataplaneOnStarted())
                .onCompleted(Result::success)
                .onTerminate(new DataplaneOnTerminate())
                .build();
        webService.registerResource(dataplane.controller());
        webService.registerResource(new ReceiveDataController(monitor));
        webService.registerResource(new SourceDataController(monitor));
    }

    @Override
    public void start() {
        dataplane.registerOn(controlplaneEndpoint)
                .orElseThrow(e -> new RuntimeException("Cannot register dataplane on controlplane", e));
    }

    private class DataplaneOnStart implements OnStart {
        @Override
        public Result<DataFlow> action(DataFlow dataFlow) {

            switch (dataFlow.getTransferType()) {
                case "NonFinite-PUSH" -> {
                    if (dataFlow.getDataAddress() == null) {
                        return Result.failure(new InvalidRequestException("DataAddress should not be null for PUSH transfers"));
                    }

                    var future = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                        var destinationUri = URI.create(dataFlow.getDataAddress().endpoint());
                        var request = HttpRequest.newBuilder(destinationUri).POST(HttpRequest.BodyPublishers.ofString("test-data")).build();
                        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding());
                    }, 0, 200, TimeUnit.MILLISECONDS);

                    ongoingNonFiniteTransfers.put(dataFlow.getId(), future);

                    return Result.success(dataFlow);
                }
                case "Finite-PUSH" -> {
                    if (dataFlow.getDataAddress() == null) {
                        return Result.failure(new InvalidRequestException("DataAddress should not be null for PUSH transfers"));
                    }

                    var destinationUri = URI.create(dataFlow.getDataAddress().endpoint());
                    var request = HttpRequest.newBuilder(destinationUri).POST(HttpRequest.BodyPublishers.ofString("test-data")).build();
                    HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding())
                            .whenComplete((response, throwable) -> notifyCompletion(dataFlow, response, throwable));

                    return Result.success(dataFlow);
                }
                case "NonFinite-PULL", "Finite-PULL" -> {
                    var dataAddress = new DataAddress(dataFlow.getTransferType(), "http", "http://localhost:%d%s/source".formatted(httpPort, httpPath), emptyList());
                    dataFlow.setDataAddress(dataAddress);
                    return Result.success(dataFlow);
                }
                default -> {
                    return Result.failure(new RuntimeException("TransferType %s not supported".formatted(dataFlow.getTransferType())));
                }
            }
        }

    }

    private class DataplaneOnStarted implements OnStarted {
        @Override
        public Result<DataFlow> action(DataFlow dataFlow) {

            switch (dataFlow.getTransferType()) {
                case "NonFinite-PULL" -> {
                    var sourceUri = URI.create(dataFlow.getDataAddress().endpoint());
                    var future = Executors.newSingleThreadScheduledExecutor()
                            .scheduleAtFixedRate(() -> requestData(dataFlow, sourceUri), 0, 200, TimeUnit.MILLISECONDS);

                    ongoingNonFiniteTransfers.put(dataFlow.getId(), future);
                }
                case "Finite-PULL" -> {
                    var sourceUri = URI.create(dataFlow.getDataAddress().endpoint());
                    requestData(dataFlow, sourceUri)
                            .whenComplete((response, throwable) -> notifyCompletion(dataFlow, response, throwable));
                }
                default -> { }
            }

            return Result.success(dataFlow);
        }

        private CompletableFuture<HttpResponse<String>> requestData(DataFlow dataFlow, URI sourceUri) {
            var request = HttpRequest.newBuilder(sourceUri).GET().build();
            return HttpClient.newHttpClient().sendAsync(request, ofString()).whenComplete((response, throwable) -> {
                if (throwable == null) {
                    if (response.statusCode() == 200) {
                        var body = response.body();
                        monitor.info("Received data for data flow %s: %s".formatted(dataFlow.getId(), body));
                    } else {
                        monitor.severe("Error retrieving data: %s: %s".formatted(response.statusCode(), response.body()));
                    }
                } else {
                    monitor.severe("Error retrieving data", throwable);
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
            var future = ongoingNonFiniteTransfers.get(dataFlow.getId());
            if (future != null) {
                future.cancel(true);
                ongoingNonFiniteTransfers.remove(dataFlow.getId());
                monitor.info("Ongoing flow %s terminated".formatted(dataFlow.getId()));
            }
            return Result.success(dataFlow);
        }
    }

    private void notifyCompletion(DataFlow dataFlow, HttpResponse<?> response, Throwable throwable) {
        if (throwable == null) {
            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                notifyCompleted(dataFlow);
            } else {
                dataplane.notifyErrored(dataFlow.getId(), new RuntimeException("Data source/destination endpoint responded with " + statusCode));
            }
        } else {
            dataplane.notifyErrored(dataFlow.getId(), throwable);
        }
    }

    private void notifyCompleted(DataFlow dataFlow) {
        var retryPolicy = RetryPolicy.builder().withMaxRetries(5).withDelay(Duration.ofSeconds(1)).build();
        Failsafe.with(retryPolicy).run(context -> {
            var notifyCompleted = dataplane.notifyCompleted(dataFlow.getId());
            if (notifyCompleted.failed()) {
                throw new RuntimeException("Notify Completed failed: " + notifyCompleted);
            }
        });
    }

}
