import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getUserIdFromToken } from '../../utils/auth.js';
import './Sidebar.css';

const Sidebar = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [userId, setUserId] = useState(null);
    const [activeItem, setActiveItem] = useState('');
    const [expanded, setExpanded] = useState(true);

    // Get stored sidebar state from localStorage or use default (expanded)
    useEffect(() => {
        const storedState = localStorage.getItem('sidebarExpanded');
        if (storedState !== null) {
            setExpanded(storedState === 'true');
        }
    }, []);

    useEffect(() => {
        // Extract user ID from token
        const id = getUserIdFromToken();
        setUserId(id);

        // Set active menu item based on current path
        if (location.pathname.includes('/market')) {
            setActiveItem('market');
        } else if (location.pathname.includes('/portfolio')) {
            setActiveItem('portfolio');
        } else if (location.pathname.includes('/order-history')) {
            setActiveItem('orders');
        } else if (location.pathname.includes('/transaction-history')) {
            setActiveItem('transactions');
        } else if (location.pathname.includes('/trading-accounts')) {
            setActiveItem('accounts');
        } else if (location.pathname.includes('/support')) {
            setActiveItem('support');
        }
    }, [location.pathname]);

    const navigateTo = (path, item) => {
        setActiveItem(item);
        navigate(path);
    };

    const toggleSidebar = () => {
        const newExpandedState = !expanded;
        setExpanded(newExpandedState);
        // Store preference in localStorage
        localStorage.setItem('sidebarExpanded', newExpandedState.toString());

        // Dispatch a custom event so other components can react to this change
        window.dispatchEvent(new CustomEvent('sidebarToggle', { detail: { expanded: newExpandedState } }));
    };

    return (
        <div className={`sidebar ${expanded ? 'expanded' : 'collapsed'}`}>
            <div className="sidebar-logo -ml-3">
                <button className="bg-none hover:bg-none flex pl-0 pr-0" onClick={() => {
                    navigate('/home');
                }}>
                    <img className="w-32 h-32" src="/new-logo.svg" alt="Logo" />
                    {expanded && <h3 className="mt-2">StockTrade</h3>}
                </button>
            </div>

            <div className="toggle-button" onClick={toggleSidebar}>
                {expanded ? (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="15 18 9 12 15 6"></polyline>
                    </svg>
                ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="9 18 15 12 9 6"></polyline>
                    </svg>
                )}
            </div>

            <div className="sidebar-menu">
                <div
                    className={`menu-item ${activeItem === 'market' ? 'active' : ''}`}
                    onClick={() => navigateTo('/market', 'market')}
                    title="Market"
                >
                    <div className="menu-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                        </svg>
                    </div>
                    {expanded && <span>Market</span>}
                </div>

                <div
                    className={`menu-item ${activeItem === 'portfolio' ? 'active' : ''}`}
                    onClick={() => navigateTo("/portfolio", "portfolio")}
                    title="Portfolio"
                >
                    <div className="menu-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                        </svg>
                    </div>
                    {expanded && <span>Portfolio</span>}
                </div>

                <div
                    className={`menu-item ${activeItem === 'orders' ? 'active' : ''}`}
                    onClick={() => navigateTo('/order-history', 'orders')}
                    title="Order History"
                >
                    <div className="menu-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                            <polyline points="14 2 14 8 20 8"></polyline>
                            <line x1="16" y1="13" x2="8" y2="13"></line>
                            <line x1="16" y1="17" x2="8" y2="17"></line>
                            <polyline points="10 9 9 9 8 9"></polyline>
                        </svg>
                    </div>
                    {expanded && <span>Order History</span>}
                </div>

                <div
                    className={`menu-item ${activeItem === 'transactions' ? 'active' : ''}`}
                    onClick={() => navigateTo('/transaction-history', 'transactions')}
                    title="Transaction History"
                >
                    <img className="w-7 h-7 mr-3" src="../../../src/assets/transaction.png" alt="transaction-icon"/>
                    {expanded && <span>Transaction History</span>}
                </div>

                <div
                    className={`menu-item ${activeItem === 'accounts' ? 'active' : ''}`}
                    onClick={() => navigateTo('/trading-accounts', 'accounts')}
                    title="Trading Accounts"
                >
                    <img className="w-7 h-7 mr-3" src="../../../src/assets/account-management.png" alt="account-icon"/>
                    {expanded && <span>Trading Accounts</span>}
                </div>

                <div
                    className={`menu-item ${activeItem === 'payment-methods' ? 'active' : ''}`}
                    onClick={() => navigateTo('/payment-methods', 'payment-methods')}
                    title="Payment Methods"
                >
                    <img className="w-7 h-7 mr-3" src="../../../src/assets/payment-method.png" alt="payment-method-icon"/>
                    {expanded && <span>Payment Methods</span>}
                </div>

                <div
                    className={`menu-item ${activeItem === 'support' ? 'active' : ''}`}
                    onClick={() => navigateTo('/support', 'support')}
                    title="Support"
                >
                    <img className="w-7 h-7 mr-3" src="../../../src/assets/maintenance.png" alt="support-icon"/>
                    {expanded && <span>Support</span>}
                </div>
            </div>

            <div className="sidebar-footer">
                <div className="menu-item" onClick={() => navigate('/')} title="Logout">
                    <div className="menu-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                            <polyline points="16 17 21 12 16 7"></polyline>
                            <line x1="21" y1="12" x2="9" y2="12"></line>
                        </svg>
                    </div>
                    {expanded && <span>Logout</span>}
                </div>
            </div>
        </div>
    );
};

export default Sidebar;
