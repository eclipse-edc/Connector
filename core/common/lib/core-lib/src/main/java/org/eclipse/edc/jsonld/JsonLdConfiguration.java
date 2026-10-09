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

package org.eclipse.edc.jsonld;

public class JsonLdConfiguration {

    private boolean httpEnabled = false;
    private boolean httpsEnabled = false;
    private boolean avoidVocab = false;
    private boolean checkPrefixes = true;

    private JsonLdConfiguration() {

    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public boolean shouldCheckPrefixes() {
        return checkPrefixes;
    }

    public boolean isAvoidVocab() {
        return avoidVocab;
    }

    public static class Builder {

        private final JsonLdConfiguration configuration = new JsonLdConfiguration();

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder httpEnabled(boolean httpEnabled) {
            configuration.httpEnabled = httpEnabled;
            return this;
        }

        public Builder httpsEnabled(boolean httpsEnabled) {
            configuration.httpsEnabled = httpsEnabled;
            return this;
        }

        public Builder avoidVocab(boolean avoidVocab) {
            configuration.avoidVocab = avoidVocab;
            return this;
        }

        public Builder checkPrefixes(boolean checkPrefixes) {
            configuration.checkPrefixes = checkPrefixes;
            return this;
        }

        public JsonLdConfiguration build() {
            return configuration;
        }
    }
}
