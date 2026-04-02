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

package org.eclipse.edc.nats;

import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

public class NatsFunctions {

    public static void createStream(JetStreamManagement jsm, String streamName, StorageType storageType, String... subject) {
        try {
            if (!streamExists(jsm, streamName)) {
                var streamConfig = StreamConfiguration.builder()
                        .name(streamName)
                        .subjects(subject)
                        .storageType(storageType)
                        .build();
                jsm.addStream(streamConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createConsumer(JetStreamManagement jsm, String streamName, String consumerName) {
        createConsumer(jsm, streamName, consumerName, null);
    }

    public static void createConsumer(JetStreamManagement jsm, String streamName, String consumerName, String filterSubject) {
        try {
            jsm.addOrUpdateConsumer(streamName, ConsumerConfiguration.builder()
                    .durable(consumerName)
                    .name(consumerName)
                    .filterSubject(filterSubject)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean streamExists(JetStreamManagement jsm, String streamName) {
        try {
            var si = jsm.getStreamInfo(streamName);
            return si != null;
        } catch (JetStreamApiException e) {
            // This means the stream doesn't exist
            if (e.getErrorCode() == 404) {
                return false;
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
