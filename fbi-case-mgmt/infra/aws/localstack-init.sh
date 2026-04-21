#!/bin/bash
# Runs inside LocalStack once it becomes ready.
# Provisions the S3 bucket & SQS queues the backend expects.
set -euo pipefail

echo "[localstack-init] Provisioning S3 + SQS resources..."

# S3 bucket for document binaries, versioned, with lifecycle policy.
awslocal s3 mb s3://cms-documents
awslocal s3api put-bucket-versioning \
    --bucket cms-documents \
    --versioning-configuration Status=Enabled

# Enable default encryption (SSE-S3 / AES256).
awslocal s3api put-bucket-encryption --bucket cms-documents \
    --server-side-encryption-configuration '{
        "Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]
    }'

# Block public access.
awslocal s3api put-public-access-block --bucket cms-documents \
    --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# SQS queues.
awslocal sqs create-queue --queue-name cms-ingest-queue \
    --attributes VisibilityTimeout=300,MessageRetentionPeriod=345600
awslocal sqs create-queue --queue-name cms-ocr-queue \
    --attributes VisibilityTimeout=600,MessageRetentionPeriod=345600
awslocal sqs create-queue --queue-name cms-dlq \
    --attributes MessageRetentionPeriod=1209600

echo "[localstack-init] Complete."
