import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import StockTable from './StockeTable';
import BuyOrderForm from './BuyOrderForm';
import OrderProgressTracker from './OrderProgressTracker';
import OrderNotificationModal from './OrderNotificationModal';
import LoadingOverlay from './LoadingOverlay';
import {submitOrder, getOrderStatus, cancelOrder} from '../../services/orderService';
import './StockTableWithOrderForm.css';
import { getUserIdFromToken } from "../../utils/auth.js";

const StockTableWithOrderForm = () => {
    const [allStepsAnimated, setAllStepsAnimated] = useState(false);
    // Set to keep track of which saga IDs have already been processed
    const notifiedSagaIdsRef = useRef(new Set());
    // State for selected stock
    const [selectedStock, setSelectedStock] = useState(null);

    // State for order processing
    const [activeOrderId, setActiveOrderId] = useState(null);
    const [orderStatus, setOrderStatus] = useState(null);
    const [orderError, setOrderError] = useState(null);
    const [isOrderSubmitting, setIsOrderSubmitting] = useState(false);
    const [isCancellingOrder, setIsCancellingOrder] = useState(false);

    // State for notification modal
    const [showNotification, setShowNotification] = useState(false);
    const [orderComplete, setOrderComplete] = useState(false);
    const [orderSuccess, setOrderSuccess] = useState(false);

    // Add these new state variables after your existing state declarations
    const [statusHistory, setStatusHistory] = useState([]);
    const [showDebugPanel, setShowDebugPanel] = useState(true);
    const lastPollingTimeRef = useRef(null);

    // Ref for polling interval
    const pollingInterval = useRef(null);

    // Navigation hook
    const navigate = useNavigate();

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
                setAllStepsAnimated(false);

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

    // Handle order cancellation
    const handleCancelOrder = async (sagaId) => {
        if (!sagaId || isCancellingOrder) {
            return;
        }

        try {
            setIsCancellingOrder(true);
            setOrderError(null);

            // Call the cancel API
            const response = await cancelOrder(sagaId);

            // Update the order status with the cancellation info
            setOrderStatus(response);

            // Continue polling to see the cancellation progress
            // The poll function will detect completion of cancellation
        } catch (error) {
            console.error("Failed to cancel order:", error);
            setOrderError(error.response?.data?.message || "Failed to cancel order. Please try again.");
            setIsCancellingOrder(false);
        }
    };

    // Handle "View Portfolio" button click
    const handleViewPortfolio = () => {
        // Navigate to portfolio page
        navigate('/portfolio');
    };

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

    const startStatusPolling = (sagaId) => {
        // Clear any existing interval
        if (pollingInterval.current) {
            clearInterval(pollingInterval.current);
        }


        // Start polling
        pollingInterval.current = setInterval(async () => {
            lastPollingTimeRef.current = new Date().toISOString();

            try {
                // Skip polling if this saga ID is already processed as complete
                if (notifiedSagaIdsRef.current.has(sagaId)) {
                    clearInterval(pollingInterval.current);
                    return;
                }

                console.log(`Polling for order status: ${sagaId}`);
                const statusData = await getOrderStatus(sagaId);
                console.log(`Received status: ${statusData.status}`, statusData);

                // Skip updating if this saga ID has been marked as complete
                if (notifiedSagaIdsRef.current.has(sagaId)) {
                    return;
                }

                // Track status history for debugging
                if (!orderStatus || statusData.status !== orderStatus.status) {
                    setStatusHistory(prev => [...prev, {
                        timestamp: new Date().toISOString(),
                        status: statusData.status,
                        currentStep: statusData.currentStep
                    }]);
                    console.log(`Status changed to: ${statusData.status}`);
                }

                setOrderStatus(statusData);

                // Check if the saga is complete
                const isComplete = statusData.status === 'COMPLETED' ||
                    statusData.status === 'FAILED' ||
                    statusData.status === 'COMPENSATION_COMPLETED';

                if (isComplete) {
                    // Mark this saga ID as processed
                    notifiedSagaIdsRef.current.add(sagaId);

                    clearInterval(pollingInterval.current);

                    // Set order completion states
                    setOrderComplete(true);
                    setOrderSuccess(statusData.status === 'COMPLETED');

                    // We'll handle showing the notification with useEffect below
                }
            } catch (error) {
                console.error("Error checking order status:", error);

                // Only show error notification if we haven't processed this saga yet
                if (!notifiedSagaIdsRef.current.has(sagaId)) {
                    notifiedSagaIdsRef.current.add(sagaId);
                    setOrderError("Failed to get order status updates");
                    clearInterval(pollingInterval.current);

                    // Show failure notification immediately for errors
                    setOrderComplete(true);
                    setOrderSuccess(false);
                    setShowNotification(true);
                }
            }
        }, 1000);
    };

    // Only show notification when both order is complete AND all steps have been animated
    useEffect(() => {
        if (orderComplete && allStepsAnimated && activeOrderId) {
            // Don't show notification if we've already shown one for this saga
            if (notifiedSagaIdsRef.current.has(activeOrderId) && showNotification) {
                return;
            }

            // Add a small delay for visual polish
            const timerId = setTimeout(() => {
                setShowNotification(true);
            }, 500);

            return () => clearTimeout(timerId);
        }
    }, [orderComplete, allStepsAnimated, activeOrderId, showNotification]);

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
            notifiedSagaIdsRef.current.clear(); // Clear notification tracking
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
                            sagaId={activeOrderId}
                            onCancelOrder={handleCancelOrder}
                            isCancelling={isCancellingOrder}
                            onAllStepsAnimated={() => setAllStepsAnimated(true)}
                            isLimitOrder={orderStatus?.orderType === 'LIMIT'}
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

            {/* Debug Panel */}
            {showDebugPanel && orderStatus && (
                <div className="debug-panel">
                    <div className="debug-header">
                        <h3>Debug Information</h3>
                        <button onClick={() => setShowDebugPanel(false)}>Hide</button>
                    </div>
                    <div className="debug-content">
                        <div className="debug-section">
                            <h4>Current Status</h4>
                            <pre>{JSON.stringify(orderStatus, null, 2)}</pre>
                        </div>
                        <div className="debug-section">
                            <h4>Status History ({statusHistory.length})</h4>
                            <table className="debug-table">
                                <thead>
                                <tr>
                                    <th>Time</th>
                                    <th>Status</th>
                                    <th>Current Step</th>
                                </tr>
                                </thead>
                                <tbody>
                                {statusHistory.map((entry, index) => (
                                    <tr key={index}>
                                        <td>{new Date(entry.timestamp).toLocaleTimeString()}</td>
                                        <td className={`status-${entry.status?.toLowerCase()}`}>
                                            {entry.status}
                                        </td>
                                        <td>{entry.currentStep}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="debug-section">
                            <h4>Polling Information</h4>
                            <p>Active Order ID: {activeOrderId || 'None'}</p>
                            <p>Is Cancelling: {isCancellingOrder ? 'Yes' : 'No'}</p>
                            <p>Last Poll: {lastPollingTimeRef.current ?
                                new Date(lastPollingTimeRef.current).toLocaleTimeString() : 'N/A'}</p>
                            <p>All Steps Animated: {allStepsAnimated ? 'Yes' : 'No'}</p>
                            <p>Show Notification: {showNotification ? 'Yes' : 'No'}</p>
                            <p>Order Complete: {orderComplete ? 'Yes' : 'No'}</p>
                        </div>
                    </div>
                </div>
            )}

            {/* Toggle button to show debug panel if it's hidden */}
            {!showDebugPanel && (
                <button className="show-debug-button" onClick={() => setShowDebugPanel(true)}>
                    Show Debug Panel
                </button>
            )}
        </>
    );
};

export default StockTableWithOrderForm;