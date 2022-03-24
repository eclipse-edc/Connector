/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.dataplane.selector.instance;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.Polymorphic;

import java.net.URL;
import java.util.Map;

/**
 * Representations of a data plane instance. Every DPF has an ID and a URL as well as a number, how often it was selected,
 * and a timestamp of its last selection time. In addition, there are extensible properties to hold specific properties.
 */
public interface DataPlaneInstance extends Polymorphic {

    /**
     * Gets the unique identifier for this instance
     */
    String getId();

    /**
     * Determines whether a particular DPF instance can handle the given request.
     *
     * @param sourceAddress      The location where the data is located
     * @param destinationAddress The destination address of the data
     */
    boolean canHandle(DataAddress sourceAddress, DataAddress destinationAddress);

    /**
     * returns the url (host+path) to the DataPlane's control API
     * Note that this API is not intended to be public and may require authentication.
     */
    URL getUrl();

    /**
     * Indicates how often one particular DPF instance has been selected
     */
    int getTurnCount();

    /**
     * The last time a particular instance was selected in POSIX time. If it has never been selected before, this returns 0.
     */
    long getLastActive();

    /**
     * A list of extensible properties, for example this could contain a DPF's public-facing API.
     */
    Map<String, Object> getProperties();
}
