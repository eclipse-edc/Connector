package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.contract.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.participant.ParticipantAgent;

import java.util.stream.Stream;

/**
 * An implementation that provides no contract definitions. Intended for use in runtimes that do not need to support contract offers.
 */
public class NullContractOfferFramework implements ContractOfferFramework {

    @Override
    public Stream<ContractDefinition> definitionsFor(ParticipantAgent agent) {
        return Stream.empty();
    }
}
