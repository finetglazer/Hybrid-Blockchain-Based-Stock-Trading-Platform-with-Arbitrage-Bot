import React from 'react';
import './OrderProgressTracker.css';

/**
 * Component to display the current status of an order saga
 * @param {Object} props - Component props
 * @param {string} props.currentStep - The current step ID
 * @param {Array<string>} props.completedSteps - Array of completed step IDs
 * @param {string} props.status - The current saga status
 * @returns {JSX.Element} - The progress tracker component
 */
const OrderProgressTracker = ({ currentStep, completedSteps = [], status }) => {
  // Define all possible steps in correct order
  const allSteps = [
    { id: 'VERIFY_TRADING_PERMISSION', name: 'Verify Permission' },
    { id: 'VERIFY_ACCOUNT_STATUS', name: 'Verify Account' },
    { id: 'VALIDATE_STOCK', name: 'Validate Stock' },
    { id: 'GET_MARKET_PRICE', name: 'Get Price' },
    { id: 'CALCULATE_REQUIRED_FUNDS', name: 'Calculate Funds' },
    { id: 'RESERVE_FUNDS', name: 'Reserve Funds' },
    { id: 'CREATE_ORDER', name: 'Create Order' },
    { id: 'UPDATE_ORDER_VALIDATED', name: 'Validate Order' },
    { id: 'EXECUTE_ORDER', name: 'Execute Order' },
    { id: 'UPDATE_ORDER_EXECUTED', name: 'Update Order' },
    { id: 'SETTLE_TRANSACTION', name: 'Settle Transaction' },
    { id: 'UPDATE_PORTFOLIO', name: 'Update Portfolio' },
    { id: 'COMPLETE_SAGA', name: 'Complete' }
  ];

  // Define compensation steps (shown when status is COMPENSATING or COMPENSATION_COMPLETED)
  const compensationSteps = [
    { id: 'RELEASE_FUNDS', name: 'Release Funds' },
    { id: 'CANCEL_ORDER', name: 'Cancel Order' },
    // Add other compensation steps as needed
  ];

  // Determine which steps to show based on status
  const stepsToShow = status === 'COMPENSATING' || status === 'COMPENSATION_COMPLETED'
    ? compensationSteps
    : allSteps;

  return (
    <div className="order-progress-tracker">
      <h3 className="status-title">Order Status: <span className={`status-${status?.toLowerCase()}`}>{status}</span></h3>
      <div className="steps-container">
        {stepsToShow.map(step => (
          <div
            key={step.id}
            className={`step 
              ${currentStep === step.id ? 'active' : ''}
              ${completedSteps.includes(step.id) ? 'completed' : ''}
              ${status === 'FAILED' && currentStep === step.id ? 'failed' : ''}`
            }
          >
            <div className="step-indicator">
              {completedSteps.includes(step.id) ? '✓' :
               status === 'FAILED' && currentStep === step.id ? '✗' : ''}
            </div>
            <div className="step-name">{step.name}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default OrderProgressTracker;