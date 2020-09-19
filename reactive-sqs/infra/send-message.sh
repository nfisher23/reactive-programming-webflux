export AWS_SECRET_ACCESS_KEY="FAKE"
export AWS_ACCESS_KEY_ID="FAKE"
export AWS_DEFAULT_REGION=us-east-1


Q_URL=$(aws --endpoint-url http://localhost:4566 sqs get-queue-url --queue-name "my-queue" --output text)
aws --endpoint-url http://localhost:4566 sqs send-message --queue-url "$Q_URL" --message-body "hey there"
