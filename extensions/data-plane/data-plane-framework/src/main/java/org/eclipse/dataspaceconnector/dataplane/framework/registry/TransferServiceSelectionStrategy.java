package org.eclipse.dataspaceconnector.dataplane.framework.registry;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Functional interface for selecting which of (potentially) multiple {@link TransferService}s to use
 * for serving a particular {@link DataFlowRequest}.
 */
public interface TransferServiceSelectionStrategy {
    /**
     * Selects which of (potentially) multiple {@link TransferService}s to use
     * for serving a particular {@link DataFlowRequest}.
     *
     * @param request          the request.
     * @param transferServices any number of services which are able to handle the request. May be an empty {@link Stream}.
     * @return the service to be used to serve the request, selected among the input {@code transferServices}, or {@code null} if the stream is empty or no service should be used.
     */
    @Nullable
    TransferService chooseTransferService(DataFlowRequest request, Stream<TransferService> transferServices);

    /**
     * Default strategy: use first matching service. This allows integrators to select
     * order preferred {@link TransferService}s in the classpath.
     *
     * @return the first service, or {@code null} if the stream is empty.
     */
    static TransferServiceSelectionStrategy selectFirst() {
        return (request, transferServices) -> transferServices.findFirst().orElse(null);
    }
}
