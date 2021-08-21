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

/**
 *
 */
public class CommitHeader {
    @JsonProperty("rev")
    private String rev;
    @JsonProperty("iss")
    private String iss;

    public String getRev() {
        return rev;
    }

    public String getIss() {
        return iss;
    }

    void setRev(String rev) {
        this.rev = rev;
    }

    public CommitHeader(@JsonProperty("iss") String iss) {
        this.rev = rev;
        this.iss = iss;
    }
}
