/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

/**
 * Returns the next batch of documents that are in a certain state and acquires an exclusive lock on it (a "lease").
 * This SPROC will only return items that are not yet leased, or where the lease has expired. Thus, two subsequent calls with identical parameters
 * will yield different results.
 *
 * @param state the desired state
 * @param limit the batch size
 * @param connectorId The name of the calling runtime
 */

function nextForState(state, limit, connectorId) {
    var context = getContext();
    var collection = context.getCollection();
    var collectionLink = collection.getSelfLink();
    var response = context.getResponse();


    // first query
    var filterQuery = {
        'query': 'SELECT * FROM t WHERE t.wrappedInstance.state = @state AND (t.lease = null OR (t.lease.leasedAt + t.lease.leaseDuration) < @now) ORDER BY t.wrappedInstance.stateTimestamp OFFSET 0 LIMIT @limit',
        'parameters': [
            {
                'name': '@state', 'value': parseInt(state, 10)
            },
            {
                'name': '@limit', 'value': parseInt(limit, 10)
            },
            {
                'name': '@leaser', 'value': connectorId
            },
            {
                'name': '@now', 'value': Date.now()
            }
        ]
    };

    var accept = collection.queryDocuments(collectionLink, filterQuery, {}, function (err, items, responseOptions) {
        if (err) throw new Error("Error" + err.message);


        if (!items || !items.length || items.length <= 0) {
            response.setBody('no docs found')
            console.log("No documents found!")
        }

        console.log("found " + items.length + " documents!")

        // add lock to all items
        for (var i = 0; i < items.length; i++) {
            lease(items[i], connectorId)
        }
        response.setBody(items)
    });

    if (!accept) throw "Unable to read document details, abort ";

    function lease(document, connectorId) {
        document.lease = {
            leasedBy: connectorId,
            leasedAt: Date.now(),
            leaseDuration: 60000
        };

        var accept = collection.replaceDocument(document._self, document, function (err, itemReplaced) {
            if (err) throw "Unable to update Document, abort ";
        })
        if (!accept) throw "Unable to update Document, abort";
        console.log("updated lease of document " + document.id)
    }
}