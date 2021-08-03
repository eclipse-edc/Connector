/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.transfer.provision;

import java.util.Set;

/**
 * Selects a data destination from a set of possible targets.
 */
public interface DestinationSelectionStrategy {

    /**
     * Select a destination from the set of possible targets.
     */
    String selectDestination(Set<String> destinationTypes);

}
