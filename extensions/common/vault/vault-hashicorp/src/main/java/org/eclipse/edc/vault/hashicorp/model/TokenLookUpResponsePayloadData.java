package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenLookUpResponsePayloadData {

    private static final String ROOT_POLICY = "root";

    @JsonProperty("explicit_max_ttl")
    private int explicitMaxTimeToLive;

    @JsonProperty("issue_time")
    private String issueTime;

    @JsonProperty("ttl")
    private int timeToLive;

    @JsonProperty("renewable")
    private boolean isRenewable;

    @JsonProperty("period")
    private Integer period;

    @JsonProperty("policies")
    private List<String> policies;

    public int getExplicitMaxTimeToLive() {
        return explicitMaxTimeToLive;
    }

    public boolean hasExplicitMaxTimeToLive() {
        return explicitMaxTimeToLive > 0;
    }

    public String getIssueTime() {
        return issueTime;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public boolean isRenewable() {
        return isRenewable;
    }

    public Integer getPeriod() {
        return period;
    }

    public boolean isPeriodicToken() {
        return period != null;
    }

    public boolean isRootToken() {
        return policies.contains(ROOT_POLICY);
    }
}
