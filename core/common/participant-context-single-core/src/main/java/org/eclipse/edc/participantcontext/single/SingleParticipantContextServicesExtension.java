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

package org.eclipse.edc.participantcontext.single;

import org.eclipse.edc.participantcontext.single.protocol.SingleParticipantProtocolWebhookResolver;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(value = SingleParticipantContextServicesExtension.NAME)
public class SingleParticipantContextServicesExtension implements ServiceExtension {

    public static final String NAME = "Single Participant Context Services Extension";

    @Inject
    private Monitor monitor;

    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    @Override
    public String name() {
        return NAME;
    }
    
    @Provider
    public ProtocolWebhookResolver protocolWebhookResolver() {
        return new SingleParticipantProtocolWebhookResolver(dataspaceProfileContextRegistry);
    }

}
