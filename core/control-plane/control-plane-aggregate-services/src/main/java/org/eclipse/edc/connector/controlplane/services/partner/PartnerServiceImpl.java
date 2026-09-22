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

package org.eclipse.edc.connector.controlplane.services.partner;

import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore.PARTNER_NOT_FOUND;

public class PartnerServiceImpl implements PartnerService {

    private final PartnerStore partnerStore;
    private final PartnerGroupStore partnerGroupStore;
    private final TransactionContext transactionContext;
    private final QueryValidator queryValidator;

    public PartnerServiceImpl(PartnerStore partnerStore, PartnerGroupStore partnerGroupStore,
                              TransactionContext transactionContext, QueryValidator queryValidator) {
        this.partnerStore = partnerStore;
        this.partnerGroupStore = partnerGroupStore;
        this.transactionContext = transactionContext;
        this.queryValidator = queryValidator;
    }

    @Override
    public ServiceResult<Partner> findById(String participantContextId, String id) {
        return transactionContext.execute(() -> {
            var partner = partnerStore.findById(participantContextId, id);
            return partner == null
                    ? ServiceResult.notFound(format(PARTNER_NOT_FOUND, id, participantContextId))
                    : ServiceResult.success(partner);
        });
    }

    @Override
    public ServiceResult<Partner> findByIdentity(String participantContextId, String identity) {
        return transactionContext.execute(() -> {
            var partner = partnerStore.findByIdentity(participantContextId, identity);
            return partner == null
                    ? ServiceResult.notFound(format("Partner with identity %s could not be found in participant context %s", identity, participantContextId))
                    : ServiceResult.success(partner);
        });
    }

    @Override
    public ServiceResult<List<Partner>> search(QuerySpec query) {
        return queryValidator.validate(query)
                .flatMap(validation -> validation.failed()
                        ? ServiceResult.badRequest(format("Error validating schema: %s", validation.getFailureDetail()))
                        : ServiceResult.success(transactionContext.execute(() -> partnerStore.query(query))));
    }

    @Override
    public ServiceResult<Partner> create(Partner partner) {
        return transactionContext.execute(() -> validate(partner)
                .compose(v -> ServiceResult.from(partnerStore.create(partner)).map(it -> partner)));
    }

    @Override
    public ServiceResult<Partner> update(Partner partner) {
        return transactionContext.execute(() -> validate(partner)
                .compose(v -> ServiceResult.from(partnerStore.update(partner)).map(it -> partner)));
    }

    @Override
    public ServiceResult<Partner> delete(String participantContextId, String id) {
        return transactionContext.execute(() -> {
            var partner = partnerStore.findById(participantContextId, id);
            if (partner == null) {
                return ServiceResult.notFound(format(PARTNER_NOT_FOUND, id, participantContextId));
            }
            return ServiceResult.from(partnerStore.deleteById(participantContextId, id)).map(it -> partner);
        });
    }

    private ServiceResult<Void> validate(Partner partner) {
        if (partner.getIdentity().isBlank()) {
            return ServiceResult.badRequest("Partner identity must not be blank");
        }
        if (partner.getParticipantContextId() == null) {
            return ServiceResult.badRequest("Partner must belong to a participant context");
        }
        var unknownGroups = partner.getGroupIds().stream()
                .filter(groupId -> partnerGroupStore.findById(partner.getParticipantContextId(), groupId) == null)
                .sorted()
                .toList();
        if (!unknownGroups.isEmpty()) {
            return ServiceResult.badRequest(format("Partner groups %s do not exist in participant context %s", unknownGroups, partner.getParticipantContextId()));
        }
        return ServiceResult.success();
    }
}
