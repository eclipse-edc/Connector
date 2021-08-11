package org.eclipse.spi;

import java.util.List;

public interface ContractOfferService {

    /*
     * Bonus implementation:
     * - pagination
     * - async. messages
     */

    /*
     * - get offer templates from the frameworks
     * - use expression to query assets in the index
     * - filter assets based on the client
     */
    List<ContractOffer> getMatchingOffers(ClientInformation clientInformation);

}
