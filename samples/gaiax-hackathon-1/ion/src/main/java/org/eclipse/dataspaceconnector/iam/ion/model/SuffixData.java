package org.eclipse.dataspaceconnector.iam.ion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
public class SuffixData {
    private final String recoveryCommitment;
    private final String deltaHash;

    public SuffixData(@JsonProperty("recoveryCommitment") String recoveryCommitment, @JsonProperty("deltaHash") String deltaHash) {
        this.recoveryCommitment = recoveryCommitment;
        this.deltaHash = deltaHash;
    }

    public String getRecoveryCommitment() {
        return recoveryCommitment;
    }

    public String getDeltaHash() {
        return deltaHash;
    }
}
