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

package org.eclipse.edc.protocol.dsp.http.spi.configuration;

/**
 * Holds configuration information for the Dataspace Protocol API context. This is includes the
 * context alias used for registering resources as well as the callback address.
 */
public class DspApiConfiguration {

    private final String contextAlias;
    private final String dspCallbackAddress;

    public DspApiConfiguration(String contextAlias, String dspCallbackAddress) {
        this.contextAlias = contextAlias;
        this.dspCallbackAddress = dspCallbackAddress;
    }

    public String getContextAlias() {
        return contextAlias;
    }

    public String getDspCallbackAddress() {
        return dspCallbackAddress;
    }

}
