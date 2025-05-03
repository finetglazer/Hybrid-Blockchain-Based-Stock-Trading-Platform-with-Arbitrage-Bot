import React, { useState, useEffect, useRef } from 'react';
import './StockTable.css';

// Modified Sparkline component with vertical line indicator
const Sparkline = ({ data, color, fillColor, type = 'line', height = 50, width = 150 }) => {
    const canvasRef = useRef(null);
    const tooltipRef = useRef(null);
    const [tooltip, setTooltip] = useState({ visible: false, x: 0, y: 0, value: null, date: null });

    useEffect(() => {
        if (!data || !data.length || !canvasRef.current) return;

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        const values = type === 'volume' ? data.map(point => point.volume) : data.map(point => point.price);
        const dates = data.map(point => point.timestamp ? new Date(point.timestamp) : null);

        // Clear canvas
        ctx.clearRect(0, 0, width, height);

        // Find min and max for scaling
        const min = Math.min(...values);
        const max = Math.max(...values);
        const range = max - min || 1; // Avoid division by zero

        // Store point coordinates for tooltip interaction
        const points = [];

        // Set line style
        ctx.strokeStyle = color;
        ctx.lineWidth = 1.5;
        ctx.beginPath();

        // Draw sparkline
        values.forEach((value, i) => {
            const x = (i / (values.length - 1)) * width;
            const y = height - ((value - min) / range) * height;

            // Store coordinates and values for tooltip
            points.push({ x, y, value, date: dates[i] });

            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        });

        ctx.stroke();

        // Add fill if specified
        if (fillColor) {
            ctx.lineTo(width, height);
            ctx.lineTo(0, height);
            ctx.closePath();
            ctx.fillStyle = fillColor;
            ctx.globalAlpha = 0.3;
            ctx.fill();
            ctx.globalAlpha = 1.0;
        }

        // Add mouse event listeners
        const handleMouseMove = (e) => {
            const rect = canvas.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;

            // Find closest point
            let closestPoint = null;
            let closestDistance = Infinity;

            points.forEach(point => {
                const distance = Math.abs(point.x - mouseX);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPoint = point;
                }
            });

            // Only show tooltip if we're reasonably close to a point
            if (closestDistance < 10 && closestPoint) {
                setTooltip({
                    visible: true,
                    x: closestPoint.x,
                    y: closestPoint.y,
                    value: closestPoint.value,
                    date: closestPoint.date
                });
            } else {
                setTooltip(prev => ({ ...prev, visible: false }));
            }
        };

        const handleMouseOut = () => {
            setTooltip(prev => ({ ...prev, visible: false }));
        };

        canvas.addEventListener('mousemove', handleMouseMove);
        canvas.addEventListener('mouseout', handleMouseOut);

        return () => {
            canvas.removeEventListener('mousemove', handleMouseMove);
            canvas.removeEventListener('mouseout', handleMouseOut);
        };
    }, [data, color, fillColor, type, height, width]);

    // Format value for display
    const formatValue = (value) => {
        if (type === 'volume') {
            return Number(value).toLocaleString();
        } else {
            return Number(value).toFixed(2);
        }
    };

    // Format date for display
    const formatDate = (date) => {
        if (!date) return '';
        return date.toISOString().split('T')[0];
    };

    return (
        <div style={{ position: 'relative', width, height }}>
            <canvas ref={canvasRef} height={height} width={width} />

            {tooltip.visible && (
                <>
                    {/* Vertical red line indicator */}
                    <div
                        style={{
                            position: 'absolute',
                            left: `${tooltip.x}px`,
                            top: 0,
                            width: '1px',
                            height: '100%',
                            backgroundColor: 'red',
                            pointerEvents: 'none',
                            zIndex: 5
                        }}
                    />

                    {/* Tooltip */}
                    <div
                        ref={tooltipRef}
                        style={{
                            position: 'absolute',
                            left: `${tooltip.x}px`,
                            top: `${tooltip.y - 40}px`,
                            background: '#222',
                            color: 'white',
                            padding: '4px 8px',
                            borderRadius: '3px',
                            fontSize: '12px',
                            pointerEvents: 'none',
                            transform: 'translateX(-50%)',
                            zIndex: 10,
                            border: '1px solid #444'
                        }}
                    >
                        {type === 'volume' ? (
                            <div>{formatValue(tooltip.value)}</div>
                        ) : (
                            <div>
                                <div>{formatValue(tooltip.value)}</div>
                                <div>{formatDate(tooltip.date)}</div>
                            </div>
                        )}
                    </div>
                </>
            )}
        </div>
    );
};

