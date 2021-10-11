package org.eclipse.dataspaceconnector.catalog.spi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionManager;

@JsonDeserialize(builder = Partition.Builder.class)
public class Partition {
    private String name;
    private PartitionManager partitionManager;

    private Partition() {

    }

    public String getName() {
        return name;
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String name;
        private PartitionManager partitionManager;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder partitionManager(PartitionManager partitionManager) {
            this.partitionManager = partitionManager;
            return this;
        }

        public Partition build() {
            Partition partition = new Partition();
            partition.partitionManager = partitionManager;
            partition.name = name;
            return partition;
        }
    }
}
