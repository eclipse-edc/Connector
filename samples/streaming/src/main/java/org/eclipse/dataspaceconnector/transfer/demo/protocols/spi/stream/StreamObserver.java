/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream;

/**
 * Observes a topic, receiving callbacks when data is published to it.
 */
@FunctionalInterface
public interface StreamObserver {
    /**
     * Callback when data is published to a topic.
     */
    void onPublish(String topicName, byte[] payload);
}
