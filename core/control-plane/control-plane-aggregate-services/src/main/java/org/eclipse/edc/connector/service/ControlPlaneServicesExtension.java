/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.service;

import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionObservableImpl;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.observe.PolicyDefinitionObservableImpl;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.service.asset.AssetEventListener;
import org.eclipse.edc.connector.service.asset.AssetServiceImpl;
import org.eclipse.edc.connector.service.catalog.CatalogProtocolServiceImpl;
import org.eclipse.edc.connector.service.catalog.CatalogServiceImpl;
import org.eclipse.edc.connector.service.contractagreement.ContractAgreementServiceImpl;
import org.eclipse.edc.connector.service.contractdefinition.ContractDefinitionEventListener;
import org.eclipse.edc.connector.service.contractdefinition.ContractDefinitionServiceImpl;
import org.eclipse.edc.connector.service.contractnegotiation.ContractNegotiationProtocolServiceImpl;
import org.eclipse.edc.connector.service.contractnegotiation.ContractNegotiationServiceImpl;
import org.eclipse.edc.connector.service.dataaddress.DataAddressValidatorImpl;
import org.eclipse.edc.connector.service.policydefinition.PolicyDefinitionEventListener;
import org.eclipse.edc.connector.service.policydefinition.PolicyDefinitionServiceImpl;
import org.eclipse.edc.connector.service.transferprocess.TransferProcessProtocolServiceImpl;
import org.eclipse.edc.connector.service.transferprocess.TransferProcessServiceImpl;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.observe.asset.AssetObservableImpl;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

@Extension(ControlPlaneServicesExtension.NAME)
public class ControlPlaneServicesExtension implements ServiceExtension {

    public static final String NAME = "Control Plane Services";

    private final DataAddressValidator dataAddressValidator = new DataAddressValidatorImpl();

    @Inject
    private Clock clock;

    @Inject
    private Monitor monitor;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcher;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private ConsumerContractNegotiationManager consumerContractNegotiationManager;

    @Inject
    private PolicyDefinitionStore policyDefinitionStore;

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private TransferProcessManager transferProcessManager;

    @Inject
    private TransactionContext transactionContext;
    
    @Inject
    private ContractValidationService contractValidationService;

    @Inject
    private ContractNegotiationObservable contractNegotiationObservable;

    @Inject
    private TransferProcessObservable transferProcessObservable;

    @Inject
    private Telemetry telemetry;

    @Inject
    private ParticipantAgentService participantAgentService;

    @Inject
    private DataServiceRegistry dataServiceRegistry;

    @Inject
    private DatasetResolver datasetResolver;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public AssetService assetService() {
        var assetObservable = new AssetObservableImpl();
        assetObservable.registerListener(new AssetEventListener(clock, eventRouter));
        return new AssetServiceImpl(assetIndex, contractNegotiationStore, transactionContext, assetObservable, dataAddressValidator);
    }

    @Provider
    public CatalogService catalogService() {
        return new CatalogServiceImpl(dispatcher);
    }

    @Provider
    public CatalogProtocolService catalogProtocolService() {
        return new CatalogProtocolServiceImpl(datasetResolver, participantAgentService, dataServiceRegistry);
    }

    @Provider
    public ContractAgreementService contractAgreementService() {
        return new ContractAgreementServiceImpl(contractNegotiationStore, transactionContext);
    }

    @Provider
    public ContractDefinitionService contractDefinitionService() {
        var contractDefinitionObservable = new ContractDefinitionObservableImpl();
        contractDefinitionObservable.registerListener(new ContractDefinitionEventListener(clock, eventRouter));
        return new ContractDefinitionServiceImpl(contractDefinitionStore, transactionContext, contractDefinitionObservable);
    }

    @Provider
    public ContractNegotiationService contractNegotiationService() {
        return new ContractNegotiationServiceImpl(contractNegotiationStore, consumerContractNegotiationManager, transactionContext);
    }

    @Provider
    public ContractNegotiationProtocolService contractNegotiationProtocolService() {
        return new ContractNegotiationProtocolServiceImpl(contractNegotiationStore,
                transactionContext, contractValidationService, contractNegotiationObservable,
                monitor, telemetry);
    }

    @Provider
    public PolicyDefinitionService policyDefinitionService() {
        var policyDefinitionObservable = new PolicyDefinitionObservableImpl();
        policyDefinitionObservable.registerListener(new PolicyDefinitionEventListener(clock, eventRouter));
        return new PolicyDefinitionServiceImpl(transactionContext, policyDefinitionStore, contractDefinitionStore, policyDefinitionObservable);
    }

    @Provider
    public TransferProcessService transferProcessService() {
        return new TransferProcessServiceImpl(transferProcessStore, transferProcessManager, transactionContext,
                dataAddressValidator);
    }

    @Provider
    public TransferProcessProtocolService transferProcessProtocolService() {
        return new TransferProcessProtocolServiceImpl(transferProcessStore, transactionContext, contractNegotiationStore,
                contractValidationService, dataAddressValidator, transferProcessObservable, clock, monitor, telemetry);
    }
}
