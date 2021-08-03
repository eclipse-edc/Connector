package org.eclipse.edc.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

/**
 *
 */
@JsonDeserialize(builder = QueryRequest.Builder.class)
public class QueryRequest implements RemoteMessage {
    private String protocol;
    private String connectorId;
    private String connectorAddress;
    private String queryLanguage;
    private String query;

    @Override
    public String getProtocol() {
        return protocol;
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public String getQuery() {
        return query;
    }

    private QueryRequest() {
    }

    public String getConnectorId() {
       return connectorId;
    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private QueryRequest queryRequest;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            queryRequest.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            queryRequest.connectorId = connectorId;
            return this;
        }

        public Builder connectorAddress(String connectorAddress) {
            queryRequest.connectorAddress = connectorAddress;
            return this;
        }

        public Builder queryLanguage(String language) {
            queryRequest.queryLanguage = language;
            return this;
        }

        public Builder query(String query) {
            queryRequest.query = query;
            return this;
        }

        public QueryRequest build() {
            Objects.requireNonNull(queryRequest.protocol, "protocol");
            Objects.requireNonNull(queryRequest.connectorId, "connectorId");
            Objects.requireNonNull(queryRequest.queryLanguage, "queryLanguage");
            Objects.requireNonNull(queryRequest.query, "query");
            Objects.requireNonNull(queryRequest.connectorAddress, "connectorAddress");
            return queryRequest;
        }

        private Builder() {
            queryRequest = new QueryRequest();
        }
    }
}
