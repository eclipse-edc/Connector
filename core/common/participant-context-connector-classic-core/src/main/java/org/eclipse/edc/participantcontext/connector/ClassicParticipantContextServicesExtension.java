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

import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.spi.system.ServiceExtensionContext.ANONYMOUS_PARTICIPANT;

@Extension(value = ClassicParticipantContextServicesExtension.NAME)
public class ClassicParticipantContextServicesExtension implements ServiceExtension {

    public static final String NAME = "Classic Participant Context Services Extension";

    @Setting(description = "Configures the participant id this runtime is operating on behalf of", key = "edc.participant.id", defaultValue = ANONYMOUS_PARTICIPANT)
    public String participantId;

    @Setting(description = "Configures the participant context id for the single participant runtime", key = "edc.participant.context.id", required = false)
    public String participantContextId;

    @Inject
    private Monitor monitor;

    @Inject
    private ParticipantContextStore participantContextStore;

    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void prepare() {
        initializeParticipantContext();
    }

    private void initializeParticipantContext() {
        var contextId = participantContextId != null ? participantContextId : participantId;
        var participantContext = ParticipantContext.Builder.newInstance().participantContextId(contextId)
                .identity(participantId).build();

        if (participantContextStore.findById(contextId).failed()) {
            participantContextStore.create(participantContext)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        } else {
            participantContextStore.update(participantContext)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        }
    }
}
