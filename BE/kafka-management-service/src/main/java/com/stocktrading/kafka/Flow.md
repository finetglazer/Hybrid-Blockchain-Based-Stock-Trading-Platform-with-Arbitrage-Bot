Flow of file code in between 2 service: kafka-service and others services

1. Send
API call to kafka-service -> kafka-service ->
Save SagaSate -> processNextStep() -> CreateCommand()
-> InitializeCommand() -> publishCommand() 

2. Receive
KafkaListener -> HandleEvent() -> checkIdempotence() -> processNextStep() -> Record the Event as process
-> updateSagaState -> moveNextStepOfSaga -> processNextStep()...

