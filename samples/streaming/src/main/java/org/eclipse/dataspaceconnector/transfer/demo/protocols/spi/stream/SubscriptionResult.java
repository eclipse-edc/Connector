package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream;

/**
 * The result of a subscription attempt.
 */
public class SubscriptionResult {
    private boolean success;
    private String error;
    private Subscription subscription;

    /**
     * Returns true if the subscription was successful.
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns an error if the connection was unsuccessful; otherwise null.
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the subscription handle.
     */
    public Subscription getSubscription() {
        return subscription;
    }

    public SubscriptionResult(String error) {
        success = false;
        this.error = error;
    }

    public SubscriptionResult(Subscription subscription) {
        success = true;
        this.subscription = subscription;
    }

}
