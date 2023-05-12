/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.mapper;

/**
 * Mapper for mapping Exceptions to their corresponding status code.
 */

public interface DspHttpStatusCodeMapper {

    /**
     * Returns the status code of an Exception.
     *
     * @param exception input Exception
     * @return corresponding status code
     */
    int mapErrorToStatusCode(Exception exception);
}
