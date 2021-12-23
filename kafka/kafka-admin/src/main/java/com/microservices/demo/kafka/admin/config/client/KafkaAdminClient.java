package com.microservices.demo.kafka.admin.config.client;


import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.RetryConfigData;
import org.apache.kafka.clients.admin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class KafkaAdminClient {

    private static final Logger LOG= LoggerFactory.getLogger(KafkaAdminClient.class);

    private final KafkaConfigData kafkaConfigData;
    private final RetryConfigData retryConfigData;
    private final AdminClient adminClient;
    private final RetryTemplate retryTemplate;
    private final WebClient webClient;


    public KafkaAdminClient(KafkaConfigData kafkaConfigData, RetryConfigData retryConfigData, AdminClient adminClient, RetryTemplate retryTemplate, WebClient webClient) {
        this.kafkaConfigData = kafkaConfigData;
        this.retryConfigData = retryConfigData;
        this.adminClient = adminClient;
        this.retryTemplate = retryTemplate;
        this.webClient = webClient;
    }

    public void createTopics(){
        CreateTopicsResult createTopicsResult;
        try{
        createTopicsResult = retryTemplate.execute(this::doCreateTopics);
            LOG.info("Create topic result {}", createTopicsResult.values().values());

    } catch (Throwable t) {
            throw new RuntimeException("Reached max number of retry for creating kafka topics");
        }
        checkTopicsCreated();
    }

    private void checkTopicsCreated() {
        Collection<TopicListing> topics = getTopics();
        int retryCount=1;
        Integer maxRetry=retryConfigData.getMaxAttempts();
        Integer multiplier =retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs=retryConfigData.getSleepTimeMs();
        for(String topic: kafkaConfigData.getTopicNamesToCreate()){
            while(!isTopicCreated(topics,topic)){
                checkMaxRetry(retryCount++,maxRetry);
                sleep(sleepTimeMs);
                sleepTimeMs *=multiplier;
                topics = getTopics();
            }
        }


    }

    public void checkSchemaRegistry(){
        int retryCount=1;
        Integer maxRetry=retryConfigData.getMaxAttempts();
        Integer multiplier =retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs=retryConfigData.getSleepTimeMs();
        for(String topic: kafkaConfigData.getTopicNamesToCreate()){
            while(!getSchemaRegistryStatus().is2xxSuccessful()){
                checkMaxRetry(retryCount++,maxRetry);
                sleep(sleepTimeMs);
                sleepTimeMs *=multiplier;

            }
        }


    }

    private HttpStatus getSchemaRegistryStatus(){
        try {

            return webClient
                    .method(HttpMethod.GET)
                    .uri(kafkaConfigData.getSchemaRegistryUrl())
                    .exchange()
                    .map(ClientResponse::statusCode)
                    .block();
        } catch (Exception e) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

    }

    private void sleep(Long sleepTimeMs) {
        try{
            Thread.sleep(sleepTimeMs);
        }catch(InterruptedException e){
            throw new RuntimeException("Error while sleeping for waiting new created topics");
        }
    }

    private void checkMaxRetry(int retry, Integer maxRetry) {
        if(retry > maxRetry){
            throw new RuntimeException("reached max number of retry for reading kafka topics");
        }
    }

    private boolean isTopicCreated(Collection<TopicListing> topics, String topicName) {
        if(topics==null){
            return false;
        }
        return  topics.stream().anyMatch((topic->topic.name().equals(topicName)));
    }


    private CreateTopicsResult doCreateTopics(RetryContext retryContext){
         List<String> topicNames = kafkaConfigData.getTopicNamesToCreate();
         LOG.info("creating {} kafka topics attempt {} ",topicNames.size(),retryContext.getRetryCount());
         List<NewTopic> kafkaTopics= topicNames.stream().map(
                 topic -> new NewTopic(
                         topic.trim(),
                         kafkaConfigData.getNumberOfPartitions(),
                         kafkaConfigData.getReplicationFactor()
                 )
         ).collect(Collectors.toList());

         return  adminClient.createTopics(kafkaTopics);
    }

    private Collection<TopicListing> getTopics(){
        Collection<TopicListing> topics = null;
        try {
            topics = retryTemplate.execute(this::doGetTopics);
        }catch (Throwable e){
            throw new RuntimeException(e);
        }
        return topics;
    }

    private Collection<TopicListing> doGetTopics(RetryContext retryContext) throws ExecutionException, InterruptedException {
        LOG.info("Reading Kafka Topic {}, attempt {}", kafkaConfigData.getTopicNamesToCreate(), retryContext.getRetryCount());

        Collection<TopicListing> topics = adminClient.listTopics().listings().get();
        if (topics != null) {
            topics.forEach(topic -> LOG.debug("Topic with name {}", topic.name()));
        }
        return topics;
    }
}
