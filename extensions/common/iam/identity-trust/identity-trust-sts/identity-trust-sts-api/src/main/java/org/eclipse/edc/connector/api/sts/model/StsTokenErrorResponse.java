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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.sts.model;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * OAuth2 <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-5.2">Error Response</a>
 *
 * @param error            Error code.
 * @param errorDescription Human-readable description.
 * @param errorUri         URI of the error page.
 */
public record StsTokenErrorResponse(@JsonProperty String error,
                                    @JsonProperty("error_description") String errorDescription,
                                    @JsonProperty("error_uri") String errorUri) {

}
