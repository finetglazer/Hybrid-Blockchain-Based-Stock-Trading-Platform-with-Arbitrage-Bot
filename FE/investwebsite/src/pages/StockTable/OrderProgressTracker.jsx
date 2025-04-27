// OrderProgressTracker.jsx - With enhanced compensation animation
import React, { useState, useEffect, useRef } from 'react';
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
  const [visibleCompletedSteps, setVisibleCompletedSteps] = useState([]);

  // Add a ref to track the previous status to detect transitions
  const prevStatusRef = useRef(null);

  // Track whether all animations are complete
  const [animationsComplete, setAnimationsComplete] = useState(false);

  // Animation timer ref
  const animationTimerRef = useRef(null);

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
  const isCompensating = status === 'COMPENSATING' || status === 'COMPENSATION_COMPLETED';
  const stepsToShow = isCompensating ? getCompensationSteps() : allSteps;

  // Detect entering compensation mode and reset animation state
  useEffect(() => {
    const isCurrentlyCompensating = status === 'COMPENSATING' || status === 'COMPENSATION_COMPLETED';
    const wasPreviouslyCompensating = prevStatusRef.current === 'COMPENSATING' ||
        prevStatusRef.current === 'COMPENSATION_COMPLETED';

    // If we're entering compensation mode
    if (isCurrentlyCompensating && !wasPreviouslyCompensating) {
      // Clear any existing animations and reset states
      if (animationTimerRef.current) {
        clearTimeout(animationTimerRef.current);
      }
      setVisibleCompletedSteps([]);
      setAnimationsComplete(false);

      // For COMPENSATION_COMPLETED status, we'll start animating all steps immediately
      if (status === 'COMPENSATION_COMPLETED') {
        startCompensationAnimation();
      }
    }

    // Update previous status
    prevStatusRef.current = status;
  }, [status]);

  // Start animation for all compensation steps in sequence
  const startCompensationAnimation = () => {
    const compensationSteps = getCompensationSteps();

    // Function to animate each step in sequence
    const animateSteps = (stepIndex) => {
      if (stepIndex >= compensationSteps.length) {
        // All steps animated
        setAnimationsComplete(true);
        return;
      }

      // Add current step to visible completed steps
      const stepId = compensationSteps[stepIndex].id;
      animationTimerRef.current = setTimeout(() => {
        setVisibleCompletedSteps(prev => [...prev, stepId]);
        // Continue with next step
        animateSteps(stepIndex + 1);
      }, 300); // Animation delay between steps
    };

    // Start animation sequence
    animateSteps(0);
  };

  // Effect to animate normal flow steps sequentially
  useEffect(() => {
    // Skip for compensation flow - it has its own animation
    if (isCompensating) return;

    // Find steps that are newly completed but not yet visible
    const newCompletedSteps = completedSteps.filter(
        step => !visibleCompletedSteps.includes(step)
    );

    if (newCompletedSteps.length === 0) return;

    // Get the step IDs in the correct order from stepsToShow
    const stepOrder = stepsToShow.map(step => step.id);

    // Sort new completed steps by their order in the flow
    const orderedNewSteps = [...newCompletedSteps].sort((a, b) => {
      const aIndex = stepOrder.indexOf(a);
      const bIndex = stepOrder.indexOf(b);

      // If the step isn't in our current flow, put it at the end
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;

      return aIndex - bIndex;
    }).filter(step => stepOrder.includes(step)); // Only include steps in the current flow

    // Animate each step sequentially
    const animateNextStep = (index) => {
      if (index >= orderedNewSteps.length) {
        // All animations complete
        const allComplete = status === 'COMPLETED' || status === 'FAILED';
        if (allComplete) {
          setAnimationsComplete(true);
        }
        return;
      }

      animationTimerRef.current = setTimeout(() => {
        setVisibleCompletedSteps(prev => [...prev, orderedNewSteps[index]]);
        animateNextStep(index + 1);
      }, 250); // 250ms delay between each step animation
    };

    animateNextStep(0);

    // Clean up timeouts if component unmounts during animation
    return () => {
      if (animationTimerRef.current) {
        clearTimeout(animationTimerRef.current);
      }
    };
  }, [completedSteps, visibleCompletedSteps, stepsToShow, status, isCompensating]);

  // For COMPENSATING status, we need to animate the first step when it becomes active
  useEffect(() => {
    if (status === 'COMPENSATING' && currentStep && stepsToShow.length > 0) {
      // Animate the current step as active, and all previous steps as completed
      const currentStepIndex = stepsToShow.findIndex(step => step.id === currentStep);

      // If we find the step, animate it and all previous steps
      if (currentStepIndex !== -1) {
        // Calculate which steps should be shown as completed
        const stepsToComplete = stepsToShow
            .slice(0, currentStepIndex)
            .map(step => step.id);

        // Update visible steps if needed
        if (stepsToComplete.length > 0 &&
            !stepsToComplete.every(step => visibleCompletedSteps.includes(step))) {
          setVisibleCompletedSteps(prev => {
            // Only add steps that aren't already there
            const newSteps = stepsToComplete.filter(step => !prev.includes(step));
            return [...prev, ...newSteps];
          });
        }
      }
    }
  }, [currentStep, status, stepsToShow, visibleCompletedSteps]);

  // Trigger the callback when animations are complete
  useEffect(() => {
    if (animationsComplete && onAllStepsAnimated) {
      // Add a final delay before notification
      const finalTimer = setTimeout(() => {
        onAllStepsAnimated();
      }, 500);

      return () => clearTimeout(finalTimer);
    }
  }, [animationsComplete, onAllStepsAnimated]);

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