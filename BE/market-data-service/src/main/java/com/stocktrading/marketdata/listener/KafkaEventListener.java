package com.stocktrading.marketdata.listener;

import com.project.kafkamessagemodels.model.EventMessage;
import com.stocktrading.marketdata.handler.StockDataWebSocketHandler;
import com.stocktrading.marketdata.model.StockUpdate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class KafkaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventListener.class);

    private final StockDataWebSocketHandler webSocketHandler;

    @KafkaListener(topics = "market.price.data",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listenStockUpdates(EventMessage event) {
        try {
            StockUpdate stockUpdate = new StockUpdate();
            stockUpdate.setSymbol(event.getPayloadValue("symbol"));
            stockUpdate.setPrice(new BigDecimal(event.getPayloadValue("price").toString()));
            stockUpdate.setBidPrice(new BigDecimal(event.getPayloadValue("bidPrice").toString()));
            stockUpdate.setAskPrice(new BigDecimal(event.getPayloadValue("askPrice").toString()));
            stockUpdate.setVolume(Long.parseLong(event.getPayloadValue("volume").toString()));
            stockUpdate.setTimestamp(event.getPayloadValue("timestamp"));
            stockUpdate.setChange(new BigDecimal(event.getPayloadValue("change").toString()));
            stockUpdate.setChangePercent(new BigDecimal(event.getPayloadValue("changePercent").toString()));
            String symbol = event.getPayloadValue("symbol");
            String company;

            switch (symbol) {
                case "AAPL":
                    company = "Apple Inc";
                    break;
                case "MSFT":
                    company = "Microsoft Corporation";
                    break;
                case "GOOGL":
                    company = "Alphabet Inc";
                    break;
                case "AMZN":
                    company = "Amazon.com Inc";
                    break;
                case "TSLA":
                    company = "Tesla Inc";
                    break;
                case "META":
                    company = "Meta Platforms Inc";
                    break;
                case "NVDA":
                    company = "NVIDIA Corporation";
                    break;
                case "JPM":
                    company = "JPMorgan Chase & Co.";
                    break;
                case "V":
                    company = "Visa Inc";
                    break;
                case "JNJ":
                    company = "Johnson & Johnson";
                    break;
                default:
                    company = "Unknown Company";
            }

            stockUpdate.setCompany(company);

            logger.debug("Received StockUpdate from Kafka: {}", stockUpdate);

            // Basic validation (optional, depends on data source guarantees)
            if (stockUpdate.getSymbol() == null || stockUpdate.getPrice() == null) {
                logger.warn("Received incomplete stock update from Kafka: {}", stockUpdate);
                return;
            }

            // Forward the validated update to the WebSocket handler for broadcasting
            webSocketHandler.broadcastStockUpdate(stockUpdate);

        } catch (Exception e) {
            logger.error("Error processing stock update received from Kafka: ", e);
        }
    }
}
