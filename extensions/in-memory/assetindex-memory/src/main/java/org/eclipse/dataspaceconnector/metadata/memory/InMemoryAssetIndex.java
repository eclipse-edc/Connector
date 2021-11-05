/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexQuery;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexResult;
import org.eclipse.dataspaceconnector.spi.pagination.Cursor;
import org.eclipse.dataspaceconnector.spi.pagination.StringCursor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * An ephemeral asset index.
 */
class InMemoryAssetIndex implements AssetIndex {
    private final AssetStorage assetStorage;
    private final CriterionToPredicateConverter predicateFactory;

    public InMemoryAssetIndex(@NotNull AssetStorage assetStorage, @NotNull CriterionToPredicateConverter predicateFactory) {
        this.assetStorage = Objects.requireNonNull(assetStorage);
        this.predicateFactory = Objects.requireNonNull(predicateFactory);
    }

    @Override
    public AssetIndexResult queryAssets(AssetIndexQuery query) {
        Objects.requireNonNull(query, "AssetSelectorExpression can not be null!");

        // return nothing if expression is empty
        var expression = query.getExpression();
        if (expression.getCriteria().isEmpty()) {
            return AssetIndexResult.Builder.newInstance().expression(expression).build();
        }

        // find matching assets
        var matchingAssets = new ArrayList<Asset>();
        var storageIterator = getStorageIterator(query.getNextCursor());
        var rootPredicate = expression.getCriteria().stream().map(predicateFactory::convert).reduce(x -> true, Predicate::and);
        while (storageIterator.hasNext() && matchingAssets.size() < query.getLimit()) {
            var asset = storageIterator.next();
            if (rootPredicate.test(asset)) {
                matchingAssets.add(asset);
            }
        }

        // create cursor
        var isIteratorEmpty = !storageIterator.hasNext();
        var cursor = isIteratorEmpty ? null : new StringCursor(lastElement(matchingAssets).getId());

        // return result
        return AssetIndexResult.Builder.newInstance()
                .assets(matchingAssets)
                .expression(expression)
                .nextCursor(cursor)
                .build();
    }

    @Override
    public Asset findById(String assetId) {
        return assetStorage.getAsset(assetId);
    }

    private Iterator<Asset> getStorageIterator(@Nullable Cursor cursor) {
        if (cursor != null && !(cursor instanceof StringCursor)) {
            throw new EdcException(String.format("not supported cursor passed to in memory asset index extension (%s)", cursor.getClass().getName()));
        }

        return cursor == null ?
                assetStorage.getAssets() :
                assetStorage.getAssetsAscending(((StringCursor) cursor).getMarker());
    }

    private static <T> T lastElement(ArrayList<T> list) {
        return list.get(list.size() - 1);
    }
}
