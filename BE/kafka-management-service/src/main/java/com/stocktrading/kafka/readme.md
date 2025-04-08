# Kafka Management Service

This service manages Kafka-based messaging for the Stock Trading Platform, with a focus on the Saga pattern implementation for distributed transactions.

## Features

- Saga orchestration for deposit transactions
- Event-driven architecture using Kafka
- Idempotent message processing
- Timeout handling and recovery
- Compensation transactions for failures
- Monitoring and metrics
- REST API for managing sagas

## Architecture

The service follows a microservices architecture and is responsible for coordinating distributed transactions across multiple services:

- **User Service** - User authentication and verification
- **Account Service** - Account management and balance operations
- **Payment Processor** - Payment processing

## Saga Flow

The deposit transaction saga follows these steps:

1. Verify user identity
2. Validate payment method
3. Create pending transaction
4. Process payment
5. Update transaction status
6. Update account balance

If any step fails, compensation transactions are executed in reverse order to maintain data consistency.

## Technical Components

- **Spring Boot** - Base framework
- **Spring Kafka** - Kafka integration
- **Spring Data MongoDB** - Saga state persistence
- **Spring Actuator & Micrometer** - Monitoring and metrics
- **Swagger/OpenAPI** - API documentation

## Getting Started

### Prerequisites

- JDK 11+
- Maven
- Docker & Docker Compose

### Running Locally

1. Clone the repository
2. Start the infrastructure:

```bash
docker-compose up -d zookeeper kafka mongodb
```

3. Build and run the application:

```bash
./mvnw spring-boot:run
```

### Using Docker Compose

Run the entire stack:

```bash
docker-compose up -d
```

## API Documentation

API documentation is available at:

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

## Monitoring

### Prometheus & Grafana

Metrics are exposed via Spring Actuator and can be viewed in:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

### Kafka UI

Monitor Kafka topics and messages:

- Kafka UI: `http://localhost:8080`

### MongoDB Express

View and manage MongoDB data:

- Mongo Express: `http://localhost:8081`

## Development

### Mock Services

For development and testing, the application includes mock implementations of the external services.

To enable mock services, set:

```properties
mock.services.enabled=true
```

## Key Configuration Properties

```yaml
# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=saga-orchestrator-group
spring.kafka.consumer.auto-offset-reset=earliest

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/saga_db

# Saga Configuration
saga.deposit.timeout.verify-identity=5000
saga.deposit.timeout.validate-payment=5000
saga.deposit.timeout.create-transaction=10000
saga.deposit.timeout.process-payment=30000
saga.deposit.timeout.update-transaction=10000
saga.deposit.timeout.update-balance=10000
saga.deposit.retry.max-attempts=3
```

## Testing

Run unit tests:

```bash
./mvnw test
```

## License

Proprietary - For Stock Trading Platform internal use only
