package com.microsoft.dagx.client.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.jline.terminal.Terminal;

import java.util.List;

import static com.microsoft.dagx.client.common.Commands.subCommands;

/**
 * A context for executing client commands.
 */
public class ExecutionContext {
    private ObjectMapper objectMapper;
    private List<String> params;
    private Terminal terminal;
    private String endpointUrl;

    public RequestBody write(Object value) {
        try {
            return RequestBody.create(objectMapper.writeValueAsString(value), MediaType.get("application/json"));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ObjectMapper getMapper() {
        return objectMapper;
    }

    public List<String> getParams() {
        return params;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public ExecutionContext createSubContext() {
        ExecutionContext context = new ExecutionContext(objectMapper);
        context.terminal = terminal;
        context.endpointUrl = endpointUrl;
        context.params = subCommands(params);
        return context;
    }

    private ExecutionContext(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static class Builder {
        private ExecutionContext context;

        public static Builder newInstance(ObjectMapper objectMapper) {
            return new Builder(objectMapper);
        }

        public Builder params(List<String> params) {
            context.params = params;
            return this;
        }

        public Builder terminal(Terminal terminal) {
            context.terminal = terminal;
            return this;
        }

        public Builder endpointUrl(String url) {
            context.endpointUrl = url;
            return this;
        }

        public ExecutionContext build() {
            return context;
        }

        private Builder(ObjectMapper objectMapper) {
            context = new ExecutionContext(objectMapper);
        }
    }
}
