// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package main

import (
	"encoding/json"
	"fmt"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/iam"
)

// PolicyDocument is our definition of our policies to be uploaded to IAM.
type PolicyDocument struct {
	Version   string
	Statement []StatementEntry
}

// StatementEntry will dictate what this policy will allow or not allow.
type StatementEntry struct {
	Effect   string
	Action   []string
	Resource string
}

// Usage:
// go run iam_createpolicy.go
func main() {
	// Initialize a session in us-west-2 that the SDK will use to load
	// credentials from the shared credentials file ~/.aws/credentials.
	sess, err := session.NewSession(&aws.Config{
		Region: aws.String("us-west-2")},
	)

	// Create a IAM service client.
	svc := iam.New(sess)

	// Builds our policy document for IAM.
	policy := PolicyDocument{
		Version: "2012-10-17",
		Statement: []StatementEntry{
			StatementEntry{
				Effect: "Allow",
				Action: []string{
					"logs:CreateLogGroup", // Allow for creating log groups
				},
				Resource: "RESOURCE ARN FOR logs:*",
			},
			StatementEntry{
				Effect: "Allow",
				// Allows for DeleteItem, GetItem, PutItem, Scan, and UpdateItem
				Action: []string{
					"dynamodb:DeleteItem",
					"dynamodb:GetItem",
					"dynamodb:PutItem",
					"dynamodb:Scan",
					"dynamodb:UpdateItem",
				},
				Resource: "RESOURCE ARN FOR dynamodb:*",
			},
		},
	}

	b, err := json.Marshal(&policy)
	if err != nil {
		fmt.Println("Error marshaling policy", err)
		return
	}

	result, err := svc.CreatePolicy(&iam.CreatePolicyInput{
		PolicyDocument: aws.String(string(b)),
		PolicyName:     aws.String("myDynamodbPolicy"),
	})

	if err != nil {
		fmt.Println("Error", err)
		return
	}

	fmt.Println("New policy", result)
}
