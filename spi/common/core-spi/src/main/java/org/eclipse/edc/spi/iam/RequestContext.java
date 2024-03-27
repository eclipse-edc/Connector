/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.iam;

import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Provides additional context for scope extractors.
 */
public class RequestContext {

    private RemoteMessage message;
    private Direction direction;

    private RequestContext() {
    }

    /**
     * Return the {@link RemoteMessage} associated to the request
     *
     * @return The message
     */
    public RemoteMessage getMessage() {
        return message;
    }

    /**
     * Returns the direction of the message Egress/Ingress
     *
     * @return The direction
     */
    public Direction getDirection() {
        return direction;
    }

    public enum Direction {
        Egress,
        Ingress
    }

    public static class Builder {
        private final RequestContext context;

        private Builder() {
            context = new RequestContext();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder message(RemoteMessage message) {
            context.message = message;
            return this;
        }

        public Builder direction(Direction direction) {
            context.direction = direction;
            return this;
        }

        public RequestContext build() {
            return context;
        }

    }
}
