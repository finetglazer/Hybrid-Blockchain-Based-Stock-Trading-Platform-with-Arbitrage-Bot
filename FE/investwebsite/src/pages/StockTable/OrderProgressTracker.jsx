// OrderProgressTracker.jsx - Improved version with dynamic compensation steps
import React, { useState, useEffect } from 'react';
import './OrderProgressTracker.css';

/**
 * Component to display the current status of an order saga with animated step transitions
 * @param {Object} props - Component props
 * @param {string} props.currentStep - The current step ID
 * @param {Array<string>} props.completedSteps - Array of completed step IDs
 * @param {string} props.status - The current saga status
 * @param {Function} props.onAllStepsAnimated - Callback when all steps have been animated
 * @returns {JSX.Element} - The progress tracker component
 */
const OrderProgressTracker = ({
                                currentStep,
                                completedSteps = [],
                                status,
                                onAllStepsAnimated
                              }) => {
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
    { id: 'SUBMIT_ORDER', name: 'Execute Order' },
    { id: 'UPDATE_ORDER_EXECUTED', name: 'Update Order' },
    { id: 'UPDATE_PORTFOLIO', name: 'Update Portfolio' },
    { id: 'SETTLE_TRANSACTION', name: 'Settle Transaction' },
    { id: 'UPDATE_ORDER_COMPLETED', name: 'Complete Order' },
    { id: 'COMPLETE_SAGA', name: 'Complete' }
  ];

  // Define all compensation steps
  const allCompensationSteps = [
    { id: 'REVERSE_SETTLEMENT', name: 'Reverse Settlement' },
    { id: 'REMOVE_POSITIONS', name: 'Remove Positions' },
    { id: 'CANCEL_BROKER_ORDER', name: 'Cancel Broker Order' },
    { id: 'RELEASE_FUNDS', name: 'Release Reserved Funds' },
    { id: 'CANCEL_ORDER', name: 'Cancel Order' }
  ];

  // Function to determine which compensation steps to show based on completed steps
  // This mirrors the backend's determineFirstCompensationStep logic
  const getCompensationSteps = () => {
    // Determine which significant steps have been completed
    const transactionSettled = completedSteps.includes('SETTLE_TRANSACTION');
    const portfolioUpdated = completedSteps.includes('UPDATE_PORTFOLIO');
    const orderExecuted = completedSteps.includes('UPDATE_ORDER_EXECUTED');
    const orderValidated = completedSteps.includes('UPDATE_ORDER_VALIDATED');
    const fundsReserved = completedSteps.includes('RESERVE_FUNDS');

    const compensationSteps = [];

    // Build the compensation chain in the correct order
    if (transactionSettled) {
      compensationSteps.push('REVERSE_SETTLEMENT');
      compensationSteps.push('REMOVE_POSITIONS');
      compensationSteps.push('CANCEL_BROKER_ORDER');
      compensationSteps.push('RELEASE_FUNDS');
      compensationSteps.push('CANCEL_ORDER');
    } else if (portfolioUpdated) {
      compensationSteps.push('REMOVE_POSITIONS');
      compensationSteps.push('CANCEL_BROKER_ORDER');
      compensationSteps.push('RELEASE_FUNDS');
      compensationSteps.push('CANCEL_ORDER');
    } else if (orderExecuted || orderValidated) {
      compensationSteps.push('CANCEL_BROKER_ORDER');
      compensationSteps.push('RELEASE_FUNDS');
      compensationSteps.push('CANCEL_ORDER');
    } else if (fundsReserved) {
      compensationSteps.push('RELEASE_FUNDS');
      compensationSteps.push('CANCEL_ORDER');
    } else {
      compensationSteps.push('CANCEL_ORDER');
    }

    // Return only the compensation steps that are in our allCompensationSteps list
    // and map them to full step objects
    return compensationSteps.map(stepId =>
        allCompensationSteps.find(step => step.id === stepId)
    ).filter(Boolean);
  };

  // Determine which steps to show based on status
  const stepsToShow = status === 'COMPENSATING' || status === 'COMPENSATION_COMPLETED'
      ? getCompensationSteps()
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
      if (index >= orderedNewSteps.length) {
        // All animations complete
        // Check if all steps are complete based on status
        const allComplete = status === 'COMPLETED' ||
            status === 'FAILED' ||
            status === 'COMPENSATION_COMPLETED';

        if (allComplete && onAllStepsAnimated) {
          // Call the callback when all steps are animated AND the saga is complete
          onAllStepsAnimated();
        }
        return;
      }

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
  }, [completedSteps, visibleCompletedSteps, stepsToShow, status, onAllStepsAnimated]);

  // Special case for the COMPLETE_SAGA step
  useEffect(() => {
    if (status === 'COMPLETED' &&
        visibleCompletedSteps.includes('COMPLETE_SAGA') &&
        onAllStepsAnimated) {

      // Add a small delay after the last step before triggering the notification
      const finalTimer = setTimeout(() => {
        onAllStepsAnimated();
      }, 800); // Slightly longer delay for the last step

      return () => clearTimeout(finalTimer);
    }
  }, [status, visibleCompletedSteps, onAllStepsAnimated]);

  // Reset visible completed steps when switching between normal and compensation flows
  useEffect(() => {
    if (status === 'COMPENSATING' && visibleCompletedSteps.length > 0) {
      // Only reset when entering compensation mode
      setVisibleCompletedSteps([]);
    }
  }, [status, visibleCompletedSteps]);

  return (
      <div className="order-progress-tracker">
        <h3 className="status-title">
          Order Status: <span className={`status-${status?.toLowerCase()}`}>{status}</span>
        </h3>
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