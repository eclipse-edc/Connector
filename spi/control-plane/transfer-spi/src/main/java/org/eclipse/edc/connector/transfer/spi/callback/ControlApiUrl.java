/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.transfer.spi.callback;


import java.net.URI;

@FunctionalInterface
public interface ControlApiUrl {

    /**
     * gets the URI which the HTTP Control API provides for out-of-process systems to call back into.
     */
    URI get();
}
