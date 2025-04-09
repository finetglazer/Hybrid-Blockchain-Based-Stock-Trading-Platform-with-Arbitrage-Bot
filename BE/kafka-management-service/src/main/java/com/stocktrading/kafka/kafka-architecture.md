# Kafka Architecture in Stock Trading Platform

This document outlines the Kafka implementation in our Stock Trading Platform's Saga Orchestration Service.

## Architecture Overview

The system uses Kafka as the primary messaging backbone to coordinate distributed transactions across microservices using the Saga pattern. The Kafka Management Service acts as the orchestration layer, sending commands to service-specific topics and receiving events as responses.

![Kafka Architecture](https://miro.medium.com/v2/resize:fit:1400/format:webp/1*1Xw8lG2DRK9rr8VvVly6kA.png)

## Core Components

### Message Models

1. **BaseMessage**
   - Abstract base class for all messages
   - Contains common fields: `messageId`, `sagaId`, `stepId`, `type`, `timestamp`

2. **CommandMessage**
   - Extends BaseMessage
   - Represents instructions sent to services
   - Contains payload, metadata, and target service information

3. **EventMessage**
   - Extends BaseMessage
   - Represents responses from services 
   - Contains payload and success/failure indicators

### Topic Structure

The system uses dedicated topics for different service domains:

- **Command Topics**
  - `account.commands.deposit` - Commands for account operations
  - `payment.commands.process` - Commands for payment operations
  - `user.commands.verify` - Commands for user verification

- **Event Topics**
  - `account.events.deposit` - Events from account operations
  - `payment.events.process` - Events from payment operations
  - `user.events.verify` - Events from user verification

- **Dead Letter Queue**
  - `saga.dlq` - For messages that couldn't be processed

### Message Flow

1. **Command Publishing**
   - Saga orchestrator creates commands based on the current saga step
   - Commands are published to the appropriate topic with the sagaId as the key
   - Using the sagaId as key ensures all related messages go to the same partition

2. **Event Consumption**
   - Service-specific listeners consume command messages
   - After processing, services publish event responses
   - Saga orchestrator consumes these events and updates saga state

3. **Error Handling**
   - Failed message processing is retried with exponential backoff
   - After max retries, messages go to the dead letter queue
   - Idempotency mechanisms prevent duplicate processing

## Implementation Details

### Kafka Configuration

- **Producer Configuration**
  - JSON serialization for message values
  - String serialization for keys (sagaId)
  - Idempotence enabled to prevent duplicate messages

- **Consumer Configuration**
  - Manual acknowledgment mode for better control
  - Configured consumer groups for scaling
  - Error handling with dead letter queue

### Key Classes

- **KafkaMessagePublisher**
  - Handles reliable message publishing
  - Provides callbacks for success/failure
  - Uses sagaId as the message key for partitioning

- **KafkaEventListener**
  - Listens to events from different services
  - Routes events to the appropriate saga handler
  - Handles acknowledgments and error cases

- **IdempotencyService**
  - Ensures each message is processed exactly once
  - Maintains a record of processed messages
  - Includes cleanup mechanism for old records

## Production Configuration

For production environments, additional configuration is necessary:

- Multiple broker addresses for high availability
- Security settings (SSL/TLS, authentication)
- Performance tuning for producers and consumers
- Appropriate monitoring and alerting

## Scaling Considerations

- Each service has its own consumer group
- Multiple instances of the same service share the consumer group
- Partition count determines maximum parallelism
- The sagaId-based partitioning ensures related messages are processed in order

## Monitoring and Management

- Key metrics to monitor:
  - Message throughput rates
  - Consumer lag (how far behind processing is)
  - Error rates and dead letter queue size
  - Processing latency

- Tools for Kafka management:
  - Prometheus/Grafana for metrics visualization
  - Kafka UI for topic inspection
  - Log aggregation for tracking message flow