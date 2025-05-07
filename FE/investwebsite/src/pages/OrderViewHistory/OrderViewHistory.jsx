// Import Space for button layout
import {
    Breadcrumb,
    Table,
    Tag,
    Spin,
    Alert,
    Row, // Import Row
    Col, // Import Col
    Select, // Import Select
    DatePicker, // Import DatePicker
    Button, // Import Button
    Space,
} from "antd";
import React, { useState, useEffect, useRef, useCallback } from "react";
import dayjs from 'dayjs';
import {useNavigate} from "react-router-dom"; // Antd v5 uses dayjs for DatePicker

// --- Constants for Filter Options ---
const TIME_IN_FORCES = ["DAY", "GTC"];
const ORDER_TYPES = ["MARKET", "LIMIT"];
const STATUSES = [
    "CREATED", "VALIDATED", "REJECTED", "CANCELLED", "EXPIRED",
    "EXECUTING", "PARTIALLY_FILLED", "FILLED", "COMPLETED", "FAILED",
];
const SIDES = ["BUY", "SELL"];
const STOCK_SYMBOLS = [
    "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META",
    "NVDA", "JPM", "V", "JNJ", "ABBV", "WMT", "PG", "MA", "UNH"
];

// --- Helper Functions (Keep from previous version) ---
const formatDate = (dateString) => {
    // ... (same as before)
    if (!dateString) return "-";
    try {
        const date = isNaN(Number(dateString))
            ? new Date(dateString)
            : new Date(Number(dateString) * 1000); // Assuming WS timestamp is in seconds
        // Let's format as YYYY-MM-DD HH:mm:ss for clarity in the table
        return date.toLocaleString('sv-SE'); // Swedish locale often gives YYYY-MM-DD HH:MM:SS
    } catch (e) {
        return e.message;
    }
};
const formatPrice = (price) => {
    // ... (same as before)
    if (price === null || price === undefined) return "-";
    return `$${Number(price).toFixed(2)}`;
};
// Helper to format date range for API (adjust format if needed)
const formatApiDate = (date) => {
    if (!date) return "";
    // Example: Return YYYY-MM-DD. Adjust if API needs time or ISO string
    // return date.format('YYYY-MM-DD');
    // If API needs full ISO string at start/end of day:
    return date.toISOString(); // Or date.startOf('day').toISOString() / date.endOf('day').toISOString()
};

