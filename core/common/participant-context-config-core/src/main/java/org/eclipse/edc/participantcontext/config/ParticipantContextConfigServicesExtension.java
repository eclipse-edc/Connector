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

import org.eclipse.edc.encryption.EncryptionAlgorithmRegistry;
import org.eclipse.edc.participantcontext.config.service.ParticipantContextConfigServiceImpl;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.participantcontext.config.ParticipantContextConfigServicesExtension.NAME;

@Extension(NAME)
public class ParticipantContextConfigServicesExtension implements ServiceExtension {

    public static final String NAME = "Participant Context Config Services Extension";

    @Setting(
            description = "The encryption algorithm used for encrypting and decrypting sensitive config.",
            key = "edc.participants.config.encryption",
            defaultValue = "aes"
    )
    private String encryptionAlgorithm;

    @Inject
    private ParticipantContextConfigStore configStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private EncryptionAlgorithmRegistry encryptionRegistry;

    @Inject
    private Monitor monitor;

    @Provider
    public ParticipantContextConfigService participantContextConfigService() {
        return new ParticipantContextConfigServiceImpl(encryptionRegistry, encryptionAlgorithm, configStore, transactionContext);
    }

    @Provider
    public ParticipantContextConfig participantContextConfig() {
        return new ParticipantContextConfigImpl(encryptionRegistry, encryptionAlgorithm, configStore, transactionContext);
    }

}
