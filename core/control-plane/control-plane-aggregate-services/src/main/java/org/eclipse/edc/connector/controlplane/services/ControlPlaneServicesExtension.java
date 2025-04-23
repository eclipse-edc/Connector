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
 *       Cofinity-X - unauthenticated DSP version endpoint
 *
 */

package org.eclipse.edc.connector.controlplane.services;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.asset.spi.observe.AssetObservableImpl;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.definition.observe.ContractDefinitionObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.ConsumerOfferResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.policy.spi.observe.PolicyDefinitionObservableImpl;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.services.asset.AssetEventListener;
import org.eclipse.edc.connector.controlplane.services.asset.AssetQueryValidator;
import org.eclipse.edc.connector.controlplane.services.asset.AssetServiceImpl;
import org.eclipse.edc.connector.controlplane.services.catalog.CatalogProtocolServiceImpl;
import org.eclipse.edc.connector.controlplane.services.catalog.CatalogServiceImpl;
import org.eclipse.edc.connector.controlplane.services.contractagreement.ContractAgreementServiceImpl;
import org.eclipse.edc.connector.controlplane.services.contractdefinition.ContractDefinitionEventListener;
import org.eclipse.edc.connector.controlplane.services.contractdefinition.ContractDefinitionServiceImpl;
import org.eclipse.edc.connector.controlplane.services.contractnegotiation.ContractNegotiationProtocolServiceImpl;
import org.eclipse.edc.connector.controlplane.services.contractnegotiation.ContractNegotiationServiceImpl;
import org.eclipse.edc.connector.controlplane.services.policydefinition.PolicyDefinitionEventListener;
import org.eclipse.edc.connector.controlplane.services.policydefinition.PolicyDefinitionServiceImpl;
import org.eclipse.edc.connector.controlplane.services.protocol.ProtocolTokenValidatorImpl;
import org.eclipse.edc.connector.controlplane.services.protocol.VersionProtocolServiceImpl;
import org.eclipse.edc.connector.controlplane.services.protocol.VersionServiceImpl;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidators;
import org.eclipse.edc.connector.controlplane.services.secret.SecretEventListener;
import org.eclipse.edc.connector.controlplane.services.secret.SecretServiceImpl;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.services.transferprocess.TransferProcessProtocolServiceImpl;
import org.eclipse.edc.connector.controlplane.services.transferprocess.TransferProcessServiceImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.secret.spi.observe.SecretObservableImpl;
import org.eclipse.edc.connector.spi.service.SecretService;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestVersionPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

import java.time.Clock;

import static org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext.CATALOGING_REQUEST_SCOPE;
import static org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext.CONTRACT_NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.edc.policy.context.request.spi.RequestVersionPolicyContext.VERSION_REQUEST_SCOPE;

@Extension(ControlPlaneServicesExtension.NAME)
public class ControlPlaneServicesExtension implements ServiceExtension {

    public static final String NAME = "Control Plane Services";

    @Setting(description = "If true enables the policy validation when creating and updating policy definitions", defaultValue = "false", key = "edc.policy.validation.enabled")
    private Boolean validatePolicy;

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
    private Vault vault;
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
    private ConsumerOfferResolver consumerOfferResolver;

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

    @Inject
    private CommandHandlerRegistry commandHandlerRegistry;

    @Inject
    private DataAddressValidatorRegistry dataAddressValidator;

    @Inject
    private IdentityService identityService;

    @Inject
    private PolicyEngine policyEngine;

    @Inject(required = false)
    private ProtocolTokenValidator protocolTokenValidator;

    @Inject
    private ProtocolVersionRegistry protocolVersionRegistry;

