/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.spi.dataaddress;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Validate a DataAddress
 */
public interface DataAddressValidator {

    /**
     * Validate a DataAddress
     *
     * @param dataAddress the {@link DataAddress} to be validated.
     * @return Successful {@link Result} if the {@link DataAddress} is valid, failure otherwise.
     */
    Result<DataAddress> validate(DataAddress dataAddress);

}
