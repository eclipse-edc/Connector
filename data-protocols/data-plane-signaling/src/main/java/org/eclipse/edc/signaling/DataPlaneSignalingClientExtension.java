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

package org.eclipse.edc.signaling;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.signaling.port.DataPlaneSignalingClient;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.signaling.DataPlaneSignalingClientExtension.NAME;

@Extension(NAME)
public class DataPlaneSignalingClientExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling client";

    @Inject
    private ControlApiHttpClient httpClient;

    @Provider
    public DataPlaneClientFactory dataPlaneClientFactory() {
        return dataPlane -> new DataPlaneSignalingClient(dataPlane, httpClient);
    }
}
