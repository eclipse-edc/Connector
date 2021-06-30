/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

function nextForState(state, limit, connectorId) {
    var context = getContext();
    var collection = context.getCollection();
    var collectionLink = collection.getSelfLink();
    var response = context.getResponse();


    // first query
    var filterQuery = {
        'query': 'SELECT * FROM TransferProcessDocuments t WHERE t.state = @state AND (t.lease = null OR t.lease.leasedBy = @leaser) ORDER BY t.stateTimestamp OFFSET 0 LIMIT @limit',
        'parameters': [
            {
                'name': '@state', 'value': parseInt(state, 10)
            },
            {
                'name': '@limit', 'value': parseInt(limit, 10)
            },
            {
                'name': '@leaser', 'value': connectorId
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
            leaseDuration: 60
        };

        var accept = collection.replaceDocument(document._self, document, function (err, itemReplaced) {
            if (err) throw "Unable to update Document, abort ";
        })
        if (!accept) throw "Unable to update Document, abort";
        console.log("updated lease of document " + document.id)
    }
}