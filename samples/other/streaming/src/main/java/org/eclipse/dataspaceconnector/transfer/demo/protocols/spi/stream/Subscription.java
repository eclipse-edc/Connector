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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A subscription to a destination topic.
 */
public class Subscription {
    private final String topicName;
    private final String subscriptionId;

    public Subscription(@JsonProperty("topicName") String topicName, @JsonProperty("subscriptionId") String subscriptionId) {
        this.topicName = topicName;
        this.subscriptionId = subscriptionId;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }


}
