package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Method {
    boolean published;
    String recoveryCommitment;
    String updateCommitment;

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
