/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.system.utils;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;

import java.time.Duration;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;

/**
 * Orchestrate a data transfer between the consumer and the provider and returns the transfer process id.
 */
public class TransferTestRunner implements UnaryOperator<String> {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final Participant consumer;
    private final Participant provider;
    private final JsonObject destination;
    private final JsonObject privateProperties;
    private final TransferProcessStates expectedState;

    public TransferTestRunner(Participant consumer, Participant provider, JsonObject destination, JsonObject privateProperties, TransferProcessStates expectedState) {
        this.consumer = consumer;
        this.provider = provider;
        this.destination = destination;
        this.privateProperties = privateProperties;
        this.expectedState = expectedState;
    }

    /**
     * Execute the data transfer.
     *
     * @param assetId provider asset id.
     * @return transfer process id.
     */
    @Override
    public String apply(String assetId) {
        var dataset = consumer.getDatasetForAsset(provider, assetId);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();
        var contractDefinitionId = ContractId.parseId(policy.getString(ID))
                .orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));
        var contractAgreementId = consumer.negotiateContract(provider, contractDefinitionId.toString(), assetId, policy);
        var transferProcessId = consumer.initiateTransfer(provider, contractAgreementId, assetId, privateProperties, destination);
        assertThat(transferProcessId).isNotNull();

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var state = consumer.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(expectedState.name());
        });

        return transferProcessId;
    }
}
