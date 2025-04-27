// OrderProgressTracker.jsx - Updated version
import React, { useState, useEffect } from 'react';
import './OrderProgressTracker.css';

/**
 * Component to display the current status of an order saga with animated step transitions
 * @param {Object} props - Component props
 * @param {string} props.currentStep - The current step ID
 * @param {Array<string>} props.completedSteps - Array of completed step IDs
 * @param {string} props.status - The current saga status
 * @returns {JSX.Element} - The progress tracker component
 */
const OrderProgressTracker = ({ currentStep, completedSteps = [], status }) => {
  // State to track which steps are visually shown as completed
  // This allows us to animate them sequentially even if they complete simultaneously
  const [visibleCompletedSteps, setVisibleCompletedSteps] = useState([]);

  // Define all possible steps in correct order
  const allSteps = [
    { id: 'CREATE_ORDER', name: 'Create Order' },
    { id: 'VERIFY_TRADING_PERMISSION', name: 'Verify Permission' },
    { id: 'VERIFY_ACCOUNT_STATUS', name: 'Verify Account' },
    { id: 'VALIDATE_STOCK', name: 'Validate Stock' },
    { id: 'GET_MARKET_PRICE', name: 'Get Price' },
    { id: 'CALCULATE_REQUIRED_FUNDS', name: 'Calculate Funds' },
    { id: 'RESERVE_FUNDS', name: 'Reserve Funds' },
    { id: 'UPDATE_ORDER_VALIDATED', name: 'Validate Order' },
    // Updated this step ID to match what's being sent from the backend
    { id: 'SUBMIT_ORDER', name: 'Execute Order' },
    { id: 'UPDATE_ORDER_EXECUTED', name: 'Update Order' },
    { id: 'UPDATE_PORTFOLIO', name: 'Update Portfolio' },
    { id: 'SETTLE_TRANSACTION', name: 'Settle Transaction' },
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

  // Effect to animate the completion of steps sequentially
  useEffect(() => {
    // Find steps that are newly completed but not yet visible
    const newCompletedSteps = completedSteps.filter(
        step => !visibleCompletedSteps.includes(step)
    );

    if (newCompletedSteps.length === 0) return;

    // Get the step IDs in the correct order from stepsToShow
    const stepOrder = stepsToShow.map(step => step.id);

    // Sort new completed steps by their order in the flow
    const orderedNewSteps = newCompletedSteps.sort((a, b) => {
      return stepOrder.indexOf(a) - stepOrder.indexOf(b);
    });

    // Animate each step sequentially
    let timeoutId;
    const animateNextStep = (index) => {
      if (index >= orderedNewSteps.length) return;

      timeoutId = setTimeout(() => {
        setVisibleCompletedSteps(prev => [...prev, orderedNewSteps[index]]);
        animateNextStep(index + 1);
      }, 250); // 250ms delay between each step animation
    };

    animateNextStep(0);

    // Clean up timeouts if component unmounts during animation
    return () => {
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [completedSteps, visibleCompletedSteps, stepsToShow]);

  // Reset visible completed steps when status changes between normal and compensation flow
  useEffect(() => {
    if (completedSteps.length === 0) {
      // Reset when going back to initial state
      setVisibleCompletedSteps([]);
    }
    // Reset when switching between normal and compensation flows
    const isCompFlow = status === 'COMPENSATING' || status === 'COMPENSATION_COMPLETED';
    setVisibleCompletedSteps(prev => isCompFlow ? [] : prev);
  }, [status, completedSteps]);

  // Add debugging log to help identify issues with step IDs
  console.log('Current step:', currentStep);
  console.log('Completed steps:', completedSteps);
  console.log('Visible completed steps:', visibleCompletedSteps);

  return (
      <div className="order-progress-tracker">
        <h3 className="status-title">Order Status: <span className={`status-${status?.toLowerCase()}`}>{status}</span></h3>
        <div className="steps-container">
          {stepsToShow.map(step => (
              <div
                  key={step.id}
                  className={`step 
              ${currentStep === step.id ? 'active' : ''}
              ${visibleCompletedSteps.includes(step.id) ? 'completed' : ''}
              ${status === 'FAILED' && currentStep === step.id ? 'failed' : ''}`
                  }
              >
                <div className="step-indicator">
                  {visibleCompletedSteps.includes(step.id) ? '✓' :
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