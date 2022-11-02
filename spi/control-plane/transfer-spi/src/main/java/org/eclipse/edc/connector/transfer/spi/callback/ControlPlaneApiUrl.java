/*
 *  Copyright (c) 2020, 2022 Microsoft Corporation
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


import java.net.URL;

@FunctionalInterface
public interface ControlPlaneApiUrl {

    /**
     * gets the URL which the HTTP Control Plane API provides for out-of-process systems to call back into.
     */
    URL get();
}
