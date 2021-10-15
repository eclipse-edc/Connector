package org.eclipse.dataspaceconnector.contract;

import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFrameworkQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferTemplate;

import java.util.stream.Stream;

/**
 * NullObject of the {@link ContractOfferFramework}
 */
public class NullContractOfferFramework implements ContractOfferFramework {

    @Override
    public Stream<ContractOfferTemplate> queryTemplates(ContractOfferFrameworkQuery query) {
        return Stream.empty();
    }
}
