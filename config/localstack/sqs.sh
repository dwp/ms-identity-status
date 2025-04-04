#!/bin/bash

awslocal sqs create-queue --queue-name pip_idv_outcome
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:pip_identity_event_nino --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:pip_idv_outcome --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-pip_identity_routing_key\": [ \"pip_identity_inbound_routing\" ] }"
awslocal sns get-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN"


awslocal sqs create-queue --queue-name update_pipcs_idv_queue
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:update_pipcs_idv_topic --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:update_pipcs_idv_queue --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\": [ \"pip_identity_outbound_routing\" ] }"
awslocal sns get-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN"

awslocal sqs create-queue --queue-name pip_idv_outcome_dlq
awslocal sqs set-queue-attributes --queue-url http://localstack:4566/000000000000/pip_idv_outcome --attributes '{"VisibilityTimeout": "5", "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:pip_idv_outcome_dlq\",\"maxReceiveCount\":5}"}'
awslocal sqs get-queue-attributes --queue-url http://localstack:4566/000000000000/pip_idv_outcome --attribute-names RedrivePolicy

awslocal sqs create-queue --queue-name change-stream-queue
awslocal sns subscribe --return-subscription-arn --topic-arn "arn:aws:sns:eu-west-2:000000000000:stream-topic" --protocol sqs --notification-endpoint "arn:aws:sqs:elasticmq:000000000000:change-stream-queue"

awslocal sqs create-queue --queue-name claimant_identity_event
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:pip_identity_event_guid --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:claimant_identity_event --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\": [ \"pip_identity_outbound_routing\" ] }"
awslocal sns get-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN"

awslocal sqs create-queue --queue-name update_coordinator_idv_queue
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:update_coordinator_idv_topic --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:update_coordinator_idv_queue --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\": [ \"pip_identity_coordinator_outbound_routing\" ] }"
awslocal sns get-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN"
