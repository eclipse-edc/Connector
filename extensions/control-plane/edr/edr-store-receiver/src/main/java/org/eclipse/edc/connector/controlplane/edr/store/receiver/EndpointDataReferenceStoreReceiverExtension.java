/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.edr.store.receiver;

import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.connector.controlplane.edr.store.receiver.EndpointDataReferenceStoreReceiverExtension.NAME;

@Extension(NAME)
public class EndpointDataReferenceStoreReceiverExtension implements ServiceExtension {

    public static final String NAME = "Endpoint Data Reference Store Receiver Extension";
    private static final String DEFAULT_SYNC_LISTENER = "false";
    @Setting(description = "If true the EDR receiver will be registered as synchronous listener", defaultValue = DEFAULT_SYNC_LISTENER, key = "edc.edr.receiver.sync")
    private boolean isSyncMode;

    @Inject
    private EventRouter router;

    @Inject
    private EndpointDataReferenceStore dataReferenceStore;

    @Inject
    private Monitor monitor;

    @Inject
    private ContractAgreementService agreementService;

    @Inject
    private PolicyArchive policyArchive;

    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var receiver = new EndpointDataReferenceStoreReceiver(dataReferenceStore, policyArchive, agreementService, transactionContext, monitor.withPrefix("EDR Receiver"));
        if (isSyncMode) {
            router.registerSync(TransferProcessEvent.class, receiver);
        } else {
            router.register(TransferProcessEvent.class, receiver);
        }
    }
}
