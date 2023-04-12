/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp;

import org.eclipse.edc.protocol.dsp.dispatcher.DspHttpRemoteMessageDispatcherImpl;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Provides an implementation of {@link DspHttpRemoteMessageDispatcher} to support sending dataspace
 * protocol messages. The dispatcher can then be used by other extensions to add support for
 * specific message types.
 */
@Extension(value = DspHttpCoreExtension.NAME)
@Provides({DspHttpRemoteMessageDispatcher.class})
public class DspHttpCoreExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol Core Extension";
    
    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private IdentityService identityService;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var dispatcher = new DspHttpRemoteMessageDispatcherImpl(httpClient, identityService);
        dispatcherRegistry.register(dispatcher);
        context.registerService(DspHttpRemoteMessageDispatcher.class, dispatcher);
    }
}
