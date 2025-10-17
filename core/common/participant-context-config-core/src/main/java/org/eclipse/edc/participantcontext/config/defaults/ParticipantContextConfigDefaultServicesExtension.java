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

package org.eclipse.edc.participantcontext.config.defaults;

import org.eclipse.edc.participantcontext.config.defaults.store.InMemoryParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.participantcontext.config.defaults.ParticipantContextConfigDefaultServicesExtension.NAME;

@Extension(NAME)
public class ParticipantContextConfigDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Participant Context Config Default Services Extension";
    
    @Provider(isDefault = true)
    public ParticipantContextConfigStore participantContextConfigStore() {
        return new InMemoryParticipantContextConfigStore();
    }
}
