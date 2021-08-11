package org.eclipse.spi;

public abstract class ContractOfferTemplate {
    public abstract ContractOffer getTemplatedOffer();
    public abstract SelectorExpression selectorExpression();
}
