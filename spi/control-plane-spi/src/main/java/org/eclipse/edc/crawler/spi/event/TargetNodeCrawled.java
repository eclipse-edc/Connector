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

package org.eclipse.edc.crawler.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.spi.event.Event;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Emitted every time the catalog crawler has crawled a {@link TargetNode},
 * regardless of the outcome: {@link #isSuccess()}
 * tells whether the catalog was fetched and cached, and if it was not,
 * {@link #getErrorMessage()} contains the reason.
 */
@JsonDeserialize(builder = TargetNodeCrawled.Builder.class)
public class TargetNodeCrawled extends Event {

    private String nodeId;
    private boolean success;
    private String errorMessage;

    private TargetNodeCrawled() {
    }

    @Override
    public String name() {
        return "targetnode.crawled";
    }

    /**
     * The ID of the {@link TargetNode} that was crawled.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Whether crawling the node succeeded.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * The reason why crawling the node failed, {@code null} if it succeeded.
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final TargetNodeCrawled event;

        private Builder() {
            event = new TargetNodeCrawled();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            event.nodeId = nodeId;
            return this;
        }

        public Builder success(boolean success) {
            event.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            event.errorMessage = errorMessage;
            return this;
        }

        public TargetNodeCrawled build() {
            Objects.requireNonNull(event.nodeId, "'nodeId' is missing");
            return event;
        }
    }
}
