import React, { useState, useEffect, useRef } from 'react';
import './SellOrderForm.css';
import axios from 'axios';
import {getUserIdFromToken} from "../../utils/auth.js";
import {Alert} from "antd";

/**
 * FilterableDropdown component for searchable dropdowns
 */
const FilterableDropdown = ({ options, value, onChange, name, id, required, label, resetKey, disabled, isLoading }) => {
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
        // Only set initial value once when value changes
        if (value) {
            setFilterText(value);
        }
    }, [value]);

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
        if (disabled || isLoading) return;
        setIsOpen(!isOpen);
        if (!isOpen) {
            setTimeout(() => {
                if (inputRef.current) {
                    inputRef.current.focus();
                }
            }, 0);
        }
    };

    const handleInputFocus = () => {
        if (disabled || isLoading) return;
        setIsOpen(true);
        setIsFocused(true);
    };

    return (
        <div className="form-group" ref={dropdownRef}>
            <label htmlFor={id}>{label}</label>
            <div className={`custom-dropdown ${disabled ? 'disabled' : ''} ${isLoading ? 'loading' : ''}`}>
                <div className="dropdown-input-container">
                    <input
                        ref={inputRef}
                        type="text"
                        id={id}
                        name={name}
                        value={filterText}
                        onChange={handleInputChange}
                        onFocus={handleInputFocus}
                        placeholder={isLoading ? 'Loading...' : `Search or select ${label}`}
                        className="dropdown-input"
                        required={required}
                        disabled={disabled || isLoading}
                    />
                    <button
                        type="button"
                        className="dropdown-toggle"
                        onClick={toggleDropdown}
                        disabled={disabled || isLoading}
                    >
                        <span className={`dropdown-arrow ${isOpen ? 'open' : ''}`}>▼</span>
                    </button>
                </div>

                {isOpen && !disabled && !isLoading && (
                    <ul className="dropdown-options">
                        {filteredOptions.length > 0 ? (
                            filteredOptions.map((option, index) => (
                                <li
                                    key={index}
                                    className={`dropdown-option ${option === value ? 'selected' : ''}`}
                                    onClick={() => handleOptionClick(option)}
                                >
                                    {option}
                                </li>
                            ))
                        ) : (
                            <li className="dropdown-option no-results">No matches found</li>
                        )}
                    </ul>
                )}
            </div>
        </div>
    );
};

/**
 * Sell Order Form Component
 * @param {Object} props - Component props
 * @param {Function} props.onSubmit - Function to handle form submission
 * @param {boolean} props.disabled - Whether the form is disabled
 */
const SellOrderForm = ({ sellSymbol, setError, setLoading, setOrderCreated, setShowForm, disabled = false }) => {
    // State for accounts fetched from API
    const [accounts, setAccounts] = useState([]);
    const [accountsLoading, setAccountsLoading] = useState(true);
    const [accountsError, setAccountsError] = useState(null);

    // Initial form state - we'll reuse this for reset functionality
    const initialFormData = {
        userId: getUserIdFromToken(),
        accountId: '',
        stockSymbol: sellSymbol,
        quantity: '',
    };

    // States for form fields
    const [formData, setFormData] = useState(initialFormData);

    // Add a reset key to trigger resets in child components
    const [resetKey, setResetKey] = useState(0);

    const [accountOptions, setAccountOptions] = useState([]); // Store full account objects

    // Fetch accounts from API
    useEffect(() => {
        const fetchAccounts = async () => {
            try {
                setAccountsLoading(true);
                const token = localStorage.getItem("token");
                const response = await axios.get('/accounts/api/v1/get-names', {
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json",
                    }
                });

                if (response.data.status === 1) {
                    // Store full account objects
                    setAccountOptions(response.data.data);
                    // Extract just the names for display
                    setAccounts(response.data.data.map(account => account.name));
                } else {
                    setAccountsError(response.data.msg || 'Failed to load trading accounts');
                    setAccounts([]);
                }
            } catch (error) {
                console.error('Error fetching accounts:', error);
                setAccountsError(error.message || 'Failed to load trading accounts');

                // Set some fallback accounts in case of error
                setAccounts([
                    "Trading Account #1",
                    "Trading Account #2",
                    "Error loading accounts"
                ]);
            } finally {
                setAccountsLoading(false);
            }
        };

        fetchAccounts();
    }, []);

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
        setResetKey(prev => prev + 1); // Increment reset key to trigger reset in FilterableDropdown
    };

    // Handle form submission
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!disabled) {
            // Find the selected account object by name
            const selectedAccount = accountOptions.find(acc => acc.name === formData.account);

            // Include the account ID in the form data
            try {
                setLoading(true);
                const token = localStorage.getItem("token");
                const response = await axios.post("/sagas/api/v1/orders/sell", {
                    userId: getUserIdFromToken(),
                    accountId: selectedAccount?.id,
                    stockSymbol: sellSymbol,
                    quantity: formData.quantity,
                }, {
                    headers: {
                        "Authorization": "Bearer " + token,
                        "Content-Type": "application/json"
                    }
                });
                setLoading(false);
                if (response.data && response.data.sagaId) {
                    setError(null);
                    setOrderCreated(true);
                    setShowForm(false);
                } else {
                    setError(response.errors);
                    setOrderCreated(false);
                }
            }
            catch (e) {
                setLoading(false);
                setError(e.message);
                setOrderCreated(false);
            }
        }
    };

    const formTitle = disabled ? "Processing Order..." : "Place Sell Order";

    return (
        <div className={`relative buy-order-form-container ${disabled ? 'disabled' : ''}`}>
            <button className="absolute -top-1.5 -right-1.5" onClick={() => setShowForm(false)}>
                X
            </button>
            <h2>{formTitle}</h2>

            {accountsError && (
                <div className="api-error-message">
                    <p>{accountsError}</p>
                    <p>Using fallback account list...</p>
                </div>
            )}

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
                        disabled={disabled}
                        isLoading={accountsLoading}
                    />
                </div>

                {/* Symbol Field - Single Column */}
                <div className="form-row single-column">
                    <FilterableDropdown
                        options={[sellSymbol]}
                        value={sellSymbol}
                        onChange={handleChange}
                        name="symbol"
                        id="symbol"
                        required={true}
                        label="Symbol"
                        resetKey={resetKey}
                        disabled={true}
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
                            disabled={true}
                            className={disabled ? 'disabled' : ''}
                        >
                            <option value="MARKET">MARKET</option>
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
                            max="100000"
                            step="1"
                            required
                            placeholder="Enter quantity (1-100,000)"
                            disabled={disabled}
                            className={disabled ? 'disabled' : ''}
                        />
                        {formData.quantity > 100000 && (
                            <div className="input-error">
                                Maximum order size is 100,000 shares
                            </div>
                        )}
                    </div>
                </div>

                <div className="form-actions">
                    <button
                        onClick={handleReset}
                        className="reset-button"
                        disabled={disabled}
                    >
                        Reset
                    </button>
                    <button
                        type="submit"
                        className={`submit-button ${disabled ? 'loading' : ''}`}
                        disabled={disabled || accountsLoading || (formData.quantity > 100000)}
                    >
                        {disabled ? 'Processing...' : 'Place Order'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default SellOrderForm;
