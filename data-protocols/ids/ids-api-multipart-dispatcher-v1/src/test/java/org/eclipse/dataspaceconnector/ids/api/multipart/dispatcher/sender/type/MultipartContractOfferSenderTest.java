/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.dataspaceconnector.ids.core.serialization.IdsTypeManagerUtil;
import org.eclipse.dataspaceconnector.ids.core.transform.IdsTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.ids.transform.type.contract.ContractOfferToIdsContractOfferTransformer;
import org.eclipse.dataspaceconnector.ids.transform.type.policy.ActionToIdsActionTransformer;
import org.eclipse.dataspaceconnector.ids.transform.type.policy.PermissionToIdsPermissionTransformer;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartContractOfferSenderTest {
    
    private MultipartContractOfferSender sender;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        var connectorId = URI.create("https://connector");
        var webhookAddress = "https://webhook";
        
        var transformerRegistry = new IdsTransformerRegistryImpl();
        transformerRegistry.register(new ContractOfferToIdsContractOfferTransformer());
        transformerRegistry.register(new PermissionToIdsPermissionTransformer());
        transformerRegistry.register(new ActionToIdsActionTransformer());
    
        objectMapper = IdsTypeManagerUtil.getIdsObjectMapper(new TypeManager());
        
        var senderContext = new SenderDelegateContext(connectorId, objectMapper, transformerRegistry, webhookAddress);
        sender = new MultipartContractOfferSender(senderContext);
    }
    
    @Test
    void buildMessagePayload_initialRequest_mapPolicyProperties() throws Exception {
        var policy = getPolicy();
        var request = getContractOfferRequest(policy, ContractOfferRequest.Type.INITIAL);
        
        var result = sender.buildMessagePayload(request);
        
        var contractRequest = objectMapper.readValue(result, ContractRequest.class);
        assertThat(contractRequest.getProperties())
                .hasSize(2)
                .containsAllEntriesOf(policy.getExtensibleProperties());
    }
    
    @Test
    void buildMessagePayload_notInitialRequest_mapPolicyProperties() throws Exception {
        var policy = getPolicy();
        var request = getContractOfferRequest(policy, ContractOfferRequest.Type.COUNTER_OFFER);
    
        var result = sender.buildMessagePayload(request);
    
        var contractOffer = objectMapper.readValue(result, de.fraunhofer.iais.eis.ContractOffer.class);
        assertThat(contractOffer.getProperties())
                .hasSize(2)
                .containsAllEntriesOf(policy.getExtensibleProperties());
    }
    
    private ContractOfferRequest getContractOfferRequest(Policy policy, ContractOfferRequest.Type type) {
        return ContractOfferRequest.Builder.newInstance()
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id("contract-offer")
                        .policy(policy)
                        .assetId("asset-id")
                        .build())
                .protocol("protocol")
                .connectorId("connector")
                .connectorAddress("https://connector")
                .type(type)
                .build();
    }
    
    private Policy getPolicy() {
        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .build();
        
        return Policy.Builder.newInstance()
                .permission(usePermission)
                .extensibleProperty("key1", "value1")
                .extensibleProperty("key2", "value2")
                .build();
    }
}
