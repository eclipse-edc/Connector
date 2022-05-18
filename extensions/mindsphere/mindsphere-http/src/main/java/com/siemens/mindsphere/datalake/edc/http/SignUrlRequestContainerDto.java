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

import java.util.Collection;
import java.util.List;

public class SignUrlRequestContainerDto {
    private Collection<SignUrlRequestDto> paths;

    public SignUrlRequestContainerDto(Collection<SignUrlRequestDto> paths) {
        this.paths = paths;
    }

    public Collection<SignUrlRequestDto> getPaths() {
        return paths;
    }

    static class SignUrlRequestDto {
        SignUrlRequestDto(String path) {
            this.path = path;
        }

        private String path;

        public String getPath() {
            return path;
        }
    }

    public static SignUrlRequestContainerDto composeForSinglePath(String path) {
        return new SignUrlRequestContainerDto(List.of(new SignUrlRequestDto(path)));
    }
}
