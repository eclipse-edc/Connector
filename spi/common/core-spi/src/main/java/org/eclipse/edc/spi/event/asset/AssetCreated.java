/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - expending Event classes
 *
 */

package org.eclipse.edc.spi.event.asset;

/**
 * Describe a new Asset creation, after this has emitted, an Asset with a certain id will be available.
 */
public class AssetCreated extends AssetEvent<AssetCreated.Payload> {

    private AssetCreated() {
    }

    /**
     * This class contains all event specific attributes of an Asset Creation Event
     *
     */
    public static class Payload extends AssetEvent.Payload {

    }

    public static class Builder extends AssetEvent.Builder<AssetCreated, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new AssetCreated(), new Payload());
        }

    }

}
