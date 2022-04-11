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

package org.eclipse.dataspaceconnector.iam.did.hub;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@OpenAPIDefinition
public interface IdentityHubApi {

    Response writeCommit(Map<String, String> credential);

    String write(String jwe);

    String queryCommits(String jwe);

    String queryObjects(String jwe);
}
