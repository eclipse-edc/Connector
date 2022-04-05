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

package org.eclipse.dataspaceconnector.iam.did.spi.hub.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;


@JsonTypeName("CommitQueryResponse")
@JsonDeserialize(builder = CommitQueryResponse.Builder.class)
public class CommitQueryResponse extends HubMessage {
    private final List<Commit> commits = new ArrayList<>();
    private String developerMessage;
    private String skipToken;

    private CommitQueryResponse() {
    }

    @JsonProperty("developer_message")
    public String getDeveloperMessage() {
        return developerMessage;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public String getSkipToken() {
        return skipToken;
    }

    public static class Builder extends HubMessage.Builder {
        private final CommitQueryResponse response;

        private Builder() {
            response = new CommitQueryResponse();
        }

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("developer_message")
        public Builder developerMessage(String message) {
            response.developerMessage = message;
            return this;
        }

        public Builder commits(List<Commit> commits) {
            response.commits.addAll(commits);
            return this;
        }

        public Builder commit(Commit commit) {
            response.commits.add(commit);
            return this;
        }

        public Builder skipToken(String skipToken) {
            response.skipToken = skipToken;
            return this;
        }

        public CommitQueryResponse build() {
            return response;
        }

    }
}
