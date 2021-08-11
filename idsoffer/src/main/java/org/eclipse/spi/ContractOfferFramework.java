package org.eclipse.spi;

import java.util.List;

public interface ContractOfferFramework {
    /*
     * Note:
     * There is only one implementation for this
     */
    List<ContractOfferTemplate> queryTemplates(ContractOfferFrameworkQuery query);

}
