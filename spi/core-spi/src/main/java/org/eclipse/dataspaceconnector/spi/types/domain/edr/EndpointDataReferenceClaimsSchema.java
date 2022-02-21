/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */


package org.eclipse.dataspaceconnector.spi.types.domain.edr;

/**
 * {@link EndpointDataReference} is composed of a security token that embeds the target data address and the contract id as claims.
 * This class provides the claim name for these entities.
 */
public interface EndpointDataReferenceClaimsSchema {
    String DATA_ADDRESS_CLAIM = "dad";
    String CONTRACT_ID_CLAM = "cid";
    String EXPIRATION_DATE_CLAIM = "exp";
}