    @Inject
    private TransferTypeParser transferTypeParser;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        policyEngine.registerScope(TRANSFER_PROCESS_REQUEST_SCOPE, RequestTransferProcessPolicyContext.class);
        policyEngine.registerScope(CONTRACT_NEGOTIATION_REQUEST_SCOPE, RequestContractNegotiationPolicyContext.class);
        policyEngine.registerScope(CATALOGING_REQUEST_SCOPE, RequestCatalogPolicyContext.class);
        policyEngine.registerScope(VERSION_REQUEST_SCOPE, RequestVersionPolicyContext.class);
    }

    @Provider
    public AssetService assetService() {
        var assetObservable = new AssetObservableImpl();
        assetObservable.registerListener(new AssetEventListener(clock, eventRouter));
        return new AssetServiceImpl(assetIndex, contractNegotiationStore, transactionContext, assetObservable,
                dataAddressValidator, new AssetQueryValidator());
    }

    @Provider
    public SecretService secretService() {
        var secretObservable = new SecretObservableImpl();
        secretObservable.registerListener(new SecretEventListener(clock, eventRouter));
        return new SecretServiceImpl(vault, secretObservable);
    }

    @Provider
    public CatalogService catalogService() {
        return new CatalogServiceImpl(dispatcher);
    }

    @Provider
    public CatalogProtocolService catalogProtocolService(ServiceExtensionContext context) {
        return new CatalogProtocolServiceImpl(datasetResolver, dataServiceRegistry,
                protocolTokenValidator(), context.getParticipantId(), transactionContext);
    }

    @Provider
    public ContractAgreementService contractAgreementService() {
        return new ContractAgreementServiceImpl(contractNegotiationStore, transactionContext, QueryValidators.contractAgreement());
    }

    @Provider
    public ContractDefinitionService contractDefinitionService() {
        var contractDefinitionObservable = new ContractDefinitionObservableImpl();
        contractDefinitionObservable.registerListener(new ContractDefinitionEventListener(clock, eventRouter));
        return new ContractDefinitionServiceImpl(contractDefinitionStore, transactionContext, contractDefinitionObservable, QueryValidators.contractDefinition());
    }

    @Provider
    public ContractNegotiationService contractNegotiationService() {
        return new ContractNegotiationServiceImpl(contractNegotiationStore, consumerContractNegotiationManager,
                transactionContext, commandHandlerRegistry, QueryValidators.contractNegotiation());
    }

    @Provider
    public ContractNegotiationProtocolService contractNegotiationProtocolService() {
        return new ContractNegotiationProtocolServiceImpl(contractNegotiationStore,
                transactionContext, contractValidationService, consumerOfferResolver, protocolTokenValidator(), contractNegotiationObservable,
                monitor, telemetry);
    }

    @Provider
    public PolicyDefinitionService policyDefinitionService() {
        var policyDefinitionObservable = new PolicyDefinitionObservableImpl();
        policyDefinitionObservable.registerListener(new PolicyDefinitionEventListener(clock, eventRouter));
        return new PolicyDefinitionServiceImpl(transactionContext, policyDefinitionStore, contractDefinitionStore,
                policyDefinitionObservable, policyEngine, QueryValidators.policyDefinition(), validatePolicy);
    }

    @Provider
    public TransferProcessService transferProcessService() {
        return new TransferProcessServiceImpl(transferProcessStore, transferProcessManager, transactionContext,
                dataAddressValidator, commandHandlerRegistry, transferTypeParser, contractNegotiationStore,
                QueryValidators.transferProcess());
    }

    @Provider
    public TransferProcessProtocolService transferProcessProtocolService() {
        return new TransferProcessProtocolServiceImpl(transferProcessStore, transactionContext, contractNegotiationStore,
                contractValidationService, protocolTokenValidator(), dataAddressValidator, transferProcessObservable, clock,
                monitor, telemetry);
    }

    @Provider
    public ProtocolTokenValidator protocolTokenValidator() {
        if (protocolTokenValidator == null) {
            protocolTokenValidator = new ProtocolTokenValidatorImpl(identityService, policyEngine, monitor, participantAgentService);
        }
        return protocolTokenValidator;
    }

    @Provider
    public VersionProtocolService versionProtocolService() {
        return new VersionProtocolServiceImpl(protocolVersionRegistry);
    }

    @Provider
    public VersionService versionService() {
        return new VersionServiceImpl(dispatcher);
    }

}
