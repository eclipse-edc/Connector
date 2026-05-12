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

package org.eclipse.edc.protocol.dsp.http.dispatcher;

import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1;

/**
 * Registers the message dispatcher for DSP v2025/1.
 */
public class DspHttpDispatcherV2025Extension implements ServiceExtension {

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    @Inject
    private DspHttpRemoteMessageDispatcher dispatcher;

    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        dataspaceProfileContextRegistry.addRegistrationCallback((profile) -> {
            if (profile.protocolVersion().equals(V_2025_1)) {
                dispatcherRegistry.register(profile.name(), dispatcher);
            }
        });
    }
}
