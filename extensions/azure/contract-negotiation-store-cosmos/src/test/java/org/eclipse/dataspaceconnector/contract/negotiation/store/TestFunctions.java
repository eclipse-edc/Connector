package org.eclipse.dataspaceconnector.contract.negotiation.store;

import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public class TestFunctions {

    public static ContractNegotiation generateNegotiation() {
        return generateNegotiation(ContractNegotiationStates.UNSAVED);
    }

    public static ContractNegotiation generateNegotiation(ContractNegotiationStates state) {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .counterPartyId("test-counterparty-1")
                .counterPartyAddress("test-counterparty-address")
                .protocol("test-protocol")
                .stateCount(1)
                .contractAgreement(ContractAgreement.Builder.newInstance().id("1")
                        .providerAgentId(URI.create("provider"))
                        .consumerAgentId(URI.create("consumer"))
                        .asset(Asset.Builder.newInstance().build())
                        .policy(Policy.Builder.newInstance().build())
                        .contractSigningDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(LocalDate.MIN.toEpochDay()), ZoneId.of("UTC")))
                        .contractStartDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(LocalDate.MIN.toEpochDay()), ZoneId.of("UTC")))
                        .contractEndDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(LocalDate.MAX.toEpochDay()), ZoneId.of("UTC")))
                        .id("1:2").build())
                .state(state.code())
                .build();
    }

    public static ContractNegotiationDocument generateDocument() {
        return new ContractNegotiationDocument(generateNegotiation());
    }
}
