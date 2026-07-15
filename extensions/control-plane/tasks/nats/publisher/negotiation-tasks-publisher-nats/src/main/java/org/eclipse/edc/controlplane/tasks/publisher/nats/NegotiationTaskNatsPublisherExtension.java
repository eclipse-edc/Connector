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

package org.eclipse.edc.controlplane.tasks.publisher.nats;

import io.nats.client.Nats;
import io.nats.client.Options;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.ContractNegotiationTaskPayload;
import org.eclipse.edc.controlplane.tasks.TaskObservable;
import org.eclipse.edc.nats.tasks.publisher.NatsTaskPublisher;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;

public class NegotiationTaskNatsPublisherExtension implements ServiceExtension {

    @Inject
    private TypeManager typeManager;

    @Configuration
    private NatsCnPublisherConfig natsCnPublisherConfig;

    @Inject
    private Clock clock;

    @Inject
    private Monitor monitor;

    @Inject
    private TaskObservable taskObservable;

    // authentication options can be contributed from the outside (e.g. the NKey auth extension).
    // Note that the 'server' will be overwritten!
    @Inject(required = false)
    private Options authenticationOptions;

    @Override
    public void initialize(ServiceExtensionContext context) {
        try {
            registerPublisher(natsCnPublisherConfig.url(), natsCnPublisherConfig.subjectPrefix());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void registerPublisher(String natsUrl, String subjectPrefix) {
        try {
            var builder = authenticationOptions != null ? new Options.Builder(authenticationOptions) : new Options.Builder();
            var connection = Nats.connect(builder.server(natsUrl).build());
            var js = connection.jetStream();
            var publisher = new NatsTaskPublisher(subjectPrefix, ContractNegotiationTaskPayload.class, js, monitor, () -> typeManager.getMapper());

            taskObservable.registerListener(publisher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Settings
    public record NatsCnPublisherConfig(
            @Setting(key = "edc.nats.cn.publisher.url", description = "The URL of the NATS server to connect to for publishing contract negotiation tasks.", defaultValue = "nats://localhost:4222")
            String url,
            @Setting(key = "edc.nats.cn.publisher.subject-prefix", description = "The prefix for the subjects", defaultValue = "negotiations")
            String subjectPrefix
    ) {
    }

}
