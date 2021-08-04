/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.client.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.client.runtime.EdcConnectorClientRuntime;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.jline.terminal.Terminal;

import java.util.List;

import static org.eclipse.dataspaceconnector.client.common.Commands.subCommands;

/**
 * A context for executing client commands.
 */
public class ExecutionContext {
    private EdcConnectorClientRuntime runtime;
    private List<String> params;
    private Terminal terminal;
    private String endpointUrl;

    public RequestBody write(Object value) {
        try {
            return RequestBody.create(runtime.getTypeManager().getMapper().writeValueAsString(value), MediaType.get("application/json"));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ObjectMapper getMapper() {
        return runtime.getTypeManager().getMapper();
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
        ExecutionContext context = new ExecutionContext();
        context.runtime = runtime;
        context.terminal = terminal;
        context.endpointUrl = endpointUrl;
        context.params = subCommands(params);
        return context;
    }

    /**
     * Returns a runtime service.
     */
    public <T> T getService(Class<T> type) {
        return runtime.getService(type);
    }

    private ExecutionContext() {
    }

    public static class Builder {
        private ExecutionContext context;

        public static Builder newInstance() {
            return new Builder();
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

        public Builder runtime(EdcConnectorClientRuntime runtime) {
            context.runtime = runtime;
            return this;
        }

        public ExecutionContext build() {
            return context;
        }

        private Builder() {
            context = new ExecutionContext();
        }
    }
}
