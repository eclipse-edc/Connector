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

package org.eclipse.edc.virtual.controlplane.contract.negotiation.subscriber;

import org.eclipse.edc.controlplane.contract.spi.negotiation.ContractNegotiationTaskExecutor;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

public class NatsContractNegotiationSubscriberExtension implements ServiceExtension {

    @Configuration
    private NatsSubscriberConfig subscriberConfig;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ContractNegotiationTaskExecutor taskExecutor;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject
    private Monitor monitor;

    @Inject
    private TaskService taskService;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private Clock clock;

    private NatsContractNegotiationTaskSubscriber subscriber;


    @Override
    public void initialize(ServiceExtensionContext context) {
        subscriber = NatsContractNegotiationTaskSubscriber.Builder.newInstance()
                .url(subscriberConfig.url())
                .name(subscriberConfig.name())
                .stream(subscriberConfig.stream)
                .subject(subscriberConfig.subject())
                .monitor(monitor)
                .mapperSupplier(() -> typeManager.getMapper())
                .taskExecutor(taskExecutor)
                .autoCreate(subscriberConfig.autoCreate)
                .executorInstrumentation(executorInstrumentation)
                .batchSize(subscriberConfig.batchSize)
                .maxWait(subscriberConfig.maxWait)
                .taskService(taskService)
                .transactionContext(transactionContext)
                .maxRetries(subscriberConfig.maxRetries)
                .clock(clock)
                .build();
    }

    @Override
    public void prepare() {
        if (subscriber != null) {
            subscriber.prepare();
        }
    }

    @Override
    public void start() {
        if (subscriber != null) {
            subscriber.start();
        }
    }

    @Override
    public void shutdown() {
        if (subscriber != null) {
            subscriber.stop();
        }
    }

    @Settings
    public record NatsSubscriberConfig(
            @Setting(key = "edc.nats.cn.subscriber.url", description = "The URL of the NATS server to connect to for contract negotiation events.", defaultValue = "nats://localhost:4222")
            String url,
            @Setting(key = "edc.nats.cn.subscriber.name", description = "The name of the consumer for contract negotiation events", defaultValue = "cn-subscriber")
            String name,
            @Setting(key = "edc.nats.cn.subscriber.autocreate", description = "When true, it will automatically create the stream and the consumer if not present", defaultValue = "false")
            Boolean autoCreate,
            @Setting(key = "edc.nats.cn.subscriber.stream", description = "The stream name where to attach the consumer", defaultValue = "cn-stream")
            String stream,
            @Setting(key = "edc.nats.cn.subscriber.subject", description = "The subject of the consumer for contract negotiation events", defaultValue = "negotiations.>")
            String subject,
            @Setting(key = "edc.nats.cn.subscriber.batch-size", description = "The size of the batch when fetching messages", defaultValue = "100")
            Integer batchSize,
            @Setting(key = "edc.nats.cn.subscriber.max-wait", description = "The max waiting time for messages (ms)", defaultValue = "100")
            Integer maxWait,
            @Setting(key = "edc.nats.cn.subscriber.max-retries", description = "Max retries for task execution failure on transient errors", defaultValue = "3")
            Integer maxRetries
    ) {
    }
}
