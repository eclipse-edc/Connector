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

import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
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

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }


    @Provider(isDefault = true)
    public SingleParticipantContextSupplier participantContextSupplier() {
        var participantContext = new ParticipantContext(participantId);
        return () -> ServiceResult.success(participantContext);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (ANONYMOUS_PARTICIPANT.equals(participantId)) {
            monitor.warning("The runtime is configured as an anonymous participant. DO NOT DO THIS IN PRODUCTION.");
        }
    }

}
