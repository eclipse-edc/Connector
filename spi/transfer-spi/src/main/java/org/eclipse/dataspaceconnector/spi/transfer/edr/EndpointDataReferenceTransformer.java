package org.eclipse.dataspaceconnector.spi.transfer.edr;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EndpointDataReferenceTransformer {
    Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr);
}
