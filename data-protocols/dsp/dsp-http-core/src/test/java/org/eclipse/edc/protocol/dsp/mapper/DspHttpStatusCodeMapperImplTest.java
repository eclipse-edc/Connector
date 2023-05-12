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

package org.eclipse.edc.protocol.dsp.mapper;

import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DspHttpStatusCodeMapperImplTest {

    private DspHttpStatusCodeMapper statusCodeMapper;

    @BeforeEach
    void setUp() {
        statusCodeMapper = new DspHttpStatusCodeMapperImpl();
    }

    @Test
    void test() {
        assertThat(statusCodeMapper.mapErrorToStatusCode(new AuthenticationFailedException())).isEqualTo(401);
        assertThat(statusCodeMapper.mapErrorToStatusCode(new NotAuthorizedException())).isEqualTo(403);
        assertThat(statusCodeMapper.mapErrorToStatusCode(new InvalidRequestException("null"))).isEqualTo(400);
        assertThat(statusCodeMapper.mapErrorToStatusCode(new ObjectNotFoundException(Object.class, null))).isEqualTo(404);
        assertThat(statusCodeMapper.mapErrorToStatusCode(new ObjectConflictException("null"))).isEqualTo(409);
        assertThat(statusCodeMapper.mapErrorToStatusCode(new BadGatewayException("null"))).isEqualTo(502);
        assertThat(statusCodeMapper.mapErrorToStatusCode(new UnsupportedOperationException())).isEqualTo(501);
        assertThat(statusCodeMapper.mapErrorToStatusCode(new Exception())).isEqualTo(500);
    }
}
