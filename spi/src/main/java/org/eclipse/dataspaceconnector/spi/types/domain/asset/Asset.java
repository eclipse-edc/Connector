/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.asset;

import java.util.Map;

// TODO maybe create simple class hierarchy for the asset types (file, api, etc.)
// Alternatively use composition over inheritance?

/**
 * The {@link Asset} contains the metadata and describes the data itself or a collection of data.
 */
public class Asset {
    private String id;
    private String title;
    private String description;
    private String version;
    private String fileName;
    private Integer byteSize;
    private String fileExtension;
    private Map<String, String> labels;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getByteSize() {
        return byteSize;
    }

    // TODO in theory each asset may have multiple representations/media types, depending on the transfer capabilities of the EDC
    public String getFileExtension() {
        return fileExtension;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public static final class Builder {
        private String id;
        private String title;
        private String description;
        private String version;
        private String fileName;
        private Integer byteSize;
        private String fileExtension;
        private Map<String, String> labels;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(final String id) {
            this.id = id;
            return this;
        }


        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder version(final String version) {
            this.version = version;
            return this;
        }

        public Builder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder byteSize(final Integer byteSize) {
            this.byteSize = byteSize;
            return this;
        }

        public Builder fileExtension(final String mediaType) {
            this.fileExtension = mediaType;
            return this;
        }

        public Builder labels(final Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public Asset build() {
            Asset asset = new Asset();
            asset.id = this.id;
            asset.title = this.title;
            asset.description = this.description;
            asset.version = this.version;
            asset.fileName = this.fileName;
            asset.byteSize = this.byteSize;
            asset.fileExtension = this.fileExtension;
            asset.labels = this.labels;
            return asset;
        }
    }
}