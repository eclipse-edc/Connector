/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.iam;

import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Determines the public endpoint at which the data for a particular data transfer is exposed.
 * For example, for HTTP transfers this would likely return the internet-facing HTTP URL of the data plane ("public url").
 */
public interface PublicEndpointGeneratorService {

    /**
     * Generates an endpoint for a destination type and a particular {@link DataAddress}
     *
     * @param destinationType   the destination type
     * @param sourceDataAddress the source data address.
     * @return The public {@link Endpoint} where the data is made available, or a failure if the endpoint could not be generated.
     */
    Result<Endpoint> generateFor(String destinationType, DataAddress sourceDataAddress);

    /**
     * Generates an endpoint for a response channel
     *
     * @param responseChannelType the response channel type
     * @return The public {@link Endpoint} where the data is made available, or a failure if the endpoint could not be generated.
     */
    Result<Endpoint> generateResponseFor(String responseChannelType);

    /**
     * Adds a function that can generate a {@link Endpoint} for particular source data address. Typically, the source data address
     * is <strong>not</strong> directly exposed publicly.
     *
     * @param destinationType   The type of the source {@link DataAddress}
     * @param generatorFunction the generator function
     */
    void addGeneratorFunction(String destinationType, Function<DataAddress, Endpoint> generatorFunction);

    /**
     * Adds a function that can generate a response channel {@link Endpoint}.
     *
     * @param responseChannelType The type of the source {@link DataAddress}
     * @param generatorFunction   the generator function
     */
    void addResponseGeneratorFunction(String responseChannelType, Supplier<Endpoint> generatorFunction);

    /**
     * Return the supported destination types.
     *
     * @return the supported types
     */
    Set<String> supportedDestinationTypes();

    /**
     * Return the types of return channels supported by this generator
     *
     * @return the supported types
     */
    Set<String> supportedResponseTypes();
}
