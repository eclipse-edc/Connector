/*
 *  Copyright (c) 2021, 2022 Siemens AG
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

package com.siemens.mindsphere.datalake.edc.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SignUrlResponseContainerDto {
    private Collection<SignUrlResponseDto> objectUrls;

    public Collection<SignUrlResponseDto> getObjectUrls() {
        return Optional.ofNullable(objectUrls).orElse(List.of());
    }

    public void setObjectUrls(Collection<SignUrlResponseDto> objectUrls) {
        this.objectUrls = objectUrls;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignUrlResponseDto {
        private String signedUrl;
        private String path;

        public String getSignedUrl() {
            return signedUrl;
        }

        public void setSignedUrl(String signedUrl) {
            this.signedUrl = signedUrl;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
