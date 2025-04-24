import React, { useState, useEffect, useRef } from 'react';
import './BuyOrderForm.css';

// Custom filterable dropdown component
const FilterableDropdown = ({ options, value, onChange, name, id, required, label, resetKey }) => {
    const [isOpen, setIsOpen] = useState(false);
    const [filterText, setFilterText] = useState('');
    const [filteredOptions, setFilteredOptions] = useState(options);
    const dropdownRef = useRef(null);
    const inputRef = useRef(null);
    const [isFocused, setIsFocused] = useState(false);

    // Reset the component when resetKey changes
    useEffect(() => {
        setFilterText('');
        setIsOpen(false);
        setIsFocused(false);
    }, [resetKey]);

    // Set initial filterText to match the value prop
    useEffect(() => {
        // Only set initial value once
        if (!filterText && value) {
            setFilterText(value);
        }
    }, []);  // Empty dependency array means this runs only once

    // Filter options when filterText changes
    useEffect(() => {
        if (!filterText) {
            setFilteredOptions(options);
        } else {
            const filtered = options.filter(option =>
                option.toLowerCase().includes(filterText.toLowerCase())
            );
            setFilteredOptions(filtered);
        }
    }, [filterText, options]);

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
                setIsFocused(false);
                // Don't reset the filter text when clicking outside
                // This allows users to keep typing where they left off
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [value]);

    const handleInputChange = (e) => {
        setFilterText(e.target.value);
    };

    const handleOptionClick = (option) => {
        onChange({ target: { name, value: option } });
        setFilterText(option);
        setIsOpen(false);
        setIsFocused(false);
    };

    const toggleDropdown = () => {
        setIsOpen(!isOpen);
        if (!isOpen) {
            // Focus the input when opening
            setTimeout(() => {
                if (inputRef.current) {
                    inputRef.current.focus();
                }
            }, 0);
        }
    };

    const handleInputFocus = () => {
        setIsOpen(true);
        setIsFocused(true);
    };

    return (
        <div className="form-group" ref={dropdownRef}>
            <label htmlFor={id}>{label}</label>
            <div className="custom-dropdown">
                <div className="dropdown-input-container">
                    <input
                        ref={inputRef}
                        type="text"
                        id={id}
                        name={name}
                        value={filterText}
                        onChange={handleInputChange}
                        onFocus={handleInputFocus}
                        placeholder={`Search or select ${label}`}
                        className="dropdown-input"
                        required={required}
                    />
                    <button
                        type="button"
                        className="dropdown-toggle"
                        onClick={toggleDropdown}
                    >
                        <span className={`dropdown-arrow ${isOpen ? 'open' : ''}`}>▼</span>
                    </button>
                </div>

                {isOpen && (
                    <ul className="dropdown-options">
                        {filteredOptions.map((option, index) => (
                            <li
                                key={index}
                                className={`dropdown-option ${option === value ? 'selected' : ''}`}
                                onClick={() => handleOptionClick(option)}
                            >
                                {option}
                            </li>
                        ))}
                        {filteredOptions.length === 0 && (
                            <li className="dropdown-option no-results">No matches found</li>
                        )}
                    </ul>
                )}
            </div>
        </div>
    );
};

