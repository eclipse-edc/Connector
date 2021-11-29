package org.eclipse.dataspaceconnector.spi.types.domain.catalog;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * DTO representing IDS Data Catalog
 */
@JsonDeserialize(builder = Catalog.Builder.class)
public class Catalog {
    private final String id;
    private final List<ContractOffer> contractOffers;

    private Catalog(@NotNull String id, @NotNull List<ContractOffer> contractOffers) {
        this.id = Objects.requireNonNull(id);
        this.contractOffers = Objects.requireNonNull(contractOffers);
    }

    public String getId() {
        return id;
    }

    public List<ContractOffer> getContractOffers() {
        return contractOffers;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String id;
        private List<ContractOffer> contractOffers;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder contractOffers(List<ContractOffer> contractOffers) {
            this.contractOffers = contractOffers;
            return this;
        }

        public Catalog build() {
            return new Catalog(id, contractOffers);
        }

    }
}
