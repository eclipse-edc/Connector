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

package org.eclipse.edc.web.spi.configuration.context;

import java.net.URI;

/**
 * Provides the Management Api URL exposed, useful for setting callbacks.
 */
@FunctionalInterface
public interface ManagementApiUrl {

    /**
     * URI on which the Management API is exposed
     *
     * @return Management API URI.
     */
    URI get();
}
