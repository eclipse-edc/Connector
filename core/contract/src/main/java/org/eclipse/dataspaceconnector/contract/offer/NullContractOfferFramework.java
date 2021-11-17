package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferFramework;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * An implementation that provides no contract definitions. Intended for use in runtimes that do not need to support contract offers.
 */
public class NullContractOfferFramework implements ContractOfferFramework {

    @Override
    @NotNull
    public Stream<ContractDefinition> definitionsFor(ParticipantAgent agent) {
        return Stream.empty();
    }
}
