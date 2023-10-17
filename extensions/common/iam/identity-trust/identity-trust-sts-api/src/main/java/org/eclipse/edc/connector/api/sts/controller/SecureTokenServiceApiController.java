/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.sts.controller;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.api.sts.SecureTokenServiceApi;
import org.eclipse.edc.connector.api.sts.model.StsTokenRequest;
import org.eclipse.edc.connector.api.sts.model.StsTokenResponse;

@Path("/")
public class SecureTokenServiceApiController implements SecureTokenServiceApi {

    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("token")
    @POST
    @Override
    public StsTokenResponse token(@BeanParam StsTokenRequest request) {
        return null;
    }
}
