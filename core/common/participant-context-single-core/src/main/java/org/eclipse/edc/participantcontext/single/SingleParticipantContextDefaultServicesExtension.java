/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

import org.eclipse.edc.participantcontext.single.config.store.SingleParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.spi.system.ServiceExtensionContext.ANONYMOUS_PARTICIPANT;

@Extension(value = SingleParticipantContextDefaultServicesExtension.NAME)
public class SingleParticipantContextDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Single Participant Context Default Services Extension";

    @Setting(description = "Configures the participant id this runtime is operating on behalf of", key = "edc.participant.id", defaultValue = ANONYMOUS_PARTICIPANT)
    public String participantId;

    @Setting(description = "Configures the participant context id for the single participant runtime", key = "edc.participant.context.id", required = false)
    public String participantContextId;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }


    @Provider(isDefault = true)
    public SingleParticipantContextSupplier participantContextSupplier() {
        var contextId = participantContextId != null ? participantContextId : participantId;
        var participantContext = new ParticipantContext(contextId);
        return () -> ServiceResult.success(participantContext);
    }


    @Provider
    public ParticipantContextConfigStore participantContextConfigStore(ServiceExtensionContext context) {
        var contextId = participantContextId != null ? participantContextId : participantId;

        var cfg = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId(contextId)
                .entries(context.getConfig().getEntries())
                .build();
        return new SingleParticipantContextConfigStore(cfg);
    }

    // by default, resolve to the configured participant id for every protocol
    @Provider(isDefault = true)
    public ParticipantIdentityResolver participantIdentityResolver() {
        return (context, protocol) -> participantId;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (ANONYMOUS_PARTICIPANT.equals(participantContextId)) {
            monitor.warning("The runtime is configured as an anonymous participant. DO NOT DO THIS IN PRODUCTION.");
        }
        if (participantContextId == null) {
            monitor.warning("The runtime is not configured with a participant context id. Using the participant id as the context id.");
        }
    }

}
