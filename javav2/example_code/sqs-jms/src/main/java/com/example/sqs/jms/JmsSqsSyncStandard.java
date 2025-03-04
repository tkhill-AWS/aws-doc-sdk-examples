package com.example.sqs.jms;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSSession;
import com.amazon.sqs.javamessaging.acknowledge.AcknowledgeMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;

public class JmsSqsSyncStandard {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JmsSqsSyncStandard.class);
    private static final int ACKNOWLEDGE_MODE =
            // Session.CLIENT_ACKNOWLEDGE;
            // Session.AUTO_ACKNOWLEDGE;
            SQSSession.UNORDERED_ACKNOWLEDGE;

    public static void main(String[] args) {
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                SqsClient.create()
        );
        try {
            SQSConnection connection = connectionFactory.createConnection();
            AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();
            if (!client.queueExists("MyQueue")) {
                client.createQueue("MyQueue");
            }

            Session session = connection.createSession(false, ACKNOWLEDGE_MODE);
            Queue queue = session.createQueue("MyQueue");
            MessageProducer producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage("Hello World!");
            producer.send(message);
            LOGGER.info("JMS Message {}", message.getJMSMessageID());

           receiveMessageSyncUnorderedAck(connection, session, queue);
           // receiveMessageSyncAutoAck(connection, session, queue);
           // receiveMessageSyncClientAck(connection, session, queue);
           // receiveMessageAsync(connection, session, queue);
            connection.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    static void receiveMessageSyncAutoAck(SQSConnection connection, Session session, Queue queue) {
        Message receivedMessage = null;
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

    static void receiveMessageSyncClientAck(SQSConnection connection, Session session, Queue queue) {
        Message receivedMessage = null;
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

    static void receiveMessageSyncUnorderedAck(SQSConnection connection, Session session, Queue queue) {
        Message receivedMessage = null;
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

    static void receiveMessageAsync(SQSConnection connection, Session session, Queue queue) {
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
