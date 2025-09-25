/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.spi.event;

import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.List;

/**
 * Define callback addresses capability
 */
public interface CallbackAddresses {

    /**
     * Return callback addresses
     *
     * @return callback addresses.
     */
    List<CallbackAddress> getCallbackAddresses();

}
