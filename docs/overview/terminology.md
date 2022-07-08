# Terminology

| Name                         | Description                                         |
|:-----------------------------|:---                                                 |
| **Artifact**                 | The instance of code which can be modulized used by the EDC
| **Asset**                    | An **asset** describes **data** that can add value to a **dataspace** and contains the **DataAddress**
| **Asset Index**              | **Asset Index**<p>* manages assets<p>* provided by an extension<p>* may support external catalogs<p>* can be queried 
| **Broker**                   | see **IDS Broker**
| **Catalog**                  | **Catalog** is a directory about all meta-information about the assets a **data consumer** can reach in the **dataspace**
| **Configuration**            | The EDC allows using a configuration file which will be loaded at every startup of an EDC instance
| **Connector**                | Component to realize the communication between two participants within a **dataspace**
| **Contract**                 | **Contract** is the finalized meta-information for a data transfer after a **contract negotiation** including the policies who have to be followed
| **Contract Managment**       | Includes all parts of **Contract Offer**, **Contract Agreement** and **Contract Negotiation**
| **Contract Agreement**       | **Contract Agreement**<p>* points to a **Contract Offer**<p>* results from a **Contract Negotiation Process**<p>* has a start date and may have a expiry date and a cancellation date
| **Contract Negotiation**     | * MVP: only possible to accept already offered contracts. Counter offers are rejected automatically.
| **Contract Offer**           | **Contract Offer**<p>* set of obligations and permissions<p>* generated on the fly on provider side (see **Contract Offer Framework**)<p>* are immutable<p>* persisted in **Contract Negotiation Process** once the negotiation has started<p>
| **Contract Offer Framework** | **Contract Offer Framework**<p>* generates **Contract Offer Templates**<p>* provided by **Extensions**<p>* may be implemented in custom extensions to created contract offers based on existing systems
| **Contract Offer Template**  | Blueprint of a **Contract Offer**
| **Consumer**                 | see **Data Consumer**
| **Consumer Pull**            | The data exchange of data is initilazied by the Consumer
| **Data**                     | **Data** describes the properties of business objects
| **Data Consumer**            | **Data Consumer** are receiver of data in a data transfer.
| **Data Provider**            | **Data Providers** produce data and are the source for data in a data transfer
| **DataAddress**              | Reference to the physical Address of an **Asset**
| **DataFlow**                 | Transfer of **data** from **Data Producer** to **Data Consumer**.
| **Dataspace**                | A **dataspace** is a space where different participants exchange **data** to get an benifit of this
| **EDC Extension**            | A Module piece of the EDC which can be customized used for the EDC
| **EndpointDataReference**    | Describes an endpoint at which one receives data
| **Event**                    | An **Event** is a action which is be observed in the EDC and published by the **Event Framework**
| **Event Framework**          | **The Event Framework** is a way to react to actions in the EDC and can also be extended
| **Event Router**             | **Event Router** is the central part of the **Event Framework**, which collects all published **Events**
| **Extension**                | see **EDC Extension**
| **Federated Catalog**        | A decentralized **catalog**
| **Framework**                | see **Contract Offer Framework**
| **Health**                   | Infrastructure Status of the EDC
| **HTTPDataAddress**          | Specific type of **DataAddress** in context of HTTP-endpoints
| **Identity Provider**        | Management of the correct identification of all participants in the **dataspace**.
| **Injection**                | **Artifacts**/**EDC Extensions** can be injected in different parts of the EDC to be used 
| **Offer**                    | see **Contract Offer**
| **Offer Framework**          | see **Contract Offer Framework**
| **Monitor**                  | The **Monitor** is an interface to realise different ways to put out logs and debug information
| **Policy**                   | logical collection of rules
| **PolicyArchive**            | Location to get **Policies** by their ID
| **PolicyDefinitionStore**    | Storing all **Policy Definitions** in the EDC
| **PolicyEngine**             | Enforcing of Policies in the EDC
| **Provider**                 | The participant in an dataspace who is producing and publish sth. See **Data Provider**
| **Resource**                 | Infrastructure Component for data transmission
| **Resource Manifest**        | Collection of **Resources**
| **Rule**                     | **Rules**<p>* bound to a **Contract Offer**, **Contract Agreement** or **Contract Offer Framework**<p>* exist independent from an **Asset**
| **Transfer Process**         | **Transfer Process**<p>* based on a **Contract Agreement**
| **Transfer Process Store**   | Storing all informations about handled **Transfer Processes**
| **TypeManager**              | Way to make serialization possible in different contexts for the EDC
| **UsageControl**             | **Usage Control** is the part of the EDC that is responsible for ensuring that the **policies** for the **data** specified in the **contract** are adhered to.
| **Vault**                    | Providing secrets