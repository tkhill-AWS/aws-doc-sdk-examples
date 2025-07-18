# AWS Entity Resolution code examples for the SDK for JavaScript (v3)

## Overview

Shows how to use the AWS SDK for JavaScript (v3) to work with AWS Entity Resolution.

<!--custom.overview.start-->
<!--custom.overview.end-->

_AWS Entity Resolution helps organizations extract, link, and organize information from multiple data sources._

## ⚠ Important

* Running this code might result in charges to your AWS account. For more details, see [AWS Pricing](https://aws.amazon.com/pricing/) and [Free Tier](https://aws.amazon.com/free/).
* Running the tests might result in charges to your AWS account.
* We recommend that you grant your code least privilege. At most, grant only the minimum permissions required to perform the task. For more information, see [Grant least privilege](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html#grant-least-privilege).
* This code is not tested in every AWS Region. For more information, see [AWS Regional Services](https://aws.amazon.com/about-aws/global-infrastructure/regional-product-services).

<!--custom.important.start-->
<!--custom.important.end-->

## Code examples

### Prerequisites

For prerequisites, see the [README](../../README.md#Prerequisites) in the `javascriptv3` folder.


<!--custom.prerequisites.start-->
<!--custom.prerequisites.end-->

### Get started

- [Hello AWS Entity Resolution](hello.js#L4) (`listMatchingWorkflows`)


### Single actions

Code excerpts that show you how to call individual service functions.

- [CheckWorkflowStatus](actions/check-workflow-status.js#L4)
- [CreateMatchingWorkflow](actions/create-matching-workflow.js#L4)
- [CreateSchemaMapping](actions/create-schema-mapping.js#L4)
- [DeleteMatchingWorkflow](actions/delete-matching-workflow.js#L4)
- [DeleteSchemaMapping](actions/delete-schema-mapping.js#L4)
- [GetMatchingJob](actions/get-matching-job.js#L4)
- [GetSchemaMapping](actions/get-schema-mapping.js#L4)
- [ListSchemaMappings](actions/list-schema-mappings.js#L4)
- [StartMatchingJob](actions/start-matching-job.js#L4)
- [TagEntityResource](actions/tag-entity-resource.js#L4)


<!--custom.examples.start-->
<!--custom.examples.end-->

## Run the examples

### Instructions

**Note**: All code examples are written in ECMAscript 6 (ES6). For guidelines on converting to CommonJS, see
[JavaScript ES6/CommonJS syntax](https://docs.aws.amazon.com/sdk-for-javascript/v3/developer-guide/sdk-examples-javascript-syntax.html).

**Run a single action**

```bash
node ./actions/<fileName>
```

**Run a scenario**

Most scenarios can be run with the following command:
```bash
node ./scenarios/<fileName>
```

**Run with options**

Some actions and scenarios can be run with options from the command line:
```bash
node ./scenarios/<fileName> --option1 --option2
```
[util.parseArgs](https://nodejs.org/api/util.html#utilparseargsconfig) is used to configure
these options. For the specific options available to each script, see the `parseArgs` usage
for that file.

<!--custom.instructions.start-->
<!--custom.instructions.end-->

#### Hello AWS Entity Resolution

This example shows you how to get started using AWS Entity Resolution.

```bash
node ./hello.js
```


### Tests

⚠ Running tests might result in charges to your AWS account.


To find instructions for running these tests, see the [README](../../README.md#Tests)
in the `javascriptv3` folder.



<!--custom.tests.start-->
<!--custom.tests.end-->

## Additional resources

- [AWS Entity Resolution User Guide](https://docs.aws.amazon.com/entityresolution/latest/userguide/what-is-service.html)
- [AWS Entity Resolution API Reference](https://docs.aws.amazon.com/entityresolution/latest/apireference/Welcome.html)
- [SDK for JavaScript (v3) AWS Entity Resolution reference](https://docs.aws.amazon.com/AWSJavaScriptSDK/v3/latest/client/entityresolution/)

<!--custom.resources.start-->
<!--custom.resources.end-->

---

Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
