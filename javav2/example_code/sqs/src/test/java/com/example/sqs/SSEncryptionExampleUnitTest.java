package com.example.sqs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SSEncryptionExampleUnitTest {

    @Mock
    private SqsClient sqsClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test case for addEncryption method
     * Verifies that the method successfully adds encryption attributes to the SQS queue
     */
    @Test
    public void testAddEncryptionSuccessful() {
        // Arrange
        String queueName = "TestQueue";
        String kmsMasterKeyAlias = "test-kms-key";
        String queueUrl = "anyqueueUrl";

        SqsClient mockSqsClient = mock(SqsClient.class);
        GetQueueUrlResponse mockGetQueueUrlResponse = mock(GetQueueUrlResponse.class);

        try (MockedStatic<SqsClient> mockedStaticSqsClient = Mockito.mockStatic(SqsClient.class)) {
            mockedStaticSqsClient.when(SqsClient::create).thenReturn(mockSqsClient);

            when(mockSqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(mockGetQueueUrlResponse);
            when(mockGetQueueUrlResponse.queueUrl()).thenReturn(queueUrl);

            // Act
            SSEncryptionExample.addEncryption(queueName, kmsMasterKeyAlias);

            // Assert
            verify(mockSqsClient).getQueueUrl(any(GetQueueUrlRequest.class));
            verify(mockSqsClient).setQueueAttributes(any(SetQueueAttributesRequest.class));
            verify(mockSqsClient).close();
        }
    }
}