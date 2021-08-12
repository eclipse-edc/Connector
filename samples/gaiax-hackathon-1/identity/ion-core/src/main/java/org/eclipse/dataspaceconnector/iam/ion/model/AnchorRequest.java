package org.eclipse.dataspaceconnector.iam.ion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = AnchorRequest.Builder.class)
public class AnchorRequest {

    private final IonRequest requestBody;
    private String challengeEndpoint;
    private String solutionEndpoint;

    public AnchorRequest(IonRequest requestbody) {
        requestBody = requestbody;
    }

    public IonRequest getRequestBody() {
        return requestBody;
    }

    public String getChallengeEndpoint() {
        return challengeEndpoint;
    }

    public String getSolutionEndpoint() {
        return solutionEndpoint;
    }


    @JsonPOJOBuilder
    public static final class Builder {
        private final static String defaultChallengeEndpoint = "https://beta.ion.msidentity.com/api/v1.0/proof-of-work-challenge";
        private final static String defaultSolutionEndpoint = "https://beta.ion.msidentity.com/api/v1.0/operations";
        private IonRequest requestBody;
        private String challengeEndpoint;
        private String solutionEndpoint;

        private Builder() {
        }

        @JsonCreator
        public static Builder create() {
            return new Builder();
        }

        public Builder requestBody(IonRequest requestbody) {
            requestBody = requestbody;
            return this;
        }

        public Builder challengeEndpoint(String challengeEndpoint) {
            this.challengeEndpoint = challengeEndpoint;
            return this;
        }

        public Builder solutionEndpoint(String solutionEndpoint) {
            this.solutionEndpoint = solutionEndpoint;
            return this;
        }

        public AnchorRequest build() {
            Objects.requireNonNull(requestBody, "RequestBody cannot be null");
            AnchorRequest anchorRequest = new AnchorRequest(requestBody);

            // either set both or none, otherwise use defaults
            if (challengeEndpoint == null || solutionEndpoint == null) {
                challengeEndpoint = defaultChallengeEndpoint;
                solutionEndpoint = defaultSolutionEndpoint;
            }

            anchorRequest.solutionEndpoint = solutionEndpoint;
            anchorRequest.challengeEndpoint = challengeEndpoint;
            return anchorRequest;
        }
    }
}
