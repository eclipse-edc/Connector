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


package org.eclipse.dataspaceconnector.spi.types.domain.dataplane;

public interface DataPlaneConstants {
    String PUBLIC_API_AUTH_HEADER = "Authorization";

    /**
     * The Data Plane public API takes as input a signed bearer token which essentially contains as claims:
     * - the contract id
     * - an encrypted data address that will be used as data source by the Data Plane.
     */
    String DATA_ADDRESS = "dad";
    String CONTRACT_ID = "cid";
}
