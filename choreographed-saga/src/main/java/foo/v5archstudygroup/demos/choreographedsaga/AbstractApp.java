package foo.v5archstudygroup.demos.choreographedsaga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import foo.v5archstudygroup.demos.choreographedsaga.events.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public abstract class AbstractApp {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Channel channel;
    private final long receiveEventsFromTimestamp;

    public AbstractApp(String host) throws IOException, TimeoutException {
        var connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);

        // In a real application, you would never do this without also having code for closing the connection/channel
        var connection = connectionFactory.newConnection();
        channel = connection.createChannel();
        channel.basicQos(3);

        channel.queueDeclare(Constants.ORDERS_STREAM, true, false, false, Collections.singletonMap("x-queue-type", "stream"));
        channel.queueDeclare(Constants.SHIPMENTS_STREAM, true, false, false, Collections.singletonMap("x-queue-type", "stream"));
        channel.queueDeclare(Constants.INVOICES_STREAM, true, false, false, Collections.singletonMap("x-queue-type", "stream"));

        Runtime.getRuntime().addShutdownHook(new Thread(this::writeReceiveEventsFromTimestamp));
        receiveEventsFromTimestamp = readReceiveEventsFromTimestamp();
    }

    private long readReceiveEventsFromTimestamp() {
        var file = new File(getClass().getSimpleName() + ".shutdownInfo");
        try (var is = new DataInputStream(new FileInputStream(file))) {
            return is.readLong();
        } catch (IOException ex) {
            return 0;
        }
    }

    private void writeReceiveEventsFromTimestamp() {
        try {
            var file = new File(getClass().getSimpleName() + ".shutdownInfo");
            file.createNewFile();
            try (var os = new DataOutputStream(new FileOutputStream(file))) {
                logger.info("Storing current timestamp in {}", file.getAbsolutePath());
                os.writeLong(System.currentTimeMillis());
            }
        } catch (IOException ex) {
            logger.error("Error storing the current timestamp", ex);
        }
    }

    protected void consume(String queue, Consumer<Delivery> consumer) {
        try {
            channel().basicConsume(queue, false, Collections.singletonMap("x-stream-offset", receiveEventsFromTimestamp == 0 ? "first" : new Date(receiveEventsFromTimestamp)),
                    (consumerTag, delivery) -> {
                        logger.debug("Received {}", consumerTag);
                        consumer.accept(delivery);
                        channel().basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }, consumerTag -> {
                        logger.warn("Cancelled {}", consumerTag);
                    });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected <T> void consume(String queue, Class<T> type, Consumer<T> consumer) {
        consume(queue, delivery -> consumer.accept(fromJson(delivery.getBody(), type)));
    }

    protected void publish(String queue, Object object) {
        try {
            channel().basicPublish("", queue, null, toJson(object).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected Channel channel() {
        return channel;
    }

    protected String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not convert given object to JSON", ex);
        }
    }

    protected <T> T fromJson(byte[] json, Class<T> type) {
        return fromJson(new String(json, StandardCharsets.UTF_8), type);
    }

    protected <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not convert given JSON to object", ex);
        }
    }
}
