import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
    Descriptions,
    Spin,
    Alert,
    Tag,
    Breadcrumb,
    Button,
    Card,
    Typography,
    Space,
    Empty
} from 'antd';
import { LineChart, Line, ResponsiveContainer } from 'recharts';
import useWebSocket from 'react-use-websocket';

const { Title } = Typography;

// --- Reusable Helper Functions (unchanged) ---
const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    try {
        const date = isNaN(Number(dateString))
            ? new Date(dateString)
            : new Date(Number(dateString) * 1000);
        return date.toLocaleString('sv-SE');
    } catch (e) { return e.message; }
};
const formatPrice = (price) => {
    if (price === null || price === undefined) return "N/A";
    return `$${Number(price).toFixed(2)}`;
};
const getStatusTagColor = (status) => {
    switch (status) {
        case "FILLED": case "COMPLETED": return "success";
        case "PARTIALLY_FILLED": case "EXECUTING": return "processing";
        case "CREATED": case "VALIDATED": return "blue";
        case "CANCELLED": case "REJECTED": case "FAILED": case "EXPIRED": return "error";
        default: return "default";
    }
};


const OrderDetailsPage = () => {
    const { orderId } = useParams();
    const navigate = useNavigate();

    const [orderData, setOrderData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [priceHistory, setPriceHistory] = useState([]);
    const [socketUrl, setSocketUrl] = useState(null);

    // --- Effect for fetching Order Details (HTTP) ---
    useEffect(() => {
        if (!orderId) {
            setError("Order ID is missing.");
            setLoading(false);
            return;
        }
        setOrderData(null);
        setPriceHistory([]);
        setError(null);
        setLoading(true);
        setSocketUrl(null);

        const fetchOrderDetails = async () => {
            try {
                const response = await fetch(`/orders/api/v1/${orderId}`, {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        'Authorization': `Bearer ${localStorage.getItem("token")}`
                    }
                });
                if (!response.ok) { throw new Error(`HTTP error! status: ${response.status}`); }
                const result = await response.json();
                if (result.status === 1 && result.data) {
                    setOrderData(result.data);
                } else { throw new Error(result.msg || "Order not found."); }
            } catch (err) { setError(err.message || "Error fetching details."); }
            finally { setLoading(false); }
        };

        fetchOrderDetails();

    }, [orderId]);

    // --- Effect to set WebSocket URL when orderData is available ---
    useEffect(() => {
        if (orderData && orderData.stockSymbol) {
            const token = localStorage.getItem("token");
            if (token) {
                const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                const wsHost = window.location.host;
                const newUrl = `${wsProtocol}//${wsHost}/ws/market-data?token=${token}`;
                // console.log(`Setting up Market Data WebSocket for symbol: ${orderData.stockSymbol}. URL: ${newUrl}`);
                setSocketUrl(newUrl);
            } else {
                console.warn("Token not found for Market Data WebSocket.");
                setSocketUrl(null);
            }
        } else {
            if (socketUrl !== null) {
                // console.log("Clearing WebSocket URL as orderData or symbol is not available.");
                setSocketUrl(null);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [orderData]);

    // --- WebSocket Connection using react-use-websocket ---
    const { lastMessage, readyState } = useWebSocket(socketUrl, {
        onOpen: () => {
            const currentSymbol = orderData?.stockSymbol || 'unknown symbol (onOpen)';
            console.log(`Market Data WebSocket Connected for ${currentSymbol}`);
        },
        onClose: (event) => {
            const currentSymbol = orderData?.stockSymbol || 'unknown symbol (onClose)';
            console.log(`Market Data WebSocket Disconnected for ${currentSymbol}:`, event.reason, event.code);
        },
        onError: (error) => {
            const currentSymbol = orderData?.stockSymbol || 'unknown symbol (onError)';
            console.error(`Market Data WebSocket Error for ${currentSymbol}:`, error);
        },
        shouldReconnect: (closeEvent) => true,
        retryOnError: true,
    });

    // --- Effect for processing WebSocket messages ---
    useEffect(() => {
        if (!lastMessage?.data || !orderData?.stockSymbol) {
            return; // Essential data missing
        }

        const currentOrderSymbol = orderData.stockSymbol;

        try {
            const parsedMessage = JSON.parse(lastMessage.data);

            // Early exit if the message is not for the current order's symbol,
            // unless it's a general "initialData" type that might contain it.
            if (parsedMessage.symbol !== currentOrderSymbol && parsedMessage.type !== "initialData") {
                return;
            }

            let historyForChart = null;

            // Priority 1: Message directly contains a "history" array for the current symbol.
            if (parsedMessage.symbol === currentOrderSymbol && Array.isArray(parsedMessage.history)) {
                historyForChart = parsedMessage.history
                    .map(item => item?.price)
                    .filter(price => typeof price === 'number');

                if (historyForChart.length > 0) {
                    console.log(`Applied direct history for ${currentOrderSymbol} from '${parsedMessage.type}' message with ${historyForChart.length} points.`);
                    setPriceHistory(historyForChart);
                } else {
                    console.warn(`Direct history for ${currentOrderSymbol} in '${parsedMessage.type}' message was empty or invalid. Clearing chart.`);
                    setPriceHistory([]); // Clear chart if history is explicitly empty
                }
                // If history is provided, we assume it's the most complete data for the chart from this message.
                // We might not need to append data.price from the same message if it's already the last point.
                return; // History processed
            }

            // Priority 2: "initialData" type with a nested history object for the current symbol.
            if (parsedMessage.type === "initialData" && parsedMessage.history && parsedMessage.history[currentOrderSymbol]) {
                const symbolSpecificHistory = parsedMessage.history[currentOrderSymbol];
                if (Array.isArray(symbolSpecificHistory)) {
                    historyForChart = symbolSpecificHistory
                        .map(item => item?.price)
                        .filter(price => typeof price === 'number');

                    if (historyForChart.length > 0) {
                        console.log(`Loaded initial history for ${currentOrderSymbol} with ${historyForChart.length} points (from 'initialData').`);
                        setPriceHistory(historyForChart);
                    } else {
                        console.warn(`Initial history for ${currentOrderSymbol} in 'initialData' was empty/invalid. Clearing chart.`);
                        setPriceHistory([]);
                    }
                    return; // History processed
                }
            }

            // Priority 3: Live tick update (if no complete history was processed from this message for this symbol).
            // This appends to existing history.
            if (parsedMessage.symbol === currentOrderSymbol && parsedMessage.data && typeof parsedMessage.data.price === 'number') {
                const newPrice = parsedMessage.data.price;
                // console.log(`Considering live price update for ${currentOrderSymbol}: ${newPrice}`);
                setPriceHistory(prevHistory => {
                    // Avoid duplicate if newPrice is already the last element
                    if (prevHistory.length > 0 && prevHistory[prevHistory.length - 1] === newPrice) {
                        return prevHistory;
                    }
                    // console.log(`Appending live price ${newPrice} to history for ${currentOrderSymbol}.`);
                    const updatedHistory = [...prevHistory, newPrice];
                    // Optional: Cap history length
                    // const MAX_PRICE_POINTS = 200;
                    // if (updatedHistory.length > MAX_PRICE_POINTS) {
                    //     return updatedHistory.slice(-MAX_PRICE_POINTS);
                    // }
                    return updatedHistory;
                });
                // No return here, as this can coexist with other non-history updates in a message
            }

        } catch (e) {
            console.error("Failed to parse WebSocket message or process data:", e, lastMessage.data);
        }
        // Using orderData.stockSymbol directly in dependency array for precision
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [lastMessage, orderData?.stockSymbol]);


    // --- Render Functions ---
    const renderOrderDetails = () => {
        if (!orderData) return null;

        const chartData = priceHistory.map((price, index) => ({
            index: index,
            price: price
        }));

        return (
            <Descriptions bordered column={{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }} layout="horizontal">
                {/* Core Order Info (unchanged) */}
                <Descriptions.Item label="Order ID" span={2}>{orderData.id}</Descriptions.Item>
                <Descriptions.Item label="Stock Symbol">{orderData.stockSymbol}</Descriptions.Item>
                <Descriptions.Item label="Status">
                    <Tag color={getStatusTagColor(orderData.status)}>{orderData.status || 'UNKNOWN'}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Side">
                    <Tag color={orderData.side === "BUY" ? "green" : "red"}>{orderData.side}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Order Type">
                    <Tag>{orderData.orderType}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Quantity Ordered">{orderData.quantity ?? 'N/A'}</Descriptions.Item>
                <Descriptions.Item label="Quantity Executed">{orderData.executedQuantity ?? 'N/A'}</Descriptions.Item>
                {orderData.orderType === 'LIMIT' && (
                    <Descriptions.Item label="Limit Price">{formatPrice(orderData.limitPrice)}</Descriptions.Item>
                )}
                <Descriptions.Item label="Average Execution Price">{formatPrice(orderData.executionPrice)}</Descriptions.Item>
                <Descriptions.Item label="Total Value">{formatPrice(orderData.totalValue)}</Descriptions.Item>
                <Descriptions.Item label="Time In Force">{orderData.timeInForce ?? 'N/A'}</Descriptions.Item>
                <Descriptions.Item label="Created At">{formatDate(orderData.createdAt)}</Descriptions.Item>
                <Descriptions.Item label="Last Updated At">{formatDate(orderData.updatedAt)}</Descriptions.Item>
                <Descriptions.Item label="Executed At">{formatDate(orderData.executedAt)}</Descriptions.Item>
                <Descriptions.Item label="Cancelled At">{formatDate(orderData.cancelledAt)}</Descriptions.Item>
                <Descriptions.Item label="Account ID">{orderData.accountId ?? 'N/A'}</Descriptions.Item>
                <Descriptions.Item label="User ID">{orderData.userId ?? 'N/A'}</Descriptions.Item>
                <Descriptions.Item label="Broker Order ID">{orderData.brokerOrderId ?? 'N/A'}</Descriptions.Item>
                {orderData.rejectionReason && (
                    <Descriptions.Item label="Rejection Reason" span={2}>
                        <Alert message={orderData.rejectionReason} type="warning" showIcon style={{ padding: '2px 8px' }} />
                    </Descriptions.Item>
                )}
                {/* Price History Chart */}
                <Descriptions.Item label="Price History (Recent)" span={2}>
                    {chartData.length > 1 ? (
                        <div style={{ height: '60px', width: '100%' }}>
                            <ResponsiveContainer>
                                <LineChart
                                    data={chartData}
                                    margin={{ top: 5, right: 5, bottom: 5, left: 5 }}
                                >
                                    <Line
                                        type="monotone"
                                        dataKey="price"
                                        stroke="#5DADE2"
                                        strokeWidth={2}
                                        dot={false}
                                        isAnimationActive={false}
                                    />
                                </LineChart>
                            </ResponsiveContainer>
                        </div>
                    ) : (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={
                            loading && !orderData ? "Loading order details..." : // Initial HTTP load
                                readyState === 0 ? "Connecting to market data..." : // WebSocket.CONNECTING
                                    readyState === 1 && chartData.length <=1 ? "Awaiting sufficient price history..." : // WebSocket.OPEN, but no data yet
                                        readyState === 3 ? "Market data disconnected. History may be incomplete." : // WebSocket.CLOSED
                                            "Price history insufficient or unavailable"
                        } />
                    )}
                </Descriptions.Item>
            </Descriptions>
        );
    };

    // --- Main Return JSX (unchanged structure) ---
    return (
        <div className="container font-opensans min-w-[100vw] min-h-[100vh] p-4 md:p-8 bg-gray-900 text-white">
            <Breadcrumb
                className="mb-4"
                separator=">"
                items={[
                    { title: <Link to="/home" style={{ color: "rgba(255, 255, 255, 0.6)" }}>Home</Link> },
                    { title: <Link to="/order-history" style={{ color: "rgba(255, 255, 255, 0.6)" }}>Order History</Link> },
                    { title: <span style={{ color: "rgba(255, 255, 255, 1)" }}>Order Details</span> },
                ]}
            />
            <Title level={2} style={{ color: 'white', marginTop: '20px', marginBottom: '20px' }}>
                Order Details
            </Title>

            <Spin spinning={loading && !orderData} tip="Loading order details...">
                {error && (
                    <Alert
                        message="Error"
                        description={error}
                        type="error"
                        showIcon
                        closable
                        className="mb-4"
                        onClose={() => setError(null)}
                    />
                )}
                {!loading && !error && orderData && (
                    <Card style={{ background: '#1f2937' }}>
                        {renderOrderDetails()}
                        <Space style={{ marginTop: '20px' }}>
                            <Button onClick={() => navigate(-1)}>Back</Button>
                        </Space>
                    </Card>
                )}
                {!loading && !error && !orderData && (
                    <Alert message="Order not found or data is unavailable." type="warning" showIcon />
                )}
            </Spin>
        </div>
    );
};

export default OrderDetailsPage;
