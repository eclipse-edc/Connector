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
 * The result of a subscription attempt.
 */
public class SubscriptionResult {
    private final boolean success;
    private String error;
    private Subscription subscription;

    public SubscriptionResult(String error) {
        success = false;
        this.error = error;
    }

    public SubscriptionResult(Subscription subscription) {
        success = true;
        this.subscription = subscription;
    }

    /**
     * Returns true if the subscription was successful.
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns an error if the connection was unsuccessful; otherwise null.
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the subscription handle.
     */
    public Subscription getSubscription() {
        return subscription;
    }

}
