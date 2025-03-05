package com.example.sqs.jms;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSSession;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;

public class JmsSqsSyncStandard {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JmsSqsSyncStandard.class);
    private static final int ACKNOWLEDGE_MODE =
            // Session.AUTO_ACKNOWLEDGE;
            Session.CLIENT_ACKNOWLEDGE;
            // SQSSession.UNORDERED_ACKNOWLEDGE;
    private static final String RECEIVE_MODE =
            "sync";
            //"async";
    private static final String QUEUE_TYPE =
            //"standard";
            "fifo";

    public static void main(String[] args) {
        // Create a new connection factory with all defaults (credentials and region) set automatically.
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                SqsClient.create()
        );
        try {
            // Create the connection.
            SQSConnection connection = connectionFactory.createConnection();

            String queueName = switch (QUEUE_TYPE) {
                case "standard" -> createStandardQueue(connection);
                case "fifo" -> createFifoQueue(connection);
                default -> throw new IllegalStateException("Unexpected value: " + QUEUE_TYPE);
            };

            Session session;
            if (RECEIVE_MODE.equals("sync")) {
                session = switch (ACKNOWLEDGE_MODE) {
                    case Session.AUTO_ACKNOWLEDGE ->  createAutoAcknowledgeSession(connection);
                    case Session.CLIENT_ACKNOWLEDGE -> createClientAcknowledgeSession(connection);
                    case SQSSession.UNORDERED_ACKNOWLEDGE -> createUnorderedAcknowledgeSession(connection);
                    default -> throw new IllegalStateException("Unexpected value: " + ACKNOWLEDGE_MODE);
                };
            } else {
                session = createAutoAcknowledgeSession(connection);
            }


            // Create a queue identity and specify the queue name to the session.
            Queue queue = session.createQueue(queueName);
            // Create a producer for the 'MyQueue'.
            MessageProducer producer = session.createProducer(queue);

            // Send message.
            switch (QUEUE_TYPE) {
                case "standard" -> sendMessageToStandardQueue(session, producer);
                case "fifo" -> sendMessageToFifoQueue(session, producer);
            }

            // Receive message
            if (RECEIVE_MODE.equals("sync")) {
                switch (QUEUE_TYPE) {
                    case "standard" -> {
                        switch (ACKNOWLEDGE_MODE) {
                            case Session.AUTO_ACKNOWLEDGE -> receiveMessageAutoAckStdQueue(connection, session, queue);
                            case Session.CLIENT_ACKNOWLEDGE -> receiveMessageExplicitAck(connection, session, queue);
                            case SQSSession.UNORDERED_ACKNOWLEDGE -> receiveMessageAutoAckAsync(connection, session, queue);
                        }
                    }
                    case "fifo" -> receiveMessageAutoAckFifoQueue(connection, session, queue);
                }
            } else {
                receiveMessageAutoAckAsync(connection, session, queue);
            }
            SqsClient sqsClient = connection.getWrappedAmazonSQSClient().getAmazonSQSClient();
            String queueUrl = connection.getWrappedAmazonSQSClient().getQueueUrl(queueName).queueUrl();

            connection.close();

            if (queueUrl != null){
                sqsClient.deleteQueue(b -> b.queueUrl(queueUrl));
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static Session createAutoAcknowledgeSession(SQSConnection connection) {
        try {
            // Create the non-transacted session with AUTO_ACKNOWLEDGE mode.
            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static Session createClientAcknowledgeSession(SQSConnection connection) {
        try {
            // Create the non-transacted session with CLIENT_ACKNOWLEDGE mode.
            return connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static Session createUnorderedAcknowledgeSession(SQSConnection connection) {
        try {
            // Create the non-transacted session with UNORDERED_ACKNOWLEDGE mode.
            return connection.createSession(false, SQSSession.UNORDERED_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static void sendMessageToStandardQueue(Session session, MessageProducer producer) {
        try {
            // Create the text message.
            TextMessage message = session.createTextMessage("Hello World!");
            // Send the message.
            producer.send(message);
            LOGGER.info("JMS Message {}", message.getJMSMessageID());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static void sendMessageToFifoQueue(Session session, MessageProducer producer) {
        try {
            // Create the text message.
            TextMessage message = session.createTextMessage("Hello World!");

            // Set the message group ID.
            message.setStringProperty("JMSXGroupID", "Default");
            /* You can also set a custom message deduplication ID
            'message.setStringProperty("JMS_SQS_DeduplicationId", "hello");'
            Here, it's not needed because content-based deduplication is enabled for the queue. */

            // Send the message.
            producer.send(message);
            LOGGER.info("JMS Message {}", message.getJMSMessageID());
            LOGGER.info("JMS Message Sequence Number {}", message.getStringProperty("JMS_SQS_SequenceNumber"));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static String createStandardQueue(SQSConnection connection){
        // Get the wrapped client.
        AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();
        // Create an SQS queue named 'MyQueue', if it doesn't already exist.
        try {
            if (!client.queueExists("MyQueue")) {
                client.createQueue("MyQueue");
            }
            return "MyQueue";
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static String createFifoQueue(SQSConnection connection) {
        // Get the wrapped client.
        AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();
        // Create an Amazon SQS FIFO queue named 'MyQueue.fifo', if it doesn't already exist.
        try {
            if (!client.queueExists("MyQueue.fifo")) {
                Map<QueueAttributeName, String> attributes = Map.of(
                        QueueAttributeName.FIFO_QUEUE, String.valueOf(true),
                        QueueAttributeName.CONTENT_BASED_DEDUPLICATION, String.valueOf(true)
                );
                client.createQueue(CreateQueueRequest.builder()
                        .queueName("MyQueue.fifo")
                        .attributes(attributes)
                        .build());
            }
            return "MyQueue.fifo";
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static void receiveMessageAutoAckStdQueue(SQSConnection connection, Session session, Queue queue) {
        Message receivedMessage;
        try {
            MessageConsumer consumer = session.createConsumer(queue);
            connection.start();
            receivedMessage = consumer.receive(1000);

            if (receivedMessage != null) {
                LOGGER.info("Received: {}", ((TextMessage) receivedMessage).getText());
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static void receiveMessageAutoAckFifoQueue(SQSConnection connection, Session session, Queue queue) {
        try {
            MessageConsumer consumer = session.createConsumer(queue);
            connection.start();
            Message receivedMessage = consumer.receive(1000);
            if (receivedMessage != null) {
                LOGGER.info("Received: {}", ((TextMessage) receivedMessage).getText());
                LOGGER.info("Group id: {}", receivedMessage.getStringProperty("JMSXGroupID"));
                LOGGER.info("Message deduplication id: {}", receivedMessage.getStringProperty("JMS_SQS_DeduplicationId"));
                LOGGER.info("Message sequence number: {}", receivedMessage.getStringProperty("JMS_SQS_SequenceNumber"));
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static void receiveMessageExplicitAck(SQSConnection connection, Session session, Queue queue) {
        Message receivedMessage;
        try {
            MessageConsumer consumer = session.createConsumer(queue);
            connection.start();
            receivedMessage = consumer.receive(1000);

            if (receivedMessage != null) {
                LOGGER.info("Received: {}", ((TextMessage) receivedMessage).getText());
                receivedMessage.acknowledge();
                LOGGER.info("Acknowledged: {}", receivedMessage.getJMSMessageID());
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }


    static void receiveMessageAutoAckAsync(SQSConnection connection, Session session, Queue queue) {
        try {
            MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(message -> {
                try {
                    // Cast the received message as TextMessage and print the text to screen.
                    LOGGER.info("Received: {}", ((TextMessage) message).getText());
                } catch (JMSException e) {
                    LOGGER.error("Error processing message: {}", e.getMessage());
                }
            });
            connection.start();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        try {
            LOGGER.info("Thread about to sleep...");
            Thread.sleep(1000);
            LOGGER.info("Thread alive again.");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
