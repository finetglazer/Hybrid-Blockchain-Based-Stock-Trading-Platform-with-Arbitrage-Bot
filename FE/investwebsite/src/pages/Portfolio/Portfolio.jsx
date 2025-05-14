import "./Portfolio.css"
import {useNavigate, useParams} from "react-router-dom";
import React, {useEffect, useRef, useState} from "react";
import {Alert, Table} from "antd";
import {Chart} from "chart.js/auto";
import 'chartjs-adapter-date-fns';
import "glider-js/glider.min.css";
import Glider from "glider-js/glider";
import axios from "axios";
import useWebSocket, {ReadyState} from 'react-use-websocket';
import {Sparklines, SparklinesLine, SparklinesSpots} from 'react-sparklines';
import SellOrderForm from "./SellOrderForm.jsx";
import LoadingOverlay from "../StockTable/LoadingOverlay.jsx";

const Portfolio = () => {
    // const SOCKET_URL = 'https://good-musical-joey.ngrok-free.app/market-data/ws/stock-data';
    const SOCKET_URL = 'http://localhost:8080/market-data/ws/stock-data';
    const navigate = useNavigate();
    const comparisonChart = useRef(null);
    const gliderInstanceRef = useRef(null);
    const gliderContainerRef = useRef(null);
    const [portfolio, setPortfolio] = useState(null);
    const [stocksData, setStocksData] = useState({});
    const [initialStocksData, setInitialStocksData] = useState({});
    const [totalValue, setTotalValue] = useState(0);
    const [initialTotalValue, setInitialTotalValue] = useState(0);
    const [symbolChanges, setSymbolChanges] = useState({});
    const [sortedSymbolChanges, setSortedSymbolChanges] = useState([]);
    const [openComparisonDataList, setOpenComparisonDataList] = useState(false);
    const [comparisonData, setComparisonData] = useState([]);
    const [comparisonDataList, setComparisonDataList] = useState([]);
    const [tableData, setTableData] = useState([]);
    const [chartDatasets, setChartDatasets] = useState([]);
    const [gliderIndex, setGliderIndex] = useState(3);
    const [showForm, setShowForm] = useState(false);
    const [sellSymbol, setSellSymbol] = useState(null);
    const [error, setError] = useState();
    const [orderCreated, setOrderCreated] = useState(false);
    const [loading, setLoading] = useState(false);

    const onClickBackBtn = () => {
        navigate("/home");
    };
    const createDate = (hour, minute, sec, ms) =>{
        const now = new Date();
        // Set to a specific base date if needed, otherwise use today
        // const baseDate = new Date('2023-10-27T00:00:00'); // Example fixed date
        // now.setFullYear(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate());
        now.setHours(hour, minute, sec, ms);
        return now;
    }
    const onClickGliderNextBtn = () => {
        if (gliderIndex >= comparisonDataList.length) return;
        setGliderIndex(gliderIndex + 3);
    };
    const onClickGliderPrevBtn = () => {
        if (gliderIndex === 3) return;
        setGliderIndex(gliderIndex - 3);
    };
    const convert = (decimal) => {
        let str = decimal.toString();
        let res = "";
        let count = 0;
        const isDecimal = str.includes(".");
        for (let i = (isDecimal ? str.split(".")[0].length - 1 : str.length - 1); i >= 0 && str[i] !== "-"; --i) {
            ++count;
            res = str[i] + res;
            if (count % 3 === 0) res = "," + res;
        }
        if (res[0] === ",") res = res.substring(1, res.length);
        return (decimal < 0 || decimal[0] === "-" ? "-" : "+") + res + (isDecimal ? "." + str.split(".")[1] : "");
    };
    const MiniChart = ({ chartData, width = 100, height = 30, color = "gold" }) => {
        if (!chartData || chartData.length < 2) {
            return <div style={{ width, height, border: '1px dashed #ccc' }}>No data</div>; // Placeholder
        }

        return (
            <Sparklines data={chartData} width={width} height={height} margin={2}>
                {/* The main line */}
                <SparklinesLine color={color} style={{ strokeWidth: 1.0, fill: "none" }} />

                {/* Optional: Spots on the line (e.g., last point) */}
                <SparklinesSpots size={1} spotColors={{ '-1': color }} />

                {/* Optional: Reference line (e.g., average, zero) */}
                {/* <SparklinesReferenceLine type="mean" style={{ stroke: 'white', strokeOpacity: .75, strokeDasharray: '2, 2' }} /> */}
            </Sparklines>
        );
    };

    const drawComparisonChart = () => {
        const ctx = document.getElementById('comparison-chart').getContext('2d');

        if (comparisonChart.current) {
            comparisonChart.current.destroy();
            comparisonChart.current = null; // Clear the ref
        }

        comparisonChart.current = new Chart(ctx, {
            type: 'line',
            data: {
                datasets: []
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                // --- Interaction Settings ---
                interaction: {
                    mode: 'index', // Important: Finds items at the same index on x-axis
                    intersect: false, // Important: Trigger hover even when not directly over point/line
                    axis: 'x' // Only consider x-axis for interaction
                },
                // --- Tooltip and Plugin Settings ---
                plugins: { // <--- START plugins block
                    legend: {
                        display: false
                    },
                    tooltip: {
                        enabled: true, // Ensure tooltips are enabled
                        mode: 'index', // Match interaction mode
                        intersect: false, // Match interaction mode
                        position: 'nearest', // Position tooltip near the interaction point
                        // --- Styling to look like the image ---
                        backgroundColor: 'rgba(255, 255, 255, 0.9)', // White background
                        bodyColor: '#5f6368', // Greyish body text
                        titleColor: '#202124', // Darker title text
                        titleFont: {
                            weight: 'normal', // Non-bold title
                            size: 12
                        },
                        bodyFont: {
                            size: 12
                        },
                        padding: 8,
                        cornerRadius: 4,
                        borderColor: 'rgba(0, 0, 0, 0.1)', // Subtle border
                        borderWidth: 1,
                        caretSize: 6, // Size of the triangle pointer
                        displayColors: false, // Hide the little color boxes in tooltip body

                        callbacks: {
                            // --- Title callback to format the date ---
                            title: function (tooltipItems) {
                                // tooltipItems[0] contains information about the first hovered item
                                if (tooltipItems.length > 0) {
                                    const item = tooltipItems[0];
                                    const date = new Date(item.parsed.x);
                                    // Use Intl.DateTimeFormat for robust formatting (adjust options as needed)
                                    const options = {
                                        year: 'numeric',
                                        month: 'short',
                                        day: 'numeric',
                                        hour: 'numeric',
                                        minute: 'numeric',
                                        hour12: true
                                    };
                                    return new Intl.DateTimeFormat('en-US', options).format(date);
                                    // Or use date-fns if you prefer
                                    // return format(date, "MMM d, yyyy h:mm a"); // Requires date-fns library
                                }
                                return '';
                            },
                            // --- Label callback (value formatting) - already good ---
                            label: function (context) {
                                let label = context.dataset.label || '';
                                if (label) {
                                    label += ': ';
                                }
                                if (context.parsed.y !== null) {
                                    label += context.parsed.y.toFixed(2) + '%';
                                }
                                return label;
                            }
                        }
                    },
                    crosshair: {
                        line: {
                            color: '#aaa',      // Customize line color if needed
                            width: 1,           // Customize line width if needed
                            dashPattern: [5, 5] // Customize dash pattern if needed
                        },
                        sync: {
                            enabled: true // Keep tooltips synchronized with the line
                        },
                        zoom: {
                            enabled: false // Disable zoom feature if you don't need it (recommended)
                        },
                        snap: {
                            enabled: true // Snap line to the nearest data point
                        }
                    }
                }, // <--- END plugins block
                scales: {
                    x: {
                        type: 'time',
                        time: {
                            unit: 'hour', // Adjust unit based on data range (day, hour, etc.)
                            displayFormats: {
                                day: 'MMM d', // Format like 'Apr 16'
                                hour: 'h:mm a', // Example if unit is 'hour'
                            },
                            // Removed problematic tooltipFormat: 'll HH:mm',
                        },
                        grid: {
                            display: false
                        },
                        ticks: {
                            source: 'auto',
                            maxTicksLimit: 25,
                            autoSkip: true,
                        }
                    },
                    y: {
                        type: 'linear',
                        position: 'left',
                        ticks: {
                            callback: function (value) {
                                return value + '%';
                            },
                            stepSize: 2
                        },
                        grid: {
                            drawBorder: false,
                            color: '#e0e0e0'
                        }
                    }
                }
            }
        });
    }

    const columns = [
        {
            title: 'SYMBOL',
            dataIndex: 'symbol',
            key: 'symbol',
            sorter: (a, b) => a.symbol.localeCompare(b.symbol),
            sortDirection: ['ascend', 'descend'],
            width: 80,
            render: (value, record) => (
                <div style={{
                    color: "white",
                    fontWeight: "bold",
                    borderRadius: 5,
                    backgroundColor: record.color,
                    textAlign: "center"
                }}>
                    {record.symbol}
                </div>
            )
        },
        {
            title: 'COMPANY',
            dataIndex: 'company',
            key: 'company',
            sorter: (a, b) => a.company.localeCompare(b.name),
            sortDirection: ['ascend', 'descend'],
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
                    borderRadius: 5,
                    textAlign: "left",
                    display: "flex",
                    marginTop: 10
                }}>
                    <p>{record.company}</p>
                    <div style={{
                        borderRadius: "50%",
                        backgroundColor: "darkblue",
                        width: 30,
                        height: 30,
                        padding: 3,
                        marginLeft: 10
                    }}>
                        {record.dayGainValue[0] === "-" && (
                            <img src="../../../src/assets/downward.png"
                                 alt="trend icon"
                                 style={{
                                     width: 20,
                                     height: 20,
                                     textAlign: "center"
                                 }}
                            />
                        )}
                        {record.dayGainValue[0] !== "-" && (
                            <img src="../../../src/assets/upward.png"
                                 alt="trend icon"
                                 style={{
                                     width: 25,
                                     height: 25,
                                     marginTop: -5
                                 }}
                            />
                        )}
                    </div>
                </div>
            )
        },
        {
            title: 'PRICE',
            dataIndex: 'currentPrice',
            key: 'currentPrice',
            sorter: (a, b) => a.currentPrice - b.currentPrice,
            sortDirection: ['ascend', 'descend'],
            width: 100,
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
                }}>
                    {record.currentPrice}
                </div>
            )
        },
        {
            title: 'AVG',
            dataIndex: 'averagePrice',
            key: 'averagePrice',
            sorter: (a, b) => a.averagePrice - b.averagePrice,
            sortDirection: ['ascend', 'descend'],
            width: 100,
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
                }}>
                    {record.averagePrice}
                </div>
            )
        },
        {
            title: 'QUANTITY',
            dataIndex: 'quantity',
            key: 'quantity',
            sorter: (a, b) => a.quantity - b.quantity,
            sortDirection: ['ascend', 'descend'],
            width: 120,
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
                }}>
                    {record.quantity}
                </div>
            )
        },
        {
            title: 'DAY GAIN',
            dataIndex: 'dayGainValue',
            key: 'dayGainValue',
            sorter: (a, b) => a.dayGainValue - b.dayGainValue,
            sortDirection: ['ascend', 'descend'],
            width: 230,
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between"
                }}>
                    <p style={{marginTop: 12}}>{record.dayGainValue}</p>
                    {record.dayGainValue[0] === "-" && (
                        <div
                            style={{
                                height: 30,
                                width: 90,
                                backgroundColor: "#670202",
                                display: "flex",
                                borderRadius: 10,
                                padding: 5,
                                // padding: "15px auto 5px",
                            }}
                        >
                            <img
                                src="../../../src/assets/arrow-down.png"
                                alt="direction icon"
                                width={20}
                                height={20}
                            />
                            <p style={{color: "#ff2343", marginLeft: 3, marginTop: -2}}>{record.dayGainPercentage}%</p>
                        </div>
                    )}
                    {record.dayGainValue[0] !== "-" && (
                        <div
                            style={{
                                height: 30,
                                width: 90,
                                backgroundColor: "#054d05",
                                display: "flex",
                                borderRadius: 10,
                                padding: 5,
                            }}
                        >
                            <img
                                src="../../../src/assets/up-arrow.png"
                                alt="direction icon"
                                width={15}
                                style={{height: 15, marginTop: 2}}
                            />
                            <p style={{color: "#88ec9f", marginLeft: 3, marginTop: -2}}>{record.dayGainPercentage}%</p>
                        </div>
                    )}
                </div>
            )
        },
        {
            title: 'ACQ DATE',
            dataIndex: 'acquisitionDate',
            key: 'acquisitionDate',
            sorter: (a, b) => a.acquisitionDate.localeCompare(b.acquisitionDate),
            sortDirection: ['ascend', 'descend'],
            width: 220,
            render: (value, record) => (
                <div style={{
                }}>
                    {record.acquisitionDate}
                </div>
            )
        },
        {
            title: 'LAST UPDATED',
            dataIndex: 'lastUpdated',
            key: 'lastUpdated',
            sorter: (a, b) => a.lastUpdated.localeCompare(b.lastUpdated),
            sortDirection: ['ascend', 'descend'],
            width: 220,
            render: (value, record) => (
                <div style={{
                }}>
                    {record.lastUpdated}
                </div>
            )
        },
        {
            title: 'VALUE',
            dataIndex: 'value',
            key: 'value',
            sorter: (a, b) => a.value - b.value,
            sortDirection: ['ascend', 'descend'],
            width: 170,
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
                }}>
                    {record.value}
                </div>
            )
        },
        {
            title: 'ACTIONS',
            width: 120,
            render: (value, record) => (
                <button className="px-2 py-2 w-[100px]" onClick={() => {
                    setShowForm(true);
                    setSellSymbol(record.symbol);
                }}>
                    SELL
                </button>
            )
        },
    ];

    const {
        lastJsonMessage, // Automatically parses incoming JSON
    } = useWebSocket(SOCKET_URL + `?token=${localStorage.getItem("token")}`, {
        onOpen: () => {
            console.log('WebSocket Connected');
        },
        onClose: (event) => console.log('WebSocket Disconnected', event.reason),
        onError: (event) => {
            console.error('WebSocket Error: ', event);
        },
        shouldReconnect: () => true, // Automatically attempt to reconnect
        reconnectInterval: 3000, // Reconnect attempt interval
        // Filter out non-JSON messages if necessary (though your BE should only send JSON)
        // filter: (message) => message.data.startsWith('{'),
    });



    // --- Handle incoming messages ---
    useEffect(() => {
        if (lastJsonMessage !== null) {
            // console.log('Received update: ', lastJsonMessage);
            const update = lastJsonMessage; // Already parsed

            // --- IMPORTANT: State Update ---
            if (update && update.symbol) {
                setStocksData(prevData => {
                    // console.log('Updating state: ', newData);
                    return {
                        ...prevData,
                        [update.symbol]: update
                    };
                });
            } else {
                console.warn("Received invalid message format: ", lastJsonMessage);
            }
        }
    }, [lastJsonMessage]); // Re-run this effect only when a new JSON message arrives

    useEffect(() => {
        if (!portfolio) return;
        drawComparisonChart();
        const positions = portfolio.positions;
        const updatedComparisonData = positions.map(position => position.stockSymbol);
        setComparisonData(updatedComparisonData);
    }, [portfolio]);

    useEffect(() => {
        const token = localStorage.getItem("token");
        const fetchPortfolio = async () => {
            try {
                const response = await axios.get(`/market-data/api/v1/me/general-portfolio`, {
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                });
                if (response.data && response.data.status === 1) {
                    const portfolio = response.data.data;
                    setPortfolio(portfolio);
                }
                else {
                    console.log(response.data.msg);
                }
            } catch (e) {
                console.log(e.message);
            }
        }

        fetchPortfolio().then(() => {})
    }, []);

    useEffect(() => {
        if (!portfolio) return;
        const positions = portfolio.positions;

        if (!stocksData || Object.keys(stocksData).length === 0) return;
        setComparisonDataList(Object.values(stocksData)
            .filter(stock => !comparisonData.includes(stock.symbol))
            .map(stock => stock.symbol)
        );

        if (!initialTotalValue) {
            setInitialTotalValue(positions.reduce((s, stock) => s + stock.currentPrice * stock.quantity, 0));
        }
        setTotalValue(positions
            .filter(stock => Object.keys(stocksData).includes(stock.stockSymbol))
            .reduce((s, stock) => s + stocksData[`${stock.stockSymbol}`].price * stock.quantity, 0));

        let updatedInitialStocksData = initialStocksData;
        Object.values(stocksData).forEach(stock => {
            if (!Object.keys(updatedInitialStocksData).includes(stock.symbol)) {
                updatedInitialStocksData[stock.symbol] = stock;
            }
        });
        setInitialStocksData(updatedInitialStocksData);

        let updatedSymbolChanges = {};
        Object.values(stocksData).forEach((stock) => {
            const symbol = stock.symbol;
            const valueChange = stock.price - (!Object.keys(updatedInitialStocksData).includes(stock.symbol) ? stock.price : updatedInitialStocksData[stock.symbol].price);
            const percentageChange = (valueChange * 100 / (!Object.keys(updatedInitialStocksData).includes(stock.symbol) ? stock.price : initialStocksData[stock.symbol].price));
            updatedSymbolChanges[symbol] = {
                symbol: stock.symbol,
                valueChange: valueChange,
                percentageChange: percentageChange
            };
        });
        setSymbolChanges(updatedSymbolChanges);

        const sortedArray = Object.values(updatedSymbolChanges).sort((a, b) => {
            return parseFloat(a.percentageChange) - parseFloat(b.percentageChange);
        });

        setSortedSymbolChanges(sortedArray);

        if (!comparisonChart.current) return;

        Object.keys(stocksData).forEach(symbol => {
            let existed = false;
            let ind = -1;
            comparisonChart.current.data.datasets.forEach((dataset, index) => {
                if (dataset.label === symbol) {
                    existed = true;
                    ind =  index;
                }
            });
            const stock = stocksData[symbol];
            const time = new Date(stock.timestamp.substring(0, 23) + "Z");
            const data = {
                x: createDate(time.getUTCHours(), time.getUTCMinutes(), time.getUTCSeconds(), time.getUTCMilliseconds()),
                y: updatedSymbolChanges[stock.symbol].percentageChange,
            };
            if (existed) {
                const datasetData = comparisonChart.current.data.datasets[ind].data;
                const lastData = datasetData[datasetData.length - 1];
                if (data.x !== lastData.x && data.y !== lastData.y) {
                    comparisonChart.current.data.datasets[ind].data.push(data);
                }
            }
            else {
                const color = '#' + Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0');
                comparisonChart.current.data.datasets.push({
                    label: symbol,
                    data: [data],
                    borderColor: color,
                    backgroundColor: 'transparent',
                    borderWidth: 2,
                    tension: 0.1, // Keep slight smoothing
                    pointRadius: (context) => { // Keep last point visible
                        const index = context.dataIndex;
                        const count = context.dataset.data.length;
                        return index === count - 1 ? 4 : 0;
                    },
                    pointBackgroundColor: color,
                    // --- Hover Point Styles ---
                    pointHoverRadius: 5, // Radius of the circle on hover
                    pointHoverBackgroundColor: color,
                    pointHoverBorderColor: 'rgba(0, 0, 0, 0.2)', // Optional subtle border
                    pointHoverBorderWidth: 1,
                    pointHitRadius: 15, // Increase hit radius for easier hovering near line
                    hidden: !positions.find(position => position.stockSymbol === symbol)
                });
            }
        });

        comparisonChart.current.update('none');

        setChartDatasets(comparisonChart.current.data.datasets);

    }, [stocksData]);

    useEffect(() => {
        if (!portfolio || !Object.keys(stocksData).length || !Object.keys(symbolChanges).length) return;
        let updatedTableData = tableData;
        const positions = portfolio.positions;
        positions.forEach(stock => {
            let existed = false;
            let ind = -1;
            updatedTableData.forEach((item, index) => {
                if (item.symbol === stock.stockSymbol) {
                    existed = true;
                    ind = index;
                }
            });
            if (Object.keys(stocksData).includes(stock.stockSymbol) && Object.keys(symbolChanges).includes(stock.stockSymbol)) {
                const updatedItem = {
                    color: existed ? updatedTableData[ind].color : '#' + Math.floor(Math.random() * 16777215).toString(16).padStart(6, '0'),
                    symbol: stock.stockSymbol,
                    company: stocksData[`${stock.stockSymbol}`].company,
                    currentPrice: convert(stocksData[`${stock.stockSymbol}`].price.toFixed(2)),
                    averagePrice: convert(stock.averagePrice.toFixed(2)),
                    quantity: stock.quantity,
                    dayGainValue: convert(symbolChanges[`${stock.stockSymbol}`].valueChange.toFixed(2)),
                    dayGainPercentage: convert(symbolChanges[`${stock.stockSymbol}`].percentageChange.toFixed(2)),
                    acquisitionDate: new Date(stock.acquiredAt).toLocaleString('en-US', {
                        year: 'numeric',
                        month: 'short',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit',
                        hour12: true
                    }),
                    lastUpdated: new Date().toLocaleString('en-US', {
                        year: 'numeric',
                        month: 'short',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit',
                        hour12: true
                    }),
                    value: convert((stock.quantity * stocksData[`${stock.stockSymbol}`].price).toFixed(2))
                };
                existed ? updatedTableData[ind] = updatedItem : updatedTableData.push(updatedItem);
            }
        });

        setTableData(updatedTableData);
    }, [stocksData, portfolio, symbolChanges]);

    useEffect(() => {
        // Ensure the container element is rendered and we have data to show
        if (!gliderContainerRef.current) return;
        if (gliderInstanceRef.current && comparisonDataList.length > 0) {

            // --- Destroy Previous Instance ---
            // If a Glider instance already exists, destroy it first
            if (gliderInstanceRef.current) {
                console.log("Destroying previous Glider instance.");
                gliderInstanceRef.current.destroy();
                gliderInstanceRef.current = null; // Clear the ref
            }

            // --- Initialize New Instance ---
            console.log("Initializing new Glider instance.");
            // Store the new instance
            gliderInstanceRef.current = new Glider(gliderInstanceRef.current, {
                slidesToShow: 4, // Or 'auto' if width varies
                slidesToScroll: 1,
                draggable: true,
                // Ensure these elements exist in your JSX
                dots: "#dots", // Make sure you have <div id="dots"></div>
                arrows: {
                    prev: "#glider-prev", // Make sure you have <button id="glider-prev">
                    next: "#glider-next"  // Make sure you have <button id="glider-next">
                },
                // Responsive options if needed
                // responsive: [ ... ]
            });

        } else if (gliderInstanceRef.current) {
            // If data becomes empty but an instance exists, destroy it
            console.log("Data empty, destroying Glider instance.");
            gliderInstanceRef.current.destroy();
            gliderInstanceRef.current = null;
        }

        // --- Cleanup Function ---
        // Runs when component unmounts or BEFORE the effect runs again
        return () => {
            if (gliderInstanceRef.current) {
                console.log("Cleaning up Glider instance.");
                gliderInstanceRef.current.destroy();
                gliderInstanceRef.current = null;
            }
        };
    }, [comparisonDataList]);

    useEffect(() => {
        // console.log(comparisonData);
        // console.log(comparisonDataList);
    }, [stocksData]);

    return (
        <div className="container portfolio-container relative">
            <>
                {showForm && (
                    <>
                        <div className="absolute top-[170vh] left-[40%] z-[1000] blur-none">
                            <SellOrderForm sellSymbol={sellSymbol}
                                           setShowForm={setShowForm}
                                           setError={setError}
                                           setOrderCreated={setOrderCreated}
                                           setLoading={setLoading}
                            />
                            <LoadingOverlay
                                visible={loading}
                                message="Preparing your order..."
                            />
                        </div>
                    </>
                )}
                {error &&
                    error.errors.map(e =>
                        <Alert showIcon type={error} message={e}/>
                    )
                }
                {!error && orderCreated && (
                    <>
                        <Alert showIcon type="success" message={
                            <p>Order created! Please visit <a href="/order-history">Order history</a> to check your order status</p>
                        }/>
                    </>
                )}
                <div className="title" onClick={onClickBackBtn}>
                    <button><img src="../../../../src/assets/left-arrow.png" alt="back icon"/></button>
                    <p>Portfolio</p>
                </div>
                <div className={`body ${showForm ? "blur-lg" : ""}`}>
                    <div className="portfolio-info">
                        <div className="prop">
                            <img src="../../../src/assets/user.png" alt="user icon"/>
                            <p>User ID: {(portfolio ?? {userId: ""}).userId}</p>
                        </div>
                        <div className="value-info">
                            <p className="portfolio-name">{(portfolio ?? {name: ""}).name}</p>
                            <div className="total-value-info">
                                <p className={"total-value"}>${convert(totalValue.toFixed(2))}</p>
                                {totalValue - initialTotalValue < 0 && (
                                    <div className="gain-percentage-wrapper items-center">
                                        <img src="../../../src/assets/arrow-down.png" alt="direction icon" style={{width: 15, height: 15, marginRight: 3, marginTop: 10}}/>
                                        <p className="gain-percentage mb-7">{convert(((totalValue - initialTotalValue) * 100 / initialTotalValue).toFixed(2))}%</p>
                                    </div>
                                )}
                                {totalValue - initialTotalValue >= 0 && (
                                    <div className="gain-percentage-wrapper" style={{backgroundColor: "#054d05"}}>
                                        <img src="../../../src/assets/up-arrow.png" alt="direction icon" style={{width: 15, height: 15, marginRight: 3}}/>
                                        <p className="gain-percentage" style={{color: "#88ec9f"}}>{initialTotalValue ? convert(((totalValue - initialTotalValue) * 100 / initialTotalValue).toFixed(2)) : (totalValue ? "100.00" : "0.00")}%</p>
                                    </div>
                                )}

                                <p className="gain-value" style={{color: totalValue - initialTotalValue >= 0 ? "#88ec9f" : "#ff0044"}}>{convert((totalValue - initialTotalValue).toFixed(2))}$</p>
                            </div>
                            <p className="date">{(new Date()).toLocaleString('en-US', {
                                month: 'short',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                second: '2-digit',
                                hour12: true
                            })
                            }
                            </p>
                        </div>
                    </div>
                    <div className="chart-and-highlight-container">
                        <div className="comparison-chart-container">
                            <div className="chart-canvas">
                                <canvas id="comparison-chart" width="100%" height="100%"/>
                            </div>
                            <div className="comparison-object-container">
                                {comparisonData.map((symbol) => (
                                    <div className="comparison-object">
                                        <div className="color-streak" style={{background: (chartDatasets.find(dataset => dataset.label === symbol) ?? {borderColor: 'orange'}).borderColor}}/>
                                        <p className="name" style={{marginLeft: 5}}>
                                            {(() => {
                                                const stock = Object.values(stocksData).find(stock => stock.symbol === symbol);
                                                if (!stock) return "...";
                                                return stock.company.length > 18
                                                    ? stock.company.substring(0, 18) + "..."
                                                    : stock.company;
                                            })()}
                                        </p>
                                        {(symbolChanges[symbol] ?? {percentageChange: 0}).percentageChange >= 0 && (
                                            <>
                                                <p className="total-gain-value">{convert((symbolChanges[symbol] ?? {valueChange: 0}).valueChange.toFixed(2))}$</p>
                                                <div className="total-gain-percentage">
                                                    <img src="../../../src/assets/up-arrow.png" alt="direction icon" style={{width: 12, height: 12, marginRight: 3}}/>
                                                    <p style={{color: "#9af1b6"}}>{convert((symbolChanges[symbol] ?? {percentageChange: 0}).percentageChange.toFixed(2))}%</p>
                                                </div>
                                            </>
                                        )}
                                        {(symbolChanges[symbol] ?? {percentageChange: 0}).percentageChange < 0 && (
                                            <>
                                                <p className="total-gain-value" style={{color: "#ff2343", fontWeight: 700}}>{convert((symbolChanges[symbol] ?? {valueChange: 0}).valueChange.toFixed(2))}$</p>
                                                <div className="total-gain-percentage" style={{backgroundColor: "#670202"}}>
                                                    <img src="../../../src/assets/arrow-down.png" alt="direction icon" style={{
                                                        width: 15,
                                                        height: 15,
                                                        marginRight: 3
                                                    }}/>
                                                    <p style={{color: "#ff2343", fontWeight: 700}}>{convert((symbolChanges[symbol] ?? {percentageChange: 0}).percentageChange.toFixed(2))}%</p>
                                                </div>
                                            </>
                                        )}
                                        <div className="remove-btn-wrapper">
                                            <button className="remove-btn" onClick={() => {
                                                const updatedComparisonData = comparisonData;
                                                updatedComparisonData.splice(comparisonData.findIndex(symbolData => symbolData === symbol), 1);
                                                setComparisonData(updatedComparisonData);
                                                setComparisonDataList([...comparisonDataList, symbol]);
                                                comparisonChart.current.data.datasets[comparisonChart.current.data.datasets.findIndex(dataset => dataset.label === symbol)].hidden = true;
                                                comparisonChart.current.update('none');
                                            }}>
                                                <img src="../../../src/assets/x-white.png" alt="remove icon" />
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                            <div className="chart-buttons">
                                <button className="add-comparison-btn" onClick={() => {setOpenComparisonDataList(true)}}>
                                    <img src="../../../src/assets/add.png" alt="add icon" />
                                    <p>Add comparison</p>
                                </button>
                                {comparisonData && comparisonData.length > 0 && (
                                    <button className="clear-all-btn" onClick={() => {
                                        let updatedComparisonDataList = [...comparisonDataList];
                                        comparisonData.forEach(symbol => {
                                            updatedComparisonDataList.push(symbol);
                                            comparisonChart.current.data.datasets[comparisonChart.current.data.datasets.findIndex(dataset => dataset.label === symbol)].hidden = true;
                                            comparisonChart.current.update('none');
                                        })
                                        setComparisonData([]);
                                        setComparisonDataList(updatedComparisonDataList);
                                    }}>
                                        Clear all
                                    </button>
                                )}
                                {openComparisonDataList && (
                                    <div className="comparison-objects-list-container">
                                        <div className="search-bar">
                                            <input placeholder="Search for stocks, ETFs & more to compare with"></input>
                                            <button className="close-btn" onClick={() => {
                                                setOpenComparisonDataList(false)
                                            }}>
                                                <img src="../../../src/assets/x-white.png" alt="close icon"/>
                                            </button>
                                        </div>
                                        <div className="comparison-objects-list">
                                            {comparisonDataList.map(symbol => (
                                                <div className="comparison-object"
                                                     onClick={() => {
                                                         comparisonChart.current.data.datasets[comparisonChart.current.data.datasets.findIndex(dataset => dataset.label === symbol)].hidden = false;
                                                         comparisonChart.current.update('none');
                                                         setComparisonData([...comparisonData, symbol]);
                                                         setComparisonDataList(() => {
                                                             const updatedComparisonDataList = comparisonDataList;
                                                             updatedComparisonDataList.splice(comparisonDataList.findIndex(symbolData => symbolData === symbol), 1)
                                                             return updatedComparisonDataList;
                                                         });
                                                     }}
                                                >
                                                    <div className="comparison-object-info">
                                                        <p className="name">{stocksData[symbol].company}</p>
                                                        <p className="symbol">{symbol}</p>
                                                    </div>
                                                    <div className="value">
                                                        <p className="total-gain-value" style={{color: symbolChanges[symbol].percentageChange >= 0 ? "#88ec9f" : "#ff0044", fontWeight: "bold"}}>{convert(symbolChanges[symbol].valueChange.toFixed(2))}$</p>
                                                        {symbolChanges[symbol].percentageChange >= 0 &&
                                                            <div className="total-gain-percentage-wrapper">
                                                                <div className="total-gain-percentage" style={{backgroundColor: "#054d05"}}>
                                                                    <img src="../../../src/assets/up-arrow.png"
                                                                         alt="direction icon"
                                                                         style={{width: 12, height: 12, marginRight: 3}}
                                                                    />
                                                                    <p style={{color: "#88ec9f", fontWeight: "bold"}}>{convert(symbolChanges[symbol].percentageChange.toFixed(2))}%</p>
                                                                </div>
                                                            </div>
                                                        }
                                                        {symbolChanges[symbol].percentageChange < 0 &&
                                                            <div className="total-gain-percentage-wrapper">
                                                                <div className="total-gain-percentage" style={{backgroundColor: "#670202"}}>
                                                                    <img src="../../../src/assets/arrow-down.png"
                                                                         alt="direction icon"
                                                                         style={{width: 15, height: 15, marginRight: 3}}
                                                                    />
                                                                    <p style={{color: "#ff2343", fontWeight: "bold"}}>{convert(symbolChanges[symbol].percentageChange.toFixed(2))}%</p>
                                                                </div>
                                                            </div>
                                                        }
                                                    </div>
                                                    <div className="mini-chart-wrapper">
                                                        <MiniChart
                                                            chartData={(chartDatasets.find(dataset => dataset.label === symbol) ?? {data: []}).data.map(item => item.y)}
                                                            color={(symbolChanges[symbol] ?? {color: "gold"}).percentageChange >= 0 ? "green" : "red"}
                                                        />
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="glider-and-highlight-container">
                            <div className="highlight-container container">
                                <p className="title">Portfolio highlights</p>
                                <div className="categories">
                                    <div className="category-wrapper">
                                        <p className="category-name">MOST GAIN</p>
                                        {Object.keys(sortedSymbolChanges).length &&
                                            <div className="value-wrapper"
                                                 style={{backgroundColor: sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(sortedSymbolChanges).length - 1]].valueChange < 0 ? "#670202" : "#054d05"}}>
                                                <p className="symbol-info" style={{
                                                    marginTop: -10,
                                                    fontWeight: 700,
                                                    fontSize: "1.2rem"
                                                }}>{sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(symbolChanges).length - 1]].symbol}</p>
                                                <p className="value"
                                                   style={{color: sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(sortedSymbolChanges).length - 1]].valueChange < 0 ? "#ff2343" : "#88ec9f"}}>{convert(sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(sortedSymbolChanges).length - 1]].valueChange.toFixed(2))}$</p>
                                                <div className="percentage items-center">
                                                    {sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(sortedSymbolChanges).length - 1]].valueChange < 0 && (
                                                        <>
                                                            <img src="../../../src/assets/arrow-down.png" alt="direction icon" className="mt-4"/>
                                                            <p style={{color: "#ff2343"}}>{convert(sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(sortedSymbolChanges).length - 1]].percentageChange.toFixed(2))}%</p>
                                                        </>
                                                    )}
                                                    {sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(sortedSymbolChanges).length - 1]].valueChange >= 0 && (
                                                        <>
                                                            <img src="../../../src/assets/up-arrow.png" alt="direction icon"
                                                                 style={{width: 15, height: 15, marginRight: 3}}/>
                                                            <p style={{color: "#88ec9f"}}>{convert(sortedSymbolChanges[Object.keys(sortedSymbolChanges)[Object.keys(sortedSymbolChanges).length - 1]].percentageChange.toFixed(2))}%</p>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                        }
                                    </div>
                                    <div className="category-wrapper">
                                        <p className="category-name">LEAST GAIN</p>
                                        {Object.keys(sortedSymbolChanges).length &&
                                            <div className="value-wrapper items-center"
                                                 style={{backgroundColor: sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].valueChange < 0 ? "#670202" : "#054d05"}}>
                                                <p className="symbol-info" style={{
                                                    marginTop: -10,
                                                    fontWeight: 700,
                                                    fontSize: "1.2rem"
                                                }}>{sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].symbol}</p>
                                                <p className="value"
                                                   style={{color: sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].valueChange < 0 ? "#ff2343" : "#88ec9f"}}>{convert(sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].valueChange.toFixed(2))}$</p>
                                                <div className="percentage">
                                                    {sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].valueChange < 0 && (
                                                        <>
                                                            <img src="../../../src/assets/arrow-down.png" alt="direction icon" style={{marginTop: 15}}/>
                                                            <p style={{color: "#ff2343"}}>{convert(sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].percentageChange.toFixed(2))}%</p>
                                                        </>
                                                    )}
                                                    {sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].valueChange >= 0 && (
                                                        <>
                                                            <img src="../../../src/assets/up-arrow.png" alt="direction icon"
                                                                 style={{width: 15, height: 15, marginRight: 3}}/>
                                                            <p style={{color: "#88ec9f"}}>{convert(sortedSymbolChanges[Object.keys(sortedSymbolChanges)[0]].percentageChange.toFixed(2))}%</p>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                        }
                                    </div>
                                </div>
                            </div>
                            <div className="glider-container">
                                <div className="glider" ref={gliderContainerRef} style={{marginTop: 30, marginLeft: -40}}>
                                    {comparisonDataList.slice(gliderIndex - 3, Math.min(gliderIndex, comparisonDataList.length)).map(symbol => (
                                        <div className="comparison-object-wrapper"
                                             onClick={() => {
                                                 comparisonChart.current.data.datasets[comparisonChart.current.data.datasets.findIndex(dataset => dataset.label === symbol)].hidden = false;
                                                 comparisonChart.current.update('none');
                                                 setComparisonData([...comparisonData, symbol]);
                                                 setComparisonDataList((() => {
                                                     const updatedComparisonDataList = comparisonDataList;
                                                     updatedComparisonDataList.splice(comparisonDataList.findIndex(symbolData => symbolData === symbol), 1)
                                                     return updatedComparisonDataList;
                                                 })());
                                             }}
                                        >
                                            <p className="company">{(() => {
                                                const company = (stocksData[symbol] ?? {company: "..."}).company;
                                                return company.length >= 15 ? company.substring(0, 15) + "..." : company;
                                            })()}</p>
                                            <p className="price" style={{fontWeight: 600, fontSize: "1.2rem"}}>${(stocksData[symbol] ?? {price: "0.00"}).price}</p>
                                            <div className="symbol-and-value-wrapper" style={{display: "flex", marginTop: -10, justifyContent: "space-between"}}>
                                                <p className="symbol-name" style={{color: "#e4e4e4", fontWeight: 600, fontSize: "0.9rem"}}>{symbol}</p>
                                                <div className="percentage" style={{display: "flex", alignItems: "center", fontSize: "0.9rem"}}>
                                                    {symbolChanges[symbol].percentageChange < 0 && (
                                                        <>
                                                            <img src="../../../src/assets/arrow-down.png" alt="direction icon" style={{width: 15, height: 15, marginRight: 3}}/>
                                                            <p style={{color: "#ff2343", fontWeight: 700}}>{convert(symbolChanges[symbol].percentageChange.toFixed(2))}%</p>
                                                        </>
                                                    )}
                                                    {symbolChanges[symbol].percentageChange >= 0 && (
                                                        <>
                                                            <img src="../../../src/assets/up-arrow.png" alt="direction icon"
                                                                 style={{width: 15, height: 15, marginRight: 3}}/>
                                                            <p style={{color: "#88ec9f", fontWeight: 700}}>{convert(symbolChanges[symbol].percentageChange.toFixed(2))}%</p>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                                <button id="glider-prev" onClick={onClickGliderPrevBtn}>«</button>
                                <button id="glider-next" onClick={onClickGliderNextBtn}>»</button>
                            </div>
                        </div>
                    </div>
                    <div className="investments-table">
                        <div className="buttons">
                            <div className="left-buttons">
                                <button className="investments-btn table-btn">
                                    Investments
                                </button>
                            </div>
                            <div className="right-buttons">
                                <button className="add-investment-btn table-btn">
                                    <img src="../../../src/assets/add.png" alt="add icon" />
                                    <p>Investment</p>
                                </button>
                            </div>
                        </div>
                        <div className="table">
                            <Table
                                dataSource={tableData}
                                columns={columns}
                            />
                        </div>
                    </div>
                </div>
            </>
        </div>
    );
};

export default Portfolio;