const OrderViewHistory = () => {
    const navigate = useNavigate();

    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const ws = useRef(null);

    // --- State for Filters ---
    const [selectedDates, setSelectedDates] = useState([null, null]); // [startDate, endDate] using dayjs
    const [selectedTimeInForces, setSelectedTimeInForces] = useState([]);
    const [selectedOrderTypes, setSelectedOrderTypes] = useState([]);
    const [selectedStatuses, setSelectedStatuses] = useState([]);
    const [selectedSides, setSelectedSides] = useState([]);
    const [selectedStockSymbols, setSelectedStockSymbols] = useState([]);

    // --- Modified Fetch Function ---
    const fetchOrders = useCallback(async (filters) => {
        setLoading(true);
        setError(null); // Clear previous errors on new fetch
        console.log("Fetching orders with filters:", filters); // Log filters being used

        try {
            const response = await fetch("/orders/api/v1/get", { // Adjust URL if needed
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    'Authorization': `Bearer ${localStorage.getItem("token")}`
                },
                body: JSON.stringify({
                    accountIds: [], // Dismissed as requested
                    startDate: filters.startDate || "",
                    endDate: filters.endDate || "",
                    timeInForces: filters.timeInForces || [],
                    orderTypes: filters.orderTypes || [],
                    statuses: filters.statuses || [],
                    sides: filters.sides || [],
                    stockSymbols: filters.stockSymbols || [],
                    page: 0, // Keep pagination params
                    size: 9999,
                }),
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();

            if (result.status === 1 && result.data && result.data.orders) {
                const ordersWithKeys = result.data.orders.map(order => ({ ...order, key: order.id }));
                // Sort by date descending after fetching/filtering
                ordersWithKeys.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                setOrders(ordersWithKeys);

            } else {
                throw new Error(result.msg || "Failed to fetch orders");
            }
        } catch (err) {
            console.error("Failed to fetch orders:", err);
            setError(err.message || "An error occurred while fetching order history.");
            setOrders([]); // Clear orders on error
        } finally {
            setLoading(false);
        }
    }, []); // useCallback dependency array is empty as fetch itself doesn't depend on component state

    // --- Initial Fetch on Mount ---
    useEffect(() => {
        // Fetch all orders initially
        fetchOrders({}); // Pass empty filters object
    }, [fetchOrders]); // Depend on the memoized fetchOrders function

    // --- WebSocket Setup (Keep from previous version) ---
    useEffect(() => {
        const token = localStorage.getItem("token");
        if (!token) {
            console.warn("Token not found for WebSocket.");
            // setError("Authentication token not found. Real-time updates disabled."); // Maybe too intrusive
            return;
        }

        const wsUrl = `https://good-musical-joey.ngrok-free.app/ws/orders?token=${token}`;

        console.log("Attempting to connect WebSocket:", wsUrl);
        ws.current = new WebSocket(wsUrl);

        ws.current.onopen = () => console.log("WebSocket Connected");
        ws.current.onerror = (error) => console.error("WebSocket Error:", error);
        ws.current.onclose = (event) => console.log("WebSocket Disconnected:", event.reason, event.code);

        ws.current.onmessage = (event) => {
            try {
                const updatedOrder = JSON.parse(event.data);
                console.log("WebSocket message received:", updatedOrder);

                setOrders((prevOrders) => {
                    const index = prevOrders.findIndex((order) => order.id === updatedOrder.id);
                    const newOrders = [...prevOrders];
                    const orderWithKey = { ...updatedOrder, key: updatedOrder.id };

                    if (index > -1) {
                        newOrders[index] = orderWithKey;
                    } else {
                        newOrders.unshift(orderWithKey); // Add new order to the top
                    }
                    // Re-sort after WebSocket update
                    newOrders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                    return newOrders;
                });
            } catch (e) {
                console.error("Failed to parse WebSocket message or update state:", e);
            }
        };

        return () => {
            if (ws.current && ws.current.readyState === WebSocket.OPEN) {
                console.log("Closing WebSocket connection");
                ws.current.close();
            }
        };
    }, []); // Empty dependency: Run only once on mount

    // --- Handlers for Filter Actions ---
    const handleApplyFilters = () => {
        const filters = {
            // Use startOf('day') and endOf('day') if API expects full day ranges based on selected date
            // startDate: selectedDates[0] ? selectedDates[0].startOf('day').toISOString() : "",
            // endDate: selectedDates[1] ? selectedDates[1].endOf('day').toISOString() : "",
            startDate: selectedDates[0] ? formatApiDate(selectedDates[0]) : "",
            endDate: selectedDates[1] ? formatApiDate(selectedDates[1]) : "",
            timeInForces: selectedTimeInForces,
            orderTypes: selectedOrderTypes,
            statuses: selectedStatuses,
            sides: selectedSides,
            stockSymbols: selectedStockSymbols,
        };
        fetchOrders(filters);
    };

    const handleResetFilters = () => {
        setSelectedDates([null, null]);
        setSelectedTimeInForces([]);
        setSelectedOrderTypes([]);
        setSelectedStatuses([]);
        setSelectedSides([]);
        setSelectedStockSymbols([]);
        fetchOrders({}); // Fetch all orders again
    };

    // --- Table Columns (Keep from previous version) ---
    const columns = [
        {
            title: "Date",
            dataIndex: "createdAt",
            key: "createdAt",
            render: (text) => formatDate(text),
            sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt),
            // defaultSortOrder: 'descend', // Sorting is now handled after fetch/update
        },
        {
            title: "Symbol",
            dataIndex: "stockSymbol",
            key: "stockSymbol",
            filters: STOCK_SYMBOLS.map(s => ({ text: s, value: s })),
            onFilter: (value, record) => record.stockSymbol === value, // Simple client-side filter example
        },
        {
            title: "Type",
            dataIndex: "orderType",
            key: "orderType",
            filters: ORDER_TYPES.map(t => ({ text: t, value: t })),
            onFilter: (value, record) => record.orderType === value,
        },
        {
            title: "Side",
            dataIndex: "side",
            key: "side",
            render: (side) => (<Tag color={side === "BUY" ? "green" : "red"}>{side}</Tag>),
            filters: SIDES.map(s => ({ text: s, value: s })),
            onFilter: (value, record) => record.side === value,
        },
        { title: "Quantity", dataIndex: "quantity", key: "quantity", align: 'right' },
        { title: "Limit Price", dataIndex: "limitPrice", key: "limitPrice", render: formatPrice, align: 'right' },
        { title: "Exec Qty", dataIndex: "executedQuantity", key: "executedQuantity", render: (qty) => qty ?? "-", align: 'right' },
        { title: "Avg Exec Price", dataIndex: "executionPrice", key: "executionPrice", render: formatPrice, align: 'right' },
        { title: "Total Value", dataIndex: "totalValue", key: "totalValue", render: formatPrice, align: 'right' },
        {
            title: "Status",
            dataIndex: "status",
            key: "status",
            render: (status) => {
                let color = "default";
                switch (status) {
                    case "FILLED": case "COMPLETED": color = "success"; break;
                    case "PARTIALLY_FILLED": case "EXECUTING": color = "processing"; break;
                    case "CREATED": case "VALIDATED": color = "blue"; break;
                    case "CANCELLED": case "REJECTED": case "FAILED": case "EXPIRED": color = "error"; break;
                    default: color = "default";
                }
                return <Tag color={color}>{status || 'UNKNOWN'}</Tag>;
            },
            filters: STATUSES.map(s => ({ text: s, value: s })),
            onFilter: (value, record) => record.status === value,
        },
        { title: "Exec At", dataIndex: "executedAt", key: "executedAt", render: formatDate },
        { title: "TIF", dataIndex: "timeInForce", key: "timeInForce", filters: TIME_IN_FORCES.map(t => ({text: t, value: t})), onFilter: (v, r) => r.timeInForce === v },
        // { title: "Rejection Reason", dataIndex: "rejectionReason", key: "rejectionReason", render: (text) => text ?? "-", width: 150, ellipsis: true }, // Optional: Add if needed
    ];

    return (
        <>
            {/* Ensure container has padding */}
            <div className="container font-opensans min-w-[90vw] min-h-[100vh] p-4 md:p-8 bg-gray-900 text-white"> {/* Added bg/text for contrast */}
                <Breadcrumb
                    className="mb-4"
                    separator=" "
                    items={[/* ... Breadcrumb items ... */]}
                />
                <div className="mt-[20px]"> {/* Reduced top margin */}
                    <p className="text-4xl mb-2">Order History</p>
                    <p className="text-gray-400 mb-6"> {/* Adjusted margin */}
                        Track, review, and analyze your past trading activity. Use the filters below to refine your search.
                    </p>

                    {/* --- Filter Section --- */}
                    <div className="filter-section bg-gray-800 p-4 rounded-lg mb-6">
                        <Row gutter={[16, 16]}> {/* Add vertical and horizontal spacing */}
                            <Col xs={24} sm={12} md={8} lg={6}>
                                <DatePicker.RangePicker
                                    value={selectedDates}
                                    onChange={setSelectedDates}
                                    style={{ width: '100%' }}
                                    // Add presets if desired
                                    presets={[
                                        { label: 'Today', value: [dayjs().startOf('day'), dayjs().endOf('day')] },
                                        { label: 'Last 7 Days', value: [dayjs().add(-7, 'd').startOf('day'), dayjs().endOf('day')] },
                                        { label: 'This Month', value: [dayjs().startOf('month'), dayjs().endOf('month')] },
                                    ]}
                                />
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={5}>
                                <Select
                                    mode="multiple"
                                    allowClear
                                    style={{ width: '100%' }}
                                    placeholder="Select Status(es)"
                                    value={selectedStatuses}
                                    onChange={setSelectedStatuses}
                                    options={STATUSES.map(s => ({ label: s, value: s }))}
                                    maxTagCount="responsive"
                                />
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={5}>
                                <Select
                                    mode="multiple"
                                    allowClear
                                    style={{ width: '100%' }}
                                    placeholder="Select Symbol(s)"
                                    value={selectedStockSymbols}
                                    onChange={setSelectedStockSymbols}
                                    options={STOCK_SYMBOLS.map(s => ({ label: s, value: s }))}
                                    maxTagCount="responsive" // Show limited tags
                                />
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={4}>
                                <Select
                                    mode="multiple"
                                    allowClear
                                    style={{ width: '100%' }}
                                    placeholder="Select Side(s)"
                                    value={selectedSides}
                                    onChange={setSelectedSides}
                                    options={SIDES.map(s => ({ label: s, value: s }))}
                                    maxTagCount="responsive"
                                />
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={4}>
                                <Select
                                    mode="multiple"
                                    allowClear
                                    style={{ width: '100%' }}
                                    placeholder="Select Type(s)"
                                    value={selectedOrderTypes}
                                    onChange={setSelectedOrderTypes}
                                    options={ORDER_TYPES.map(t => ({ label: t, value: t }))}
                                    maxTagCount="responsive"
                                />
                            </Col>
                            <Col xs={24} sm={12} md={8} lg={4}>
                                <Select
                                    mode="multiple"
                                    allowClear
                                    style={{ width: '100%' }}
                                    placeholder="Select TIF(s)"
                                    value={selectedTimeInForces}
                                    onChange={setSelectedTimeInForces}
                                    options={TIME_IN_FORCES.map(t => ({ label: t, value: t }))}
                                    maxTagCount="responsive"
                                />
                            </Col>

                            {/* Action Buttons */}
                            <Col xs={24} style={{ textAlign: 'right' }}> {/* Align buttons to the right */}
                                <Space>
                                    <Button onClick={handleResetFilters}>
                                        Reset
                                    </Button>
                                    <Button type="primary" onClick={handleApplyFilters} loading={loading}>
                                        Apply Filters
                                    </Button>
                                </Space>
                            </Col>
                        </Row>
                    </div>

                    {/* --- Error Alert --- */}
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

                    {/* --- Table Section --- */}
                    <Spin spinning={loading} tip="Loading orders...">
                        <Table
                            columns={columns}
                            dataSource={orders}
                            rowKey="id"
                            scroll={{ x: 'max-content' }}
                            pagination={{ /* ... pagination config ... */
                                pageSize: 10,
                                showSizeChanger: true,
                                pageSizeOptions: ['10', '20', '50', '100'],
                                showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} orders`,
                            }}
                            onRow={(record) => {
                                return {
                                    onClick: () => {
                                        console.log(`Row clicked: ${record.id}`);
                                        if (record && record.id) {
                                            navigate(`/${record.id}/order-details`);
                                        } else {
                                            console.error("Clicked row is missing record or record.id", record);
                                        }
                                    },
                                    // Optional: Add visual feedback on hover
                                    style: { cursor: 'pointer' }
                                };
                            }}
                            // --- END onRow PROP ---
                            // Note: Antd Table's built-in filters (filters/onFilter in columns)
                            // work *client-side* on the currently loaded `dataSource`.
                            // Since we are re-fetching data from the API based on filters,
                            // these client-side filters might become redundant or behave unexpectedly
                            // if the API always returns the filtered set. You might remove them
                            // or keep them for secondary filtering on the fetched results.
                            // Let's keep them for now as examples.
                            className="bg-gray-800 rounded-lg overflow-hidden" // Added overflow hidden
                            onChange={(pagination, filters, sorter) => {
                                console.log('Table Changed:', pagination, filters, sorter);
                                // You could potentially trigger server-side sorting/filtering here
                                // based on the `filters` and `sorter` objects if needed,
                                // but the current setup uses the dedicated filter controls above.
                            }}
                        />
                    </Spin>
                </div>
            </div>
        </>
    );
};

export default OrderViewHistory;
