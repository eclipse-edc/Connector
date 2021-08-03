/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.dataspaceconnector.catalog.atlas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AtlasErrorCode {
    @JsonProperty
    private String errorCode;

    @JsonProperty
    private String errorMessage;

    public AtlasErrorCode() {
    }

    public AtlasErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
