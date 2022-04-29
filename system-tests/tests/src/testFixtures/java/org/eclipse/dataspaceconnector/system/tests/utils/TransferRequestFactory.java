package org.eclipse.dataspaceconnector.system.tests.utils;

import java.util.function.Function;

/**
 * Pluggable definition for {@link org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation}
 * implementations to define the kind of transfer to be performed.
 */
public interface TransferRequestFactory extends Function<TransferSimulationUtils.TransferInitiationData, String> {
}
