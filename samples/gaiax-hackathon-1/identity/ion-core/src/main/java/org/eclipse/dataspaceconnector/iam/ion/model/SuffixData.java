package org.eclipse.dataspaceconnector.iam.ion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
public class SuffixData {
    private final String recoveryCommitment;
    private final String deltaHash;
    private String type;
    private String anchorOrigin;

    public SuffixData(@JsonProperty("recoveryCommitment") String recoveryCommitment, @JsonProperty("deltaHash") String deltaHash) {
        this.recoveryCommitment = recoveryCommitment;
        this.deltaHash = deltaHash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAnchorOrigin() {
        return anchorOrigin;
    }

    public void setAnchorOrigin(String anchorOrigin) {
        this.anchorOrigin = anchorOrigin;
    }

    public String getRecoveryCommitment() {
        return recoveryCommitment;
    }

    public String getDeltaHash() {
        return deltaHash;
    }
}
