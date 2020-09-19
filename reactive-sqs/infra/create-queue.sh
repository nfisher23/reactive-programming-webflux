

export AWS_SECRET_ACCESS_KEY="FAKE"
export AWS_ACCESS_KEY_ID="FAKE"
export AWS_DEFAULT_REGION=us-east-1

QUEUE_NAME="my-queue"

aws --endpoint-url http://localhost:4566 sqs create-queue --queue-name "$QUEUE_NAME"