const BuyOrderForm = ({ stockData }) => {
    // Mock data for accounts and symbols
    const accounts = [
        "Trading Account #1",
        "Trading Account #2",
        "Retirement Account",
        "Joint Account",
        "Individual Account #1",
        "Individual Account #2",
        "Investment Account",
        "Savings Account",
        "IRA Account",
        "401K Account",
        "Educational Fund Account",
        "Family Trust Account"
    ];

    const symbols = [
        "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
        "META", "NVDA", "JPM", "V", "JNJ",
        "ABBV", "WMT", "PG", "MA", "UNH"
    ];

    // Initial form state - we'll reuse this for reset functionality
    const initialFormData = {
        account: '',
        symbol: '',
        orderType: 'MARKET',
        quantity: '',
        timeInForce: 'DAY',
        price: '',
    };

    // States for form fields
    const [formData, setFormData] = useState(initialFormData);

    // State to track required funds calculation
    const [requiredFunds, setRequiredFunds] = useState(null);

    // Add a reset key to trigger resets in child components
    const [resetKey, setResetKey] = useState(0);

    // Calculate required funds only for LIMIT orders
    useEffect(() => {
        if (formData.orderType === 'LIMIT' && formData.quantity && formData.price) {
            const quantity = parseInt(formData.quantity, 10);
            const limitPrice = parseFloat(formData.price);

            if (!isNaN(quantity) && !isNaN(limitPrice)) {
                setRequiredFunds((limitPrice * quantity).toFixed(2));
            } else {
                setRequiredFunds(null);
            }
        } else {
            setRequiredFunds(null);
        }
    }, [formData.quantity, formData.price, formData.orderType]);

    // Handle form field changes
    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    // Handle form reset
    const handleReset = (e) => {
        e.preventDefault(); // Prevent the default reset behavior
        setFormData(initialFormData); // Reset to initial state
        setRequiredFunds(null); // Clear required funds display
        setResetKey(prev => prev + 1); // Increment reset key to trigger reset in FilterableDropdown
    };

    // Handle form submission
    const handleSubmit = (e) => {
        e.preventDefault();
        // Just log the form data since we're not connecting to an API yet
        console.log("Order submitted:", formData);
        alert(`Order submitted!\n${JSON.stringify(formData, null, 2)}`);
    };

    return (
        <div className="buy-order-form-container">
            <h2>Place Buy Order</h2>

            <form onSubmit={handleSubmit}>
                {/* Account Field - Single Column */}
                <div className="form-row single-column">
                    <FilterableDropdown
                        options={accounts}
                        value={formData.account}
                        onChange={handleChange}
                        name="account"
                        id="account"
                        required={true}
                        label="Account"
                        resetKey={resetKey}
                    />
                </div>

                {/* Symbol Field - Single Column */}
                <div className="form-row single-column">
                    <FilterableDropdown
                        options={symbols}
                        value={formData.symbol}
                        onChange={handleChange}
                        name="symbol"
                        id="symbol"
                        required={true}
                        label="Symbol"
                        resetKey={resetKey}
                    />
                </div>

                {/* Order Type Field - Single Column */}
                <div className="form-row single-column">
                    <div className="form-group">
                        <label htmlFor="orderType">Order Type</label>
                        <select
                            id="orderType"
                            name="orderType"
                            value={formData.orderType}
                            onChange={handleChange}
                            required
                        >
                            <option value="MARKET">MARKET</option>
                            <option value="LIMIT">LIMIT</option>
                        </select>
                    </div>
                </div>

                {/* Quantity Field - Single Column */}
                <div className="form-row single-column">
                    <div className="form-group">
                        <label htmlFor="quantity">Quantity</label>
                        <input
                            type="number"
                            id="quantity"
                            name="quantity"
                            value={formData.quantity}
                            onChange={handleChange}
                            min="1"
                            step="1"
                            required
                            placeholder="Enter quantity"
                        />
                    </div>
                </div>

                {/* Time in Force Field - Single Column (only for LIMIT orders) */}
                {formData.orderType === 'LIMIT' && (
                    <div className="form-row single-column limit-order-fields">
                        <div className="form-group">
                            <label htmlFor="timeInForce">Time in Force</label>
                            <select
                                id="timeInForce"
                                name="timeInForce"
                                value={formData.timeInForce}
                                onChange={handleChange}
                                required
                                className="time-in-force-select"
                            >
                                <option value="DAY">DAY</option>
                                <option value="GTC">GTC (Good Till Canceled)</option>
                            </select>
                        </div>
                    </div>
                )}

                {/* Limit Price Field - Single Column (only for LIMIT orders) */}
                {formData.orderType === 'LIMIT' && (
                    <div className="form-row single-column limit-order-fields">
                        <div className="form-group">
                            <label htmlFor="price">Limit Price</label>
                            <div className="price-input-container">
                                <span className="price-symbol">$</span>
                                <input
                                    type="number"
                                    id="price"
                                    name="price"
                                    value={formData.price}
                                    onChange={handleChange}
                                    step="0.01"
                                    min="0.01"
                                    required
                                    placeholder="Enter price"
                                />
                            </div>
                        </div>
                    </div>
                )}

                {/* Only show required funds for LIMIT orders */}
                {formData.orderType === 'LIMIT' && requiredFunds && (
                    <div className="required-funds">
                        <div className="funds-label">Required Funds:</div>
                        <div className="funds-amount">${requiredFunds}</div>
                    </div>
                )}

                <div className="form-actions">
                    <button onClick={handleReset} className="reset-button">Reset</button>
                    <button type="submit" className="submit-button">Place Order</button>
                </div>
            </form>
        </div>
    );
};

export default BuyOrderForm;