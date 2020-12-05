

export AWS_SECRET_ACCESS_KEY="FAKE"
export AWS_ACCESS_KEY_ID="FAKE"
export AWS_DEFAULT_REGION=us-east-1

QUEUE_NAME="my-queue"
TOPIC_NAME="my-topic"

QUEUE_URL=$(aws --endpoint-url http://localhost:4566 sqs create-queue --queue-name "$QUEUE_NAME" --output text)
echo "queue url: $QUEUE_URL"

TOPIC_ARN=$(aws --endpoint-url http://localhost:4566 sns create-topic --output text --name "$TOPIC_NAME")
echo "topic arn: $TOPIC_ARN"

QUEUE_ARN=$(aws --endpoint-url http://localhost:4566 sqs get-queue-attributes --queue-url "$QUEUE_URL" | jq -r ".Attributes.QueueArn")
echo "queue arn: $QUEUE_ARN"

SUBSCRIPTION_ARN=$(aws --endpoint-url http://localhost:4566 sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$QUEUE_ARN" --output text)

# modify to raw message delivery true
aws --endpoint-url http://localhost:4566 sns set-subscription-attributes \
  --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name RawMessageDelivery --attribute-value true
