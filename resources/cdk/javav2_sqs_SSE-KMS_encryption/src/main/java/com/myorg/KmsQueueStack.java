package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.AccountRootPrincipal;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.kms.KeySpec;
import software.amazon.awscdk.services.kms.KeyUsage;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;
import java.util.UUID;


public class KmsQueueStack extends Stack {
    public KmsQueueStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public KmsQueueStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        String keyAlias = "alias/MY_KEY" + UUID.randomUUID();
        Key.Builder.create(this, "MyKey")
                .alias(keyAlias)
                .keyUsage(KeyUsage.ENCRYPT_DECRYPT)
                .keySpec(KeySpec.SYMMETRIC_DEFAULT)
                .removalPolicy(RemovalPolicy.DESTROY)
                .pendingWindow(Duration.days(7))
                .policy(PolicyDocument.Builder.create()
                        .statements(
                                List.of(
                                        PolicyStatement.Builder.create()
                                                .actions(List.of("kms:*"))
                                                .resources(List.of("*"))
                                                .principals(List.of(new AccountRootPrincipal()))
                                                .build()
                                )

                        ).build()
                ).build();

        String queueName = "sse-kms-examples-queue" + UUID.randomUUID();
        Queue.Builder.create(this, "MyQueue")
                .queueName(queueName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        CfnOutput.Builder.create(this, "KeyAlias")
                .description("The key alias")
                .value(keyAlias)
                .build();

        CfnOutput.Builder.create(this, "QueueName")
                .description("The queue name")
                .value(queueName)
                .build();
    }
}
