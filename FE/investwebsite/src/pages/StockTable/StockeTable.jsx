import React, { useState, useEffect, useRef } from 'react';
import './StockTable.css';

// Simple sparkline chart component
const Sparkline = ({ data, color, fillColor, type = 'line', height = 50, width = 150 }) => {
    const canvasRef = useRef(null);

    useEffect(() => {
        if (!data || !data.length || !canvasRef.current) return;

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        const values = type === 'volume' ? data.map(point => point.volume) : data.map(point => point.price);

        // Clear canvas
        ctx.clearRect(0, 0, width, height);

        // Find min and max for scaling
        const min = Math.min(...values);
        const max = Math.max(...values);
        const range = max - min || 1; // Avoid division by zero

        // Set line style
        ctx.strokeStyle = color;
        ctx.lineWidth = 1.5;
        ctx.beginPath();

        // Draw sparkline
        values.forEach((value, i) => {
            const x = (i / (values.length - 1)) * width;
            const y = height - ((value - min) / range) * height;

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
    }, [data, color, fillColor, type, height, width]);

    return <canvas ref={canvasRef} height={height} width={width} />;
};

const StockTable = () => {
    const [stocks, setStocks] = useState([]);
    const [filteredStocks, setFilteredStocks] = useState([]);
    const [filter, setFilter] = useState('');
    const [timePeriod, setTimePeriod] = useState('day');
    const [currentPage, setCurrentPage] = useState(1);
    const [stocksHistory, setStocksHistory] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const websocket = useRef(null);
    const stocksPerPage = 10;

    // Connect to WebSocket when component mounts
    useEffect(() => {
        // Connect to WebSocket server
        const ws = new WebSocket('ws://localhost:8080/ws/market-data');
        websocket.current = ws;

        ws.onopen = () => {
            console.log('Connected to market data server');
        };

        ws.onmessage = (event) => {
            const data = JSON.parse(event.data);

            if (data.type === 'initialData') {
                setStocks(data.stocks);
                setStocksHistory(data.history || {});
                setLoading(false);
            } else if (data.type === 'update') {
                updateStockData(data.symbol, data.data, data.history);
            } else if (data.type === 'filteredData') {
                setFilteredStocks(data.stocks);
            }
        };

        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            setError('Failed to connect to market data server');
            setLoading(false);
        };

        ws.onclose = () => {
            console.log('Disconnected from market data server');
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

    // Handle time period change
    const handleTimePeriodChange = (period) => {
        setTimePeriod(period);
    };

    // Calculate pagination
    const indexOfLastStock = currentPage * stocksPerPage;
    const indexOfFirstStock = indexOfLastStock - stocksPerPage;
    const currentStocks = filteredStocks.slice(indexOfFirstStock, indexOfLastStock);
    const totalPages = Math.ceil(filteredStocks.length / stocksPerPage);

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
                    <button
                        className={timePeriod === 'day' ? 'active' : ''}
                        onClick={() => handleTimePeriodChange('day')}
                    >
                        Day
                    </button>
                    <button
                        className={timePeriod === 'week' ? 'active' : ''}
                        onClick={() => handleTimePeriodChange('week')}
                    >
                        Week
                    </button>
                    <button
                        className={timePeriod === 'month' ? 'active' : ''}
                        onClick={() => handleTimePeriodChange('month')}
                    >
                        Month
                    </button>
                </div>
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
                        disabled={currentPage === 1}
                        onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                    >
                        Previous
                    </button>

                    <div className="page-numbers">
                        {currentPage} of {totalPages}
                    </div>

                    <button
                        disabled={currentPage === totalPages}
                        onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                    >
                        Next
                    </button>
                </div>
            )}
        </div>
    );
};

export default StockTable;