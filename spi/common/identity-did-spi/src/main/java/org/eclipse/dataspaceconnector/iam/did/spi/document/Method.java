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

package org.eclipse.dataspaceconnector.iam.did.spi.document;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Method {
    private boolean published;
    private String recoveryCommitment;
    private String updateCommitment;

    public Method() {
        // needed for json deserialization
    }

    @JsonProperty("published")
    public boolean getPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    @JsonProperty("recoveryCommitment")
    public String getRecoveryCommitment() {
        return recoveryCommitment;
    }

    public void setRecoveryCommitment(String recoveryCommitment) {
        this.recoveryCommitment = recoveryCommitment;
    }

    @JsonProperty("updateCommitment")
    public String getUpdateCommitment() {
        return updateCommitment;
    }

    public void setUpdateCommitment(String updateCommitment) {
        this.updateCommitment = updateCommitment;
    }
}
