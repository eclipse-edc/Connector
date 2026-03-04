/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.crawler.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a job in a crawl run. Every {@link TargetNode} is converted into a {@code WorkItem}, which is then fed to a
 * {@code Crawler} alongside a {@link CrawlerAction}.
 * The {@code Crawler} takes the {@link WorkItem} and executes the {@link CrawlerAction} against it.
 */
public class WorkItem {
    private final String id;
    private final String url;
    private final String protocol;
    private final List<String> errors;

    public WorkItem(String id, String url, String protocol) {
        this.id = id;
        this.url = url;
        this.protocol = protocol;
        errors = new ArrayList<>();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUrl() {
        return url;
    }

    public String getId() {
        return id;
    }

    public void error(String message) {
        errors.add(message);
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "WorkItem{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", protocol='" + protocol + '\'' +
                ", errors=" + errors +
                '}';
    }
}
