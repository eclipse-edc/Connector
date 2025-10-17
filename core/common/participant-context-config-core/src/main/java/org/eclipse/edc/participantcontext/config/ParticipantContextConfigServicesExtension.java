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

package org.eclipse.edc.participantcontext.config;

import org.eclipse.edc.participantcontext.config.service.ParticipantContextConfigServiceImpl;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.participantcontext.config.defaults.ParticipantContextConfigDefaultServicesExtension.NAME;

@Extension(NAME)
public class ParticipantContextConfigServicesExtension implements ServiceExtension {

    public static final String NAME = "Participant Context Config Services Extension";

    @Inject
    private ParticipantContextConfigStore configStore;

    @Inject
    private TransactionContext transactionContext;

    @Provider
    public ParticipantContextConfigService participantContextConfigService() {
        return new ParticipantContextConfigServiceImpl(configStore, transactionContext);
    }

    @Provider
    public ParticipantContextConfig participantContextConfig() {
        return new ParticipantContextConfigImpl(configStore, transactionContext);
    }

}
