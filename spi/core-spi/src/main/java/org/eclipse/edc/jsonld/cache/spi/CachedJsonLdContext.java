/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.cache.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.Entity;

import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * A cached JSON-LD context document. It maps a context {@code url} to its (raw JSON) {@code content} and
 * records the {@link PullStrategy} that governs when the document is (re)fetched from its {@code url}.
 */
@JsonDeserialize(builder = CachedJsonLdContext.Builder.class)
public class CachedJsonLdContext extends Entity {

    public static final String CACHED_JSON_LD_CONTEXT_TYPE_TERM = "CachedJsonLdContext";
    public static final String CACHED_JSON_LD_CONTEXT_TYPE_IRI = EDC_NAMESPACE + CACHED_JSON_LD_CONTEXT_TYPE_TERM;
    public static final String CACHED_JSON_LD_CONTEXT_URL_TERM = "url";
    public static final String CACHED_JSON_LD_CONTEXT_URL_IRI = EDC_NAMESPACE + CACHED_JSON_LD_CONTEXT_URL_TERM;
    public static final String CACHED_JSON_LD_CONTEXT_CONTENT_TERM = "content";
    public static final String CACHED_JSON_LD_CONTEXT_CONTENT_IRI = EDC_NAMESPACE + CACHED_JSON_LD_CONTEXT_CONTENT_TERM;
    public static final String CACHED_JSON_LD_CONTEXT_PULL_STRATEGY_TERM = "pullStrategy";
    public static final String CACHED_JSON_LD_CONTEXT_PULL_STRATEGY_IRI = EDC_NAMESPACE + CACHED_JSON_LD_CONTEXT_PULL_STRATEGY_TERM;
    public static final String CACHED_JSON_LD_CONTEXT_UPDATED_AT_TERM = "updatedAt";
    public static final String CACHED_JSON_LD_CONTEXT_UPDATED_AT_IRI = EDC_NAMESPACE + CACHED_JSON_LD_CONTEXT_UPDATED_AT_TERM;

    private String url;
    private String content;
    private PullStrategy pullStrategy;
    private long updatedAt;

    private CachedJsonLdContext() {
    }

    public String getUrl() {
        return url;
    }

    public String getContent() {
        return content;
    }

    public PullStrategy getPullStrategy() {
        return pullStrategy;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Builder toBuilder() {
        return Builder.newInstance()
                .id(id)
                .url(url)
                .content(content)
                .pullStrategy(pullStrategy)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends Entity.Builder<CachedJsonLdContext, Builder> {

        private Builder() {
            super(new CachedJsonLdContext());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder url(String url) {
            entity.url = url;
            return this;
        }

        public Builder content(String content) {
            entity.content = content;
            return this;
        }

        public Builder pullStrategy(PullStrategy pullStrategy) {
            entity.pullStrategy = pullStrategy;
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            entity.updatedAt = updatedAt;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public CachedJsonLdContext build() {
            super.build();
            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(entity.url, "url cannot be null");
            if (entity.pullStrategy == null) {
                entity.pullStrategy = entity.content != null ? PullStrategy.NEVER : PullStrategy.IF_NOT_PRESENT;
            }
            if (entity.updatedAt == 0) {
                entity.updatedAt = entity.createdAt;
            }
            return entity;
        }
    }
}