const StockTable = () => {
    const [stocks, setStocks] = useState([]);
    const [filteredStocks, setFilteredStocks] = useState([]);
    const [filter, setFilter] = useState('');
    const [currentPage, setCurrentPage] = useState(1);
    const [stocksHistory, setStocksHistory] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const websocket = useRef(null);
    const stocksPerPage = 10;

    // Connect to WebSocket when component mounts
    useEffect(() => {
        // Connect to WebSocket server
        console.log("Attempting to connect to WebSocket server...");
        const ws = new WebSocket('ws://localhost:8080/ws/market-data');
        websocket.current = ws;

        ws.onopen = (event) => {
            console.log('Connected to market data server', event);
            setError(null); // Clear any previous errors
        };

        ws.onmessage = (event) => {
            console.log('Received message from server');
            try {
                const data = JSON.parse(event.data);
                console.log('Parsed data type:', data.type);

                if (data.type === 'initialData') {
                    console.log('Received initial data with stocks:', data.stocks?.length || 0);
                    setStocks(data.stocks || []);
                    setStocksHistory(data.history || {});
                    setLoading(false);
                } else if (data.type === 'update') {
                    updateStockData(data.symbol, data.data, data.history);
                } else if (data.type === 'filteredData') {
                    setFilteredStocks(data.stocks);
                }
            } catch (err) {
                console.error('Error parsing WebSocket message:', err);
                setError('Failed to process market data');
            }
        };

        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            setError('Failed to connect to market data server');
            setLoading(false);
        };

        ws.onclose = (event) => {
            console.log('WebSocket closed with code:', event.code, 'reason:', event.reason);
            if (event.code !== 1000) { // Normal closure
                setError(`Connection closed unexpectedly. Code: ${event.code}, Reason: ${event.reason}`);
            }
        };

        // Clean up WebSocket connection on unmount
        return () => {
            if (websocket.current) {
                websocket.current.close();
            }
        };
    }, []);

    // Update filtered stocks when stocks or filter changes
    useEffect(() => {
        const filtered = stocks.filter(stock => {
            return (
                stock.symbol.toLowerCase().includes(filter.toLowerCase()) ||
                stock.name.toLowerCase().includes(filter.toLowerCase())
            );
        });
        setFilteredStocks(filtered);
    }, [stocks, filter]);

    // Update a single stock in the stocks array
    const updateStockData = (symbol, newData, history) => {
        setStocks(prevStocks => {
            const stockIndex = prevStocks.findIndex(stock => stock.symbol === symbol);

            if (stockIndex === -1) {
                return [...prevStocks, newData];
            }

            const updatedStocks = [...prevStocks];
            updatedStocks[stockIndex] = { ...updatedStocks[stockIndex], ...newData };
            return updatedStocks;
        });

        // Update history if provided
        if (history) {
            setStocksHistory(prev => ({
                ...prev,
                [symbol]: history
            }));
        }
    };

    // Handle filter input change
    const handleFilterChange = (e) => {
        setFilter(e.target.value);
        setCurrentPage(1); // Reset to first page when filtering

        // Send filter request to server
        if (websocket.current && websocket.current.readyState === WebSocket.OPEN) {
            websocket.current.send(JSON.stringify({
                filter: e.target.value
            }));
        }
    };

    // Calculate pagination
    const indexOfLastStock = currentPage * stocksPerPage;
    const indexOfFirstStock = indexOfLastStock - stocksPerPage;
    const currentStocks = filteredStocks.slice(indexOfFirstStock, indexOfLastStock);
    const totalPages = Math.ceil(filteredStocks.length / stocksPerPage);

    // Generate page numbers for pagination
    const getPageNumbers = () => {
        const pageNumbers = [];
        const maxPagesToShow = 5; // Show max 5 page numbers at once

        if (totalPages <= maxPagesToShow) {
            // Less than maxPagesToShow pages - show all
            for (let i = 1; i <= totalPages; i++) {
                pageNumbers.push(i);
            }
        } else {
            // More than maxPagesToShow pages - show current with neighbors
            const startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
            const endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);

            if (startPage > 1) {
                pageNumbers.push(1);
                if (startPage > 2) pageNumbers.push('...'); // ellipsis
            }

            for (let i = startPage; i <= endPage; i++) {
                pageNumbers.push(i);
            }

            if (endPage < totalPages) {
                if (endPage < totalPages - 1) pageNumbers.push('...'); // ellipsis
                pageNumbers.push(totalPages);
            }
        }

        return pageNumbers;
    };

    // Format percent change
    const formatPercentChange = (change) => {
        if (!change && change !== 0) return '0.00 %';
        const sign = change >= 0 ? '+' : '';
        return `${sign}${change.toFixed(2)} %`;
    };

    // Get CSS class for percent change
    const getChangeClass = (change) => {
        if (!change && change !== 0) return 'neutral';
        return change >= 0 ? 'positive' : 'negative';
    };

    // Update this function in StockTable.jsx
    const formatDetailedTime = (timestamp) => {
        if (!timestamp) return '';

        try {
            const date = new Date(timestamp);
            return date.toLocaleString('en-US', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
                // Removed the hour, minute, second options
            });
        } catch (e) {
            console.error('Error formatting timestamp:', e);
            return timestamp;
        }
    };

    if (loading) {
        return <div className="loading">Loading stock data...</div>;
    }

    if (error) {
        return <div className="error">{error}</div>;
    }

    return (
        <div className="stock-table-container">
            <div className="stock-controls">
                <div className="search-container">
                    <input
                        type="text"
                        placeholder="Search stocks..."
                        value={filter}
                        onChange={handleFilterChange}
                        className="search-input"
                    />
                </div>
                <div className="time-period">
                    <div className="time-label">Daily View</div>
                </div>
            </div>

            <div className="chart-headers">
                <div className="chart-header-space"></div>
                <div className="chart-header-label">VOLUME</div>
                <div className="chart-header-label">PRICE</div>
                <div className="chart-header-space"></div>
            </div>

            <div className="stock-table">
                {currentStocks.length === 0 ? (
                    <div className="no-stocks">No stocks found</div>
                ) : (
                    currentStocks.map(stock => {
                        const history = stocksHistory[stock.symbol] || [];
                        const priceHistory = history.length > 0 ? history : [{ price: stock.price, volume: 0 }];
                        const isPositive = stock.changePercent >= 0;

                        return (
                            <div className="stock-row" key={stock.symbol}>
                                <div className="stock-info">
                                    <div className="stock-symbol">{stock.symbol}</div>
                                    <div className="stock-name">{stock.name}</div>
                                    <div className="stock-time">{formatDetailedTime(stock.timestamp)}</div>
                                </div>
                                <div className="stock-chart">
                                    <Sparkline
                                        data={priceHistory}
                                        type="volume"
                                        color="#ffc00d"
                                        height={50}
                                        width={150}
                                    />
                                </div>
                                <div className="stock-chart">
                                    <Sparkline
                                        data={priceHistory}
                                        color={isPositive ? '#0ddd0d' : 'red'}
                                        fillColor={isPositive ? '#193b05c4' : '#680000'}
                                        height={50}
                                        width={150}
                                    />
                                </div>
                                <div className={`stock-change ${getChangeClass(stock.changePercent)}`}>
                                    {formatPercentChange(stock.changePercent)}
                                </div>
                            </div>
                        );
                    })
                )}
            </div>

            {totalPages > 1 && (
                <div className="pagination">
                    <button
                        className="prev-btn"
                        disabled={currentPage === 1}
                        onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                    >
                        Previous
                    </button>

                    <div className="page-numbers">
                        {getPageNumbers().map((pageNum, index) =>
                            typeof pageNum === 'number' ? (
                                <button
                                    key={index}
                                    className={`${currentPage === pageNum ? 'active' : ''} 
                        ${pageNum === 2 && currentPage === 1 ? 'next-page-blur' : ''}`}
                                    onClick={() => setCurrentPage(pageNum)}
                                >
                                    {pageNum}
                                </button>
                            ) : (
                                <span key={index} className="ellipsis">{pageNum}</span>
                            )
                        )}
                    </div>

                    <button
                        className="next-btn"
                        disabled={currentPage === totalPages}
                        onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                    >
                        Next
                    </button>
                </div>
            )}

            <div className="table-info">
                {filteredStocks.length > 0 && (
                    <span>Showing {indexOfFirstStock + 1}-{Math.min(indexOfLastStock, filteredStocks.length)} of {filteredStocks.length} stocks</span>
                )}
            </div>
        </div>
    );
};

export default StockTable;