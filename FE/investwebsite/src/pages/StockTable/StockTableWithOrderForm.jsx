import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import StockTable from './StockeTable';
import BuyOrderForm from './BuyOrderForm';
import OrderProgressTracker from './OrderProgressTracker';
import { submitOrder, getOrderStatus } from '../../services/orderService';
import './StockTableWithOrderForm.css';
import {getUserIdFromToken} from "../../utils/auth.js";

const StockTableWithOrderForm = () => {
    // State for selected stock
    const [selectedStock, setSelectedStock] = useState(null);

    // State for order processing
    const [activeOrderId, setActiveOrderId] = useState(null);
    const [orderStatus, setOrderStatus] = useState(null);
    const [orderError, setOrderError] = useState(null);

    // Ref for polling interval
    const pollingInterval = useRef(null);

    // Navigation hook
    const navigate = useNavigate();

    // Hard-coded user and account IDs for demo purposes
    // In a real app, these would come from auth context or user selection
    const userId = getUserIdFromToken();
    const accountId = "67f78822422684206f613a40";

    // Function to handle stock selection from the table
    const handleStockSelect = (stock) => {
        setSelectedStock(stock);
    };

    const handleSubmitOrder = async (formData) => {
        try {
            // Clear any previous errors
            setOrderError(null);

            const orderData = {
                userId: userId,
                accountId: formData.accountId, // From the form
                stockSymbol: formData.symbol,
                orderType: formData.orderType,
                quantity: parseInt(formData.quantity),
                limitPrice: formData.orderType === 'LIMIT' ? parseFloat(formData.price) : undefined,
                timeInForce: formData.timeInForce || "DAY"
            };

            const response = await submitOrder(orderData);

            if (response && response.sagaId) {
                setActiveOrderId(response.sagaId);
                setOrderStatus(response);
                startStatusPolling(response.sagaId);
            } else {
                setOrderError("Received invalid response from server");
            }
        } catch (error) {
            console.error("Failed to submit order:", error);
            setOrderError(error.response?.data?.message || "Failed to submit order. Please try again.");
        }
    };

    // Start polling for status updates
    const startStatusPolling = (sagaId) => {
        // Clear any existing interval
        if (pollingInterval.current) {
            clearInterval(pollingInterval.current);
        }

        // Start polling
        pollingInterval.current = setInterval(async () => {
            try {
                const statusData = await getOrderStatus(sagaId);
                setOrderStatus(statusData);

                // Stop polling if the saga is complete
                if (statusData.status === 'COMPLETED' ||
                    statusData.status === 'FAILED' ||
                    statusData.status === 'COMPENSATION_COMPLETED') {
                    clearInterval(pollingInterval.current);

                    // Navigate to portfolio if completed successfully
                    if (statusData.status === 'COMPLETED') {
                        setTimeout(() => {
                            // In real app, navigate to portfolio or order history
                            // navigate('/portfolio');
                            console.log("Order completed successfully!");
                        }, 3000); // Wait 3 seconds to show completion
                    }
                }
            } catch (error) {
                console.error("Error checking order status:", error);
                setOrderError("Failed to get order status updates");
                clearInterval(pollingInterval.current);
            }
        }, 2000); // Check every 2 seconds
    };

    // Clean up on unmount
    useEffect(() => {
        return () => {
            if (pollingInterval.current) {
                clearInterval(pollingInterval.current);
            }
        };
    }, []);

    // Reset order when a new stock is selected
    useEffect(() => {
        if (selectedStock && activeOrderId) {
            setActiveOrderId(null);
            setOrderStatus(null);
            clearInterval(pollingInterval.current);
        }
    }, [selectedStock]);

    return (
        <>
            {/* Added page title */}
            <div className="page-header">
                <h1 className="page-title">Stock Trading Dashboard</h1>
            </div>

            <div className="stock-trading-container">
                {/* Stock table section */}
                <div className="stock-table-section">
                    <StockTable onSelectStock={handleStockSelect} />
                </div>

                {/* Order form and status section */}
                <div className="order-section">
                    <BuyOrderForm
                        stockData={selectedStock}
                        onSubmit={handleSubmitOrder}
                        disabled={!!activeOrderId}
                    />

                    {orderError && (
                        <div className="order-error">
                            <p>{orderError}</p>
                        </div>
                    )}

                    {orderStatus && (
                        <OrderProgressTracker
                            currentStep={orderStatus.currentStep}
                            completedSteps={orderStatus.completedSteps || []}
                            status={orderStatus.status}
                        />
                    )}
                </div>
            </div>
        </>
    );
};

export default StockTableWithOrderForm;