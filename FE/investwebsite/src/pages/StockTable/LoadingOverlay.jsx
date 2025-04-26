import React from 'react';
import './LoadingOverlay.css';

/**
 * Loading overlay component that displays a spinner and message
 * @param {Object} props - Component props
 * @param {boolean} props.visible - Whether the overlay is visible
 * @param {string} props.message - Message to display (optional)
 * @returns {JSX.Element|null} - The loading overlay or null if not visible
 */
const LoadingOverlay = ({ visible, message = "Preparing your order..." }) => {
    if (!visible) return null;

    return (
        <div className="loading-overlay">
            <div className="loading-content">
                <div className="spinner">
                    <div className="spinner-circle"></div>
                </div>
                <div className="loading-message">{message}</div>
            </div>
        </div>
    );
};

export default LoadingOverlay;