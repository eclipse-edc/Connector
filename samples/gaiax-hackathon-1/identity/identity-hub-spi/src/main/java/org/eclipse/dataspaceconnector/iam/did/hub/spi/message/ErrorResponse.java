/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.hub.spi.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 *
 */
@JsonTypeName("ErrorResponse")
public class ErrorResponse extends HubMessage {
    private String errorCode;
    private String developerMessage;

    @JsonProperty("error_code")
    public String getErrorCode() {
        return errorCode;
    }


    @JsonProperty("developer_message")
    public String getDeveloperMessage() {
        return developerMessage;
    }

    public ErrorResponse(@JsonProperty("error_code") String errorCode, @JsonProperty("developer_message") String developerMessage) {
        this.errorCode = errorCode;
        this.developerMessage = developerMessage;
    }
}
