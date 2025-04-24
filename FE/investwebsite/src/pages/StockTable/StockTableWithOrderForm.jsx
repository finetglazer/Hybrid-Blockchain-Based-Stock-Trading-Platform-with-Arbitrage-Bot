import React, { useState, useEffect } from 'react';
import StockTable from './StockeTable'; // Import your existing StockTable component
import BuyOrderForm from './BuyOrderForm'; // Import the new BuyOrderForm component

const StockTableWithOrderForm = () => {
    // State to hold selected stock data that can be passed to BuyOrderForm
    const [selectedStock, setSelectedStock] = useState(null);

    // Function to handle stock selection from the table
    const handleStockSelect = (stock) => {
        setSelectedStock(stock);
    };

    return (
        <div className="stock-trading-container">
            {/* Your existing StockTable component with selection handler */}
            <StockTable onSelectStock={handleStockSelect} />

            {/* The new BuyOrderForm component with stock data */}
            <BuyOrderForm stockData={selectedStock} />
        </div>
    );
};

export default StockTableWithOrderForm;