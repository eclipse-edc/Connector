package org.eclipse.dataspaceconnector.samples.identity.registrationservice.events;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherAsyncClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import org.eclipse.dataspaceconnector.events.azure.AzureEventGridConfig;
import org.eclipse.dataspaceconnector.spi.security.Vault;

import java.util.Objects;

public class CrawlerEventPublisher {
    private final EventGridPublisherAsyncClient<EventGridEvent> client;

    public CrawlerEventPublisher(Vault vault) {

        var config = AzureEventGridConfig.getDefault();
        var topicName = config.getTopic();
        var endpoint = config.getEndpoint(topicName);
        client = new EventGridPublisherClientBuilder()
                .credential(new AzureKeyCredential(Objects.requireNonNull(vault.resolveSecret(topicName), "Did not find secret in vault: " + endpoint)))
                .endpoint(endpoint)
                .buildEventGridEventPublisherAsyncClient();
    }

    public void discoveryFinished(int foundItems) {
        client.sendEvent(new EventGridEvent("crawlFinised",
                        "dataspaceconnector/identity/ion/discovery",
                        BinaryData.fromObject(foundItems),
                        "0.1"))
                .subscribe();
    }

}
