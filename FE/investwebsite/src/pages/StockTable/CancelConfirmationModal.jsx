import React from 'react';
import './CancelConfirmationModal.css';

/**
 * Modal that asks user to confirm order cancellation
 * @param {Object} props
 * @param {Function} props.onConfirm - Function to call when user confirms cancellation
 * @param {Function} props.onCancel - Function to call when user cancels the cancellation
 * @returns {JSX.Element}
 */
const CancelConfirmationModal = ({ onConfirm, onCancel }) => {
    return (
        <div className="cancel-confirmation-overlay">
            <div className="cancel-confirmation-modal">
                <div className="warning-icon">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path
                            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                            stroke="#f39c12"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        />
                    </svg>
                </div>
                <h3 className="modal-title">Cancel Order?</h3>
                <p className="modal-message">
                    Are you sure you want to cancel this order? This action cannot be undone.
                </p>
                <div className="modal-actions">
                    <button className="modal-btn cancel-btn" onClick={onCancel}>
                        Keep Order
                    </button>
                    <button className="modal-btn confirm-btn" onClick={onConfirm}>
                        Yes, Cancel Order
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CancelConfirmationModal;