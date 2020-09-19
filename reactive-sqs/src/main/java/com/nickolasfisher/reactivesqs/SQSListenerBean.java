package com.nickolasfisher.reactivesqs;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.RetrySpec;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

@Component
public class SQSListenerBean {

    public static final Logger LOGGER = LoggerFactory.getLogger(SQSListenerBean.class);
    private final SqsAsyncClient sqsAsyncClient;
    private final String queueUrl;

    public SQSListenerBean(SqsAsyncClient sqsAsyncClient) {
        this.sqsAsyncClient = sqsAsyncClient;
        try {
            this.queueUrl = this.sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder().queueName("my-queue").build()).get().queueUrl();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void continuousListener() {
        Mono<ReceiveMessageResponse> receiveMessageResponseMono = Mono.fromFuture(() ->
                sqsAsyncClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .maxNumberOfMessages(5)
                            .queueUrl(queueUrl)
                            .waitTimeSeconds(10)
                            .visibilityTimeout(30)
                            .build()
                )
        );

        receiveMessageResponseMono
                .repeat()
                .retry()
                .map(ReceiveMessageResponse::messages)
                .map(Flux::fromIterable)
                .flatMap(messageFlux -> messageFlux)
                .subscribe(message -> {
                    LOGGER.info("message body: " + message.body());

                    sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build())
                        .thenAccept(deleteMessageResponse -> {
                            LOGGER.info("deleted message with handle " + message.receiptHandle());
                        });
                });
    }
}
