/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.edr;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Set;

/**
 * Registry that manages endpoint data reference services
 */
public interface EndpointDataReferenceServiceRegistry {

    /**
     * Register a service
     *
     * @param addressType the address type.
     * @param edrService the service.
     */
    void register(String addressType, EndpointDataReferenceService edrService);

    /**
     * Register a service for response channel
     *
     * @param addressType the address type.
     * @param edrService the service.
     */
    void registerResponseChannel(String addressType, EndpointDataReferenceService edrService);

    /**
     * Create an EDR.
     *
     * @param dataFlow the data flow.
     * @param address the address.
     * @return the EDR if successful, failure otherwise.
     */
    ServiceResult<DataAddress> create(DataFlow dataFlow, DataAddress address);


    /**
     * Create an EDR for response channel.
     *
     * @param dataFlow the data flow.
     * @param address the address.
     * @return the EDR for response channel if successful, failure otherwise.
     */
    ServiceResult<DataAddress> createResponseChannel(DataFlow dataFlow, DataAddress address);

    /**
     * Revoke the EDR.
     *
     * @param dataFlow the data flow.
     * @param reason the reason.
     * @return the result.
     */
    ServiceResult<Void> revoke(DataFlow dataFlow, String reason);

    /**
     * Return the supported destination types.
     *
     * @return the supported types
     */
    Set<String> supportedDestinationTypes();

    /**
     * Return the types of return channels supported
     *
     * @return the supported types
     */
    Set<String> supportedResponseTypes();

}
