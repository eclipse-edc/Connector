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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@JsonTypeName("CommitQueryResponse")
@JsonDeserialize(builder = CommitQueryResponse.Builder.class)
public class CommitQueryResponse extends HubMessage {
    private String developerMessage;
    private String skipToken;

    private List<String> commits = new ArrayList<>();

    private CommitQueryResponse() {
    }

    @JsonProperty("developer_message")
    public String getDeveloperMessage() {
        return developerMessage;
    }

    public List<String> getCommits() {
        return commits;
    }

    public String getSkipToken() {
        return skipToken;
    }

    public static class Builder extends HubMessage.Builder {
        private CommitQueryResponse response;

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("developer_message")
        public Builder developerMessage(String message) {
            this.response.developerMessage = message;
            return this;
        }

        public Builder commits(List<String> commits) {
            this.response.commits.addAll(commits);
            return this;
        }

        public Builder commit(String commit) {
            this.response.commits.add(commit);
            return this;
        }

        public Builder skipToken(String skipToken) {
            this.response.skipToken = skipToken;
            return this;
        }

        public CommitQueryResponse build() {
            return response;
        }

        private Builder() {
            response = new CommitQueryResponse();
        }

    }
}
