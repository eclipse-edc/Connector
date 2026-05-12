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

package org.eclipse.edc.participantcontext.connector;

import org.eclipse.edc.participantcontext.connector.identity.ParticipantContextIdentityResolverImpl;
import org.eclipse.edc.participantcontext.connector.profile.ParticipantProfileResolverImpl;
import org.eclipse.edc.participantcontext.connector.webhook.ParticipantWebhookResolverImpl;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(value = ConnectorParticipantContextExtension.NAME)
public class ConnectorParticipantContextExtension implements ServiceExtension {

    public static final String NAME = "Connector Participant Context Extension";

    @Setting(description = "When running in virtual mode, enable all the participants to use all the profiles enabled in the connector", key = "edc.dsp.profiles.enable.all", defaultValue = "false")
    private Boolean dspEnableAllProfiles;

    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private ParticipantContextConfig participantContextConfig;
    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ProtocolWebhookResolver participantWebhookResolver() {
        return new ParticipantWebhookResolverImpl(dataspaceProfileContextRegistry);
    }

    @Provider
    public ParticipantProfileResolver participantProfileResolver() {
        return new ParticipantProfileResolverImpl(participantContextConfig, dataspaceProfileContextRegistry, dspEnableAllProfiles);
    }

    @Provider(isDefault = true)
    public ParticipantIdentityResolver participantIdentityResolver() {
        return new ParticipantContextIdentityResolverImpl(participantContextService, monitor);
    }
}
