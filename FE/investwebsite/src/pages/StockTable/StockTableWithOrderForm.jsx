import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import StockTable from './StockeTable';
import BuyOrderForm from './BuyOrderForm';
import OrderProgressTracker from './OrderProgressTracker';
import OrderNotificationModal from './OrderNotificationModal';
import LoadingOverlay from './LoadingOverlay';
import { submitOrder, getOrderStatus } from '../../services/orderService';
import './StockTableWithOrderForm.css';
import { getUserIdFromToken } from "../../utils/auth.js";

const StockTableWithOrderForm = () => {
    const [allStepsAnimated, setAllStepsAnimated] = useState(false);
    // Add this near the top of your component
    const notifiedSagaIdsRef = useRef(new Set());
    // State for selected stock
    const [selectedStock, setSelectedStock] = useState(null);

    // State for order processing
    const [activeOrderId, setActiveOrderId] = useState(null);
    const [orderStatus, setOrderStatus] = useState(null);
    const [orderError, setOrderError] = useState(null);
    const [isOrderSubmitting, setIsOrderSubmitting] = useState(false);

    // State for notification modal
    const [showNotification, setShowNotification] = useState(false);
    const [orderComplete, setOrderComplete] = useState(false);
    const [orderSuccess, setOrderSuccess] = useState(false);

    // Ref for polling interval
    const pollingInterval = useRef(null);

    // Navigation hook
    const navigate = useNavigate();
    // Add this new ref to track if notification was shown
    const notificationShownRef = useRef(false);

    // Get userId from token
    const userId = getUserIdFromToken();

    // Function to handle stock selection from the table
    const handleStockSelect = (stock) => {
        setSelectedStock(stock);
    };

    const handleSubmitOrder = async (formData) => {
        try {
            // Clear any previous errors
            setOrderError(null);
            // Set submitting state to true to show loading indicator
            setIsOrderSubmitting(true);

            const orderData = {
                userId: userId,
                accountId: formData.accountId,
                stockSymbol: formData.symbol,
                orderType: formData.orderType,
                quantity: parseInt(formData.quantity),
                limitPrice: formData.orderType === 'LIMIT' ? parseFloat(formData.price) : undefined,
                timeInForce: formData.timeInForce || "DAY"
            };

            const response = await submitOrder(orderData);

            if (response && response.sagaId) {
                // Clear previous order notifications when starting a new order
                notifiedSagaIdsRef.current.clear();

                setActiveOrderId(response.sagaId);
                setOrderStatus(response);
                startStatusPolling(response.sagaId);
            } else {
                setOrderError("Received invalid response from server");
            }

        } catch (error) {
            console.error("Failed to submit order:", error);
            setOrderError(error.response?.data?.message || "Failed to submit order. Please try again.");
        } finally {
            // Set submitting state back to false when the request completes
            setIsOrderSubmitting(false);
        }
    };

    // Handle "View Portfolio" button click
    const handleViewPortfolio = () => {
        // Navigate to portfolio page
        navigate('/portfolio');
    };

    // In StockTableWithOrderForm.jsx, add to the handleCloseNotification function
    const handleCloseNotification = () => {
        setShowNotification(false);
        // Reset order states if needed
        if (orderComplete) {
            setActiveOrderId(null);
            setOrderStatus(null);
            setOrderComplete(false);
            setAllStepsAnimated(false); // Reset animation state too
        }
    };

    // Then update the startStatusPolling function
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

                    // Set order completion states
                    setOrderComplete(true);
                    setOrderSuccess(statusData.status === 'COMPLETED');

                    // Don't show notification immediately
                    // We'll handle showing it with useEffect below
                }
            } catch (error) {
                console.error("Error checking order status:", error);
                setOrderError("Failed to get order status updates");
                clearInterval(pollingInterval.current);

                // Show failure notification immediately for errors
                setOrderComplete(true);
                setOrderSuccess(false);
                setShowNotification(true);
            }
        }, 1000);
    };

    // In StockTableWithOrderForm.jsx, add this effect
    useEffect(() => {
        // Only show notification when both order is complete AND all steps have been animated
        if (orderComplete && allStepsAnimated) {
            // Add a small delay for visual polish
            const timerId = setTimeout(() => {
                setShowNotification(true);
            }, 500);

            return () => clearTimeout(timerId);
        }
    }, [orderComplete, allStepsAnimated]);

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
            setAllStepsAnimated(false); // Reset animation state
            clearInterval(pollingInterval.current);
        }
    }, [selectedStock]);

    return (
        <>
            {/* Added page title */}
            <div className="page-header">
                <h1 className="page-title">Stock Trading Dashboard</h1>
            </div>

            <div className={`stock-trading-container ${showNotification ? 'blurred' : ''}`}>
                {/* Stock table section */}
                <div className="stock-table-section">
                    <StockTable onSelectStock={handleStockSelect} />
                </div>

                {/* Order form and status section */}
                <div className="order-section">
                    <div style={{ position: 'relative' }}>
                        <BuyOrderForm
                            stockData={selectedStock}
                            onSubmit={handleSubmitOrder}
                            disabled={!!activeOrderId || isOrderSubmitting}
                        />
                        <LoadingOverlay
                            visible={isOrderSubmitting}
                            message="Preparing your order..."
                        />
                    </div>

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
                            onAllStepsAnimated={() => setAllStepsAnimated(true)}
                        />
                    )}
                </div>
            </div>

            {/* Order notification modal with failure reason */}
            {showNotification && (
                <OrderNotificationModal
                    isSuccess={orderSuccess}
                    orderDetails={orderStatus}
                    errorMessage={orderStatus?.failureReason || orderError}
                    onViewPortfolio={handleViewPortfolio}
                    onClose={handleCloseNotification}
                />
            )}
        </>
    );
};

export default StockTableWithOrderForm;