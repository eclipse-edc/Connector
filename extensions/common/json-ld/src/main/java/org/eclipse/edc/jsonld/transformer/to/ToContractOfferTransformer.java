package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class ToContractOfferTransformer extends AbstractJsonLdTransformer<JsonObject, ContractOffer> {

    public ToContractOfferTransformer() {
        super(JsonObject.class, ContractOffer.class);
    }

    @Override
    public @Nullable ContractOffer transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        return null;
    }
}
