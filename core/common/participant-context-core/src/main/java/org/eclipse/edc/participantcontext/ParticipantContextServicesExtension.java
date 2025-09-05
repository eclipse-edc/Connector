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

package org.eclipse.edc.participantcontext;

import org.eclipse.edc.participantcontext.service.ParticipantContextServiceImpl;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Extension(value = ParticipantContextServicesExtension.NAME)
public class ParticipantContextServicesExtension implements ServiceExtension {

    public static final String NAME = "Participant Context Default Services Extension";

    @Inject
    private ParticipantContextStore participantContextStore;
    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }
    
    @Provider
    public ParticipantContextService participantContextService() {
        return new ParticipantContextServiceImpl(participantContextStore, transactionContext);
    }
}
