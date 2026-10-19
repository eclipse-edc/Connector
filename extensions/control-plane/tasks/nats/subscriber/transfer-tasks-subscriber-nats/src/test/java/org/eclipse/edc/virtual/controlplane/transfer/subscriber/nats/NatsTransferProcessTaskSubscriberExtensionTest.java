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

package org.eclipse.edc.virtual.controlplane.transfer.subscriber.nats;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class NatsTransferProcessTaskSubscriberExtensionTest {

    private static final String AUTOCREATE = "edc.nats.tp.subscriber.autocreate";
    private static final String STREAM_AUTOCREATE = "edc.nats.tp.subscriber.stream.autocreate";
    private static final String CONSUMER_AUTOCREATE = "edc.nats.tp.subscriber.consumer.autocreate";

    @Test
    void initialize_shouldNotAutoCreate_whenNoSettingProvided(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var subscriber = subscriberFor(Map.of(), context, objectFactory);

        assertThat(subscriber.isAutoCreateStream()).isFalse();
        assertThat(subscriber.isAutoCreateConsumer()).isFalse();
    }

    /**
     * The legacy {@code edc.nats.tp.subscriber.autocreate} setting must keep provisioning both the stream and the
     * consumer, while the granular settings only enable their own component.
     */
    @ParameterizedTest
    @CsvSource({
            "true,  false, false, true,  true",
            "false, true,  false, true,  false",
            "false, false, true,  false, true",
            "false, true,  true,  true,  true",
            "true,  true,  false, true,  true",
    })
    void initialize_shouldMapAutoCreateSettings(boolean autoCreate, boolean streamAutoCreate, boolean consumerAutoCreate,
                                                boolean expectedStream, boolean expectedConsumer,
                                                ServiceExtensionContext context, ObjectFactory objectFactory) {
        var settings = Map.of(
                AUTOCREATE, String.valueOf(autoCreate),
                STREAM_AUTOCREATE, String.valueOf(streamAutoCreate),
                CONSUMER_AUTOCREATE, String.valueOf(consumerAutoCreate)
        );

        var subscriber = subscriberFor(settings, context, objectFactory);

        assertThat(subscriber.isAutoCreateStream()).isEqualTo(expectedStream);
        assertThat(subscriber.isAutoCreateConsumer()).isEqualTo(expectedConsumer);
    }


    @Test
    void initialize_shouldRegisterLivenessProvider(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var healthCheckService = mock(HealthCheckService.class);
        context.registerService(HealthCheckService.class, healthCheckService);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of()));
        var extension = objectFactory.constructInstance(NatsTransferProcessTaskSubscriberExtension.class);

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(LivenessProvider.class);
        verify(healthCheckService).addLivenessProvider(captor.capture());
        var result = captor.getValue().get();
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getComponent()).isEqualTo("NATS transfer process task subscriber");
    }

    @Test
    void initialize_shouldUseSharedRetrySettings(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var subscriber = subscriberFor(Map.of(
                "edc.transfer.send.retry.base-delay.ms", "250",
                "edc.transfer.send.retry.limit", "4"), context, objectFactory);

        assertThat(subscriber).extracting("retryBaseDelayMillis").isEqualTo(250L);
        assertThat(subscriber).extracting("retryLimit").isEqualTo(4);
    }

    @Test
    void initialize_shouldUseDefaultRetrySettings_whenNotConfigured(ServiceExtensionContext context, ObjectFactory objectFactory) {
        var subscriber = subscriberFor(Map.of(), context, objectFactory);

        assertThat(subscriber).extracting("retryBaseDelayMillis").isEqualTo(1000L);
        assertThat(subscriber).extracting("retryLimit").isEqualTo(7);
    }

    private NatsTransferProcessTaskSubscriber subscriberFor(Map<String, String> settings, ServiceExtensionContext context,
                                                            ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        var extension = objectFactory.constructInstance(NatsTransferProcessTaskSubscriberExtension.class);

        extension.initialize(context);

        return extension.subscriber;
    }
}
