import React from 'react';
import './OrderNotificationModal.css';

/**
 * Order Notification Modal Component
 * Displays a notification when an order is completed or failed
 *
 * @param {Object} props - Component props
 * @param {boolean} props.isSuccess - Whether the order was successful
 * @param {Object} props.orderDetails - Details about the order
 * @param {Function} props.onViewPortfolio - Callback for the "View Portfolio" button
 * @param {Function} props.onClose - Callback for the "Close" button
 * @returns {JSX.Element} - The notification modal
 */
const OrderNotificationModal = ({
                                    isSuccess,
                                    orderDetails,
                                    onViewPortfolio,
                                    onClose
                                }) => {
    const status = isSuccess ? 'success' : 'failed';
    const title = isSuccess ? 'Order Completed' : 'Order Failed';
    const message = isSuccess
        ? 'Your order has been executed successfully.'
        : 'There was a problem processing your order.';

    return (
        <div className="order-notification-overlay">
            <div className="order-notification-modal">
                <div className={`notification-icon ${status}`}>
                    {isSuccess ? (
                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path
                                d="M9 12l2 2 4-4"
                                stroke="#1ca443"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                            <circle
                                cx="12"
                                cy="12"
                                r="9"
                                stroke="#1ca443"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                        </svg>
                    ) : (
                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path
                                d="M15 9l-6 6M9 9l6 6"
                                stroke="#ff4d4d"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                            <circle
                                cx="12"
                                cy="12"
                                r="9"
                                stroke="#ff4d4d"
                                strokeWidth="2"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                        </svg>
                    )}
                </div>

                <h2 className={`notification-title ${status}`}>{title}</h2>
                <p className="notification-message">{message}</p>

                {orderDetails && (
                    <div className="notification-order-details">
                        <div className="order-detail-item">
                            <span className="order-detail-label">Symbol:</span>
                            <span className="order-detail-value">{orderDetails.stockSymbol}</span>
                        </div>
                        <div className="order-detail-item">
                            <span className="order-detail-label">Order Type:</span>
                            <span className="order-detail-value">{orderDetails.orderType}</span>
                        </div>
                        <div className="order-detail-item">
                            <span className="order-detail-label">Quantity:</span>
                            <span className="order-detail-value">{orderDetails.quantity}</span>
                        </div>
                        {orderDetails.executionPrice && (
                            <div className="order-detail-item">
                                <span className="order-detail-label">Execution Price:</span>
                                <span className="order-detail-value">${parseFloat(orderDetails.executionPrice).toFixed(2)}</span>
                            </div>
                        )}
                        {orderDetails.limitPrice && orderDetails.orderType === 'LIMIT' && (
                            <div className="order-detail-item">
                                <span className="order-detail-label">Limit Price:</span>
                                <span className="order-detail-value">${parseFloat(orderDetails.limitPrice).toFixed(2)}</span>
                            </div>
                        )}
                    </div>
                )}

                <div className="notification-actions">
                    {isSuccess && (
                        <button className="notification-btn view-portfolio-btn" onClick={onViewPortfolio}>
                            View Portfolio
                        </button>
                    )}
                    <button className="notification-btn close-btn" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default OrderNotificationModal;