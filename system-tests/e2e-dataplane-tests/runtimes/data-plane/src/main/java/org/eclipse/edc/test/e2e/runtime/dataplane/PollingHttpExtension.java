/*
 *  Copyright (c) 2024 Cofinity-X
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

package org.eclipse.edc.test.e2e.runtime.dataplane;

import okhttp3.Request;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Testing extension that registers a PollingHttp DataSourceFactory.
 *
 * Used to test long-running push transfers.
 */
public class PollingHttpExtension implements ServiceExtension {

    @Inject
    private PipelineService pipelineService;

    @Inject
    private EdcHttpClient edcHttpClient;

    @Override
    public void initialize(ServiceExtensionContext context) {
        pipelineService.registerFactory(new PollingHttpDataSourceFactory(edcHttpClient));
    }

    private record PollingHttpDataSourceFactory(EdcHttpClient edcHttpClient) implements DataSourceFactory {

        @Override
        public String supportedType() {
            return "PollingHttp";
        }

        @Override
        public DataSource createSource(DataFlowStartMessage request) {
            return new PollingHttpDataSource(edcHttpClient, request.getSourceDataAddress());
        }

        @Override
        public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
            return Result.success();
        }
    }

    private static class PollingHttpDataSource implements DataSource {

        private final EdcHttpClient edcHttpClient;
        private final DataAddress dataAddress;
        private final ScheduledExecutorService executor;
        private final BlockingQueue<Part> requestQueue = new ArrayBlockingQueue<>(10);
        private ScheduledFuture<?> future;

        PollingHttpDataSource(EdcHttpClient edcHttpClient, DataAddress dataAddress) {
            this.edcHttpClient = edcHttpClient;
            this.dataAddress = dataAddress;
            this.executor = Executors.newScheduledThreadPool(1);
        }

        @Override
        public StreamResult<Stream<Part>> openPartStream() {
            future = executor.scheduleAtFixedRate(() -> {
                var request = new Request.Builder().url(dataAddress.getStringProperty("baseUrl")).get().build();
                try {
                    var responseBody = edcHttpClient.execute(request).body();
                    requestQueue.add(new PollingHttpPart(UUID.randomUUID().toString(), responseBody.byteStream(), "application/ octet-stream"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0L, 100L, MILLISECONDS);

            return StreamResult.success(Stream.generate(() -> {
                try {
                    return requestQueue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        @Override
        public void close() {
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    public record PollingHttpPart(String name, InputStream content, String mediaType) implements DataSource.Part {

        @Override
        public long size() {
            return SIZE_UNKNOWN;
        }

        @Override
        public InputStream openStream() {
            return content;
        }

        @Override
        public String mediaType() {
            return mediaType;
        }
    }
}
