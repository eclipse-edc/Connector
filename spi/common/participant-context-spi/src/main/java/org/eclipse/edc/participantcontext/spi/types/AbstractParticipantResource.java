/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.spi.types;

import org.eclipse.edc.spi.entity.Entity;

public abstract class AbstractParticipantResource extends Entity implements ParticipantResource {

    protected String participantContextId;


    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }
    
    protected abstract static class Builder<T extends AbstractParticipantResource, B extends AbstractParticipantResource.Builder<T, B>> extends Entity.Builder<T, B> {

        protected Builder(T entity) {
            super(entity);
        }

        public B participantContextId(String participantContextId) {
            entity.participantContextId = participantContextId;
            return self();
        }

        @Override
        protected T build() {
            super.build();
            return entity;
        }
    }
}