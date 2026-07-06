/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.discovery;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = DiscoveryDefaultServicesExtension.NAME)
public class DiscoveryDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Discovery Default Services";

    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private ParticipantProfileService participantProfileService;
    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DiscoveryService discoveryService() {
        return new DiscoveryServiceImpl(httpClient, participantProfileService, typeManager.getMapper());
    }
}
