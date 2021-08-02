package com.microsoft.dagx.transfer.demo.protocols.http;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.TopicManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A JAX-RS endpoint that accepts data to be published to a topic.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/demo/pubsub")
public class PubSubHttpEndpoint {
    private TopicManager topicManager;

    private AtomicBoolean active = new AtomicBoolean();

    private ExecutorService executorService;
    private LinkedBlockingQueue<QueueEntry> queue = new LinkedBlockingQueue<>();

    public PubSubHttpEndpoint(TopicManager topicManager) {
        this.topicManager = topicManager;
        executorService = Executors.newSingleThreadExecutor();
    }

    public void start() {
        active.set(true);
        executorService.submit(this::run);
    }

    public void stop() {
        active.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @POST
    @Path("{destinationName}")
    public Response publish(@PathParam("destinationName") String topicName, @HeaderParam("X-Authorization") String token, byte[] data) {
        var result = topicManager.connect(topicName, token);
        if (result.success()) {
            var entry = new QueueEntry(result.getConsumer(), data);
            try {
                queue.put(entry);
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new DagxException(e);
            }
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    private static class QueueEntry {
        Consumer<byte[]> connection;
        byte[] data;

        public QueueEntry(Consumer<byte[]> connection, byte[] data) {
            this.connection = connection;
            this.data = data;
        }
    }

    private void run() {
        while (active.get()) {
            var entry = queue.poll();
            if (entry != null) {
                entry.connection.accept(entry.data);
            }
        }
    }

}
