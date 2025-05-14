import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import Sidebar from '../Sidebar/Sidebar.jsx';
import './AppLayout.css';

const AppLayout = ({ children }) => {
    const location = useLocation();
    const [sidebarExpanded, setSidebarExpanded] = useState(true);

    // Don't show sidebar on login page
    const isLoginPage = location.pathname === '/' || location.pathname === '/register' || location.pathname === '/forget-password';

    useEffect(() => {
        // Check localStorage for saved preference on initial load
        const storedState = localStorage.getItem('sidebarExpanded');
        if (storedState !== null) {
            setSidebarExpanded(storedState === 'true');
        }

        // Listen for sidebar toggle events
        const handleSidebarToggle = (event) => {
            setSidebarExpanded(event.detail.expanded);
        };

        window.addEventListener('sidebarToggle', handleSidebarToggle);

        // Clean up
        return () => {
            window.removeEventListener('sidebarToggle', handleSidebarToggle);
        };
    }, []);

    return (
        <div className="app-layout">
            {!isLoginPage && <Sidebar />}
            <div className={
                isLoginPage
                    ? 'main-content'
                    : `main-content content-with-sidebar ${sidebarExpanded ? 'expanded' : 'collapsed'}`
            }>
                {children}
            </div>
        </div>
    );
};

export default AppLayout;
