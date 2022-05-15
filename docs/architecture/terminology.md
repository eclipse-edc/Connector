# Terminology

| Name                                   | Description                                         |
|:---                                    |:---                                                 |
| **Artifact**                           |
| **Asset**                              | **Assets**<p>* have one content type<p>* can be a finite as a (set of) file(s) or non finite as a service, stream</br>* are the _unit of sharing_<p>* can point to one or more (physical) asset elements
| **Asset Element**                      |
| **Asset Index**                        | **Asset Index**<p>* manages assets<p>* provided by an extension<p>* may support external catalogs<p>* can be queried 
| **Broker**                             | see **IDS Broker**
| **Connector**                          |
| **Connector Directory**                |
| **Contract**                           |
| **Contract Agreement**                 | **Contract Agreement**<p>* points to a **Contract Offer**<p>* results from a **Contract Negotiation Process**<p>* has a start date and may have a expiry date and a cancellation date
| **Contract Negotiation**               | * MVP: only possible to accept already offered contracts. Counter offers are rejected automatically.
| **Contract Offer**                     | **Contract Offer**<p>* set of obligations and permissions<p>* generated on the fly on provider side (see **Contract Offer Framework**)<p>* are immutable<p>* persisted in **Contract Negotiation Process** once the negotiation has started<p>
| **Contract Offer Framework**           | **Contract Offer Framework**<p>* generates **Contract Offer Templates**<p>* provided by **Extensions**<p>* may be implemented in custom extensions to created contract offers based on existing systems
| **Contract Offer Template**            | Blueprint of a **Contract Offer**
| **Consumer**                           |
| **Data**                               |
| **Dataspace**                          |
| **EDC Extension**                      |
| **Element**                            | see **Asset Element**
| **Extension**                          | see **EDC Extension**
| **Framework**                          | see **Contract Offer Framework**
| **Identity Provider**                  |
| **IDS Broker**                         | IDS version of the **Connector Directory**
| **Offer**                              | see **Contract Offer**
| **Offer Framework**                    | see **Contract Offer Framework**
| **Policy**                             | logical collection of rules
| **Provider**                           |
| **Resource**                           |
| **Resource Manifest**                  |
| **Rule**                               | **Rules**<p>* bound to a **Contract Offer**, **Contract Agreement** or **Contract Offer Framework**<p>* exist independent from an **Asset**
| **Transfer Process**                   | **Transfer Process**<p>* based on a **Contract Agreement**