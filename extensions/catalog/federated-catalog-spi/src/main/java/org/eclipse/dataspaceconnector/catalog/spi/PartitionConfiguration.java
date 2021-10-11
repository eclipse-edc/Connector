package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.Partition;

import java.util.Collection;

public interface PartitionConfiguration {
    Collection<Partition> getPartitions();
}
