# event-driven-microservices
Event Drivent Microservices

Part 1: Streaming Tweets from Twitter and sending them to Apache Kakfa
This involved 
Building Kafka Admin, Kafka model, Kafka Producer
Building Spring RetryTemplate for KafkaAdmin 
Building Avro Model(TwitterAvroModel) for Kafka Producer and a Transformer.
Docker Compose -schemaregisty, zookeeper and Kafka brokers

Part 2: Externalized Configuration Microservice Pattern
This involved
1.Created a git repo  cofig-server-repository with .yml files for specific microservice configuration
2.Created a config-server module as below
   1.Add dependencies for spring-cloud,spring-boot-autoconfigure,spring-cloud-starter-bootstrap
   2.application.yml - port 8888 logging levels
   3.bootstrap.yml- with config-server-repository git url and other parameters
   4.Create a class ConfigServer.java and annotate it with @EnableConfigServer
3.For other microservices to use config-server
   1.Add spring-cloud-starter-config
   2.Create bootstrap.yml with spring /application.name / profile / cloud.config.name:name-of-service,config-client
   3.Add spring-cloud-starter-bootstrap
 



