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

import org.eclipse.edc.protocol.dsp.dispatcher.DspRemoteMessageDispatcherImpl;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspRemoteMessageDispatcher;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = DspCoreExtension.NAME)
@Provides({DspRemoteMessageDispatcher.class})
public class DspCoreExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol Core Extension";
    
    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    
    @Inject
    private EdcHttpClient httpClient;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var dispatcher = new DspRemoteMessageDispatcherImpl(httpClient);
        dispatcherRegistry.register(dispatcher);
        context.registerService(DspRemoteMessageDispatcher.class, dispatcher);
    }
    
}
