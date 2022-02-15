package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.time.LocalDate;
import java.util.List;

public class TestFunctions {

    public static ContractNegotiation createNegotiation(String name) {
        return ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(name)
                .contractAgreement(createAgreement())
                .contractOffers(List.of(ContractOffer.Builder.newInstance().id("contractId")
                        .policy(Policy.Builder.newInstance().build()).build()))
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();
    }

    public static ContractAgreement createAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id("agreementId")
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .asset(Asset.Builder.newInstance().build())
                .policy(Policy.Builder.newInstance().build())
                .contractSigningDate(LocalDate.MIN.toEpochDay())
                .contractStartDate(LocalDate.MIN.toEpochDay())
                .contractEndDate(LocalDate.MAX.toEpochDay())
                .build();
    }
}
