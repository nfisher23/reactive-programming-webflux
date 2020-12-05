package com.nickolasfisher.reactivesqs;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import javax.annotation.PostConstruct;

@Component
public class SnsSenderBean {

    private final SnsAsyncClient snsAsyncClient;

    // ARN's are immutable. In reality, you'll want to pass this in as config per environment
    private static final String topicARN = "arn:aws:sns:us-east-1:000000000000:my-topic";

    public SnsSenderBean(SnsAsyncClient snsAsyncClient) {
        this.snsAsyncClient = snsAsyncClient;
    }

    @PostConstruct
    public void sendHelloToSNS() {
        Mono.fromFuture(() -> snsAsyncClient.publish(PublishRequest.builder().topicArn(topicARN).message("message-from-sns").build()))
                .repeat(3)
                .subscribe();
    }
}
