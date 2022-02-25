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
        public SignUrlRequestDto(String path) {
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
