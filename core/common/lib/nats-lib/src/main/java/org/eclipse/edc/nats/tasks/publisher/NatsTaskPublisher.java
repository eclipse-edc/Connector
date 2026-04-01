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

package org.eclipse.edc.nats.tasks.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskListener;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.function.Supplier;

import static java.lang.String.format;

public class NatsTaskPublisher implements TaskListener {

    private final String subjectPrefix;
    private final Class<? extends ProcessTaskPayload> target;
    private final JetStream js;
    private final Monitor monitor;
    private final Supplier<ObjectMapper> objectMapper;


    public NatsTaskPublisher(String subjectPrefix, Class<? extends ProcessTaskPayload> target, JetStream js, Monitor monitor, Supplier<ObjectMapper> objectMapper) {
        this.subjectPrefix = subjectPrefix;
        this.target = target;
        this.js = js;
        this.monitor = monitor;
        this.objectMapper = objectMapper;
    }


    private String formatSubject(ProcessTaskPayload t) {
        return format("%s.%s.%s", subjectPrefix, t.getProcessType().toLowerCase(), t.name());
    }

    @Override
    public void created(Task task) {
        try {
            if (target.isAssignableFrom(task.getPayload().getClass())) {
                var message = objectMapper.get().writeValueAsString(task);
                js.publish(formatSubject((ProcessTaskPayload) task.getPayload()), message.getBytes());
            }
        } catch (Exception e) {
            monitor.severe("Failed to publish task created event for task id " + task.getId(), e);
            throw new EdcException(e);
        }

    }
}
