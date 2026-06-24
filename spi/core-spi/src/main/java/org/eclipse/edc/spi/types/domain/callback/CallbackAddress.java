/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.callback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * The {@link CallbackAddress} contains information about users configured callbacks
 * that can be invoked in various state of the requests processing according to the filter provided
 * in {@link CallbackAddress#events}
 */
@JsonDeserialize(builder = CallbackAddress.Builder.class)
public class CallbackAddress {
    public static final String CALLBACKADDRESS_TYPE = EDC_NAMESPACE + "CallbackAddress";
    public static final String IS_TRANSACTIONAL = EDC_NAMESPACE + "transactional";
    public static final String URI = EDC_NAMESPACE + "uri";
    public static final String EVENTS = EDC_NAMESPACE + "events";

    public static final String AUTH_KEY = EDC_NAMESPACE + "authKey";
    public static final String AUTH_CODE_ID = EDC_NAMESPACE + "authCodeId";

    private String uri;
    private Set<String> events = new HashSet<>();
    private boolean transactional;

    private String authKey;
    private String authCodeId;


    public Set<String> getEvents() {
        return events;
    }

    public String getUri() {
        return uri;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public String getAuthCodeId() {
        return authCodeId;
    }

    public String getAuthKey() {
        return authKey;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {


        private final CallbackAddress callbackAddress;

        protected Builder() {
            callbackAddress = new CallbackAddress();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder uri(String url) {
            callbackAddress.uri = url;
            return this;
        }

        public Builder events(Set<String> events) {
            callbackAddress.events = events;
            return this;
        }

        public Builder transactional(boolean transactional) {
            callbackAddress.transactional = transactional;
            return this;
        }

        public Builder authKey(String authKey) {
            callbackAddress.authKey = authKey;
            return this;
        }

        public Builder authCodeId(String authCodeId) {
            callbackAddress.authCodeId = authCodeId;
            return this;
        }


        public CallbackAddress build() {
            Objects.requireNonNull(callbackAddress.uri, "URI should not be null");
            Objects.requireNonNull(callbackAddress.events, "Events should not be null");
            return callbackAddress;
        }
    }

}
