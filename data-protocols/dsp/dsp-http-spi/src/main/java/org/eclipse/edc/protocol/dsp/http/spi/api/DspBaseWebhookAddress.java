/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.api;

/**
 * The base webhook address for the DSP API. Should be used as the base for DSP-version-specific
 * webhooks.
 */
@FunctionalInterface
public interface DspBaseWebhookAddress {
    
    /**
     * Provides the base DSP webhook address.
     *
     * @return the base DSP webhook address
     */
    String get();
}
