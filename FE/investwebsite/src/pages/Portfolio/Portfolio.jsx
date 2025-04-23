import "./Portfolio.css"
import {useNavigate, useParams} from "react-router-dom";
import React, {useEffect, useRef, useState} from "react";
import {Table} from "antd";
import {Chart} from "chart.js/auto";
import 'chartjs-adapter-date-fns';
import "glider-js/glider.min.css";
import Glider from "glider-js/glider";

const Portfolio = () => {
    const accountId = useParams();

    const navigate = useNavigate();
    const performanceChart = useRef(null);
    const gliderRef = useRef(null);
    const [testData, setTestData] = useState([]);
    const [wilshireData, setWilshireData] = useState([]);
    const [openComparisonObjectsList, setOpenComparisonObjectsList] = useState(false);

    const comparisonData = [
        {
            name: 'TEST',
            symbol: "TEST",
            totalValue: "$9,254,878,152.77",
            totalGainValue: "+$6,725,812.05",
            totalGainPercentage: "7.93%"
        },
        {
            name: 'Dow Johns Industrial Average',
            symbol: "DJI",
            totalValue: "38,837.58",
            totalGainValue: "+747.17",
            totalGainPercentage: "1.96%"
        },
    ];

    const onClickBackBtn = () => {
        navigate("/home");
    };
    function createDate(hour, minute) {
        const now = new Date();
        // Set to a specific base date if needed, otherwise use today
        // const baseDate = new Date('2023-10-27T00:00:00'); // Example fixed date
        // now.setFullYear(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate());
        now.setHours(hour, minute, 0, 0);
        return now;
    }

    const drawPerformanceChart = () => {
        const ctx = document.getElementById('performance-chart').getContext('2d');

        if (performanceChart.current) {
            performanceChart.current.destroy();
            performanceChart.current = null; // Clear the ref
        }

        performanceChart.current = new Chart(ctx, {
            type: 'line',
            data: {
                datasets: [
                    {
                        label: 'TEST',
                        data: testData,
                        borderColor: '#4285F4',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        tension: 0.1, // Keep slight smoothing
                        pointRadius: (context) => { // Keep last point visible
                            const index = context.dataIndex;
                            const count = context.dataset.data.length;
                            return index === count - 1 ? 4 : 0;
                        },
                        pointBackgroundColor: '#4285F4',
                        // --- Hover Point Styles ---
                        pointHoverRadius: 5, // Radius of the circle on hover
                        pointHoverBackgroundColor: '#4285F4',
                        pointHoverBorderColor: 'rgba(0, 0, 0, 0.2)', // Optional subtle border
                        pointHoverBorderWidth: 1,
                        pointHitRadius: 15, // Increase hit radius for easier hovering near line
                    },
                    {
                        label: 'Wilshire 5000',
                        data: wilshireData,
                        borderColor: '#F9AB00',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        tension: 0.1, // Keep slight smoothing
                        pointRadius: (context) => { // Keep last point visible
                            const index = context.dataIndex;
                            const count = context.dataset.data.length;
                            return index === count - 1 ? 4 : 0;
                        },
                        pointBackgroundColor: '#F9AB00',
                        // --- Hover Point Styles ---
                        pointHoverRadius: 5, // Radius of the circle on hover
                        pointHoverBackgroundColor: '#F9AB00',
                        pointHoverBorderColor: 'rgba(0, 0, 0, 0.2)', // Optional subtle border
                        pointHoverBorderWidth: 1,
                        pointHitRadius: 15, // Increase hit radius for easier hovering near line
                    }
                ]
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

    const mockData = [
        {
            symbol: 'AMZN',
            name: 'Amazon.com Inc',
            currentPrice: "$167.32",
            averagePrice: "$166.55",
            quantity: 200,
            acquisitionDate: "Apr 22, 17:34:22",
            lastUpdated: "Apr 22, 17:34:22",
            dayGainValue: "-$10,720.00",
            dayGainPercentage: "3.11%",
            value: "$8,696,443,112"
        }
    ];

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
                    backgroundColor: "red",
                    textAlign: "center"
                }}>
                    {record.symbol}
                </div>
            )
        },
        {
            title: 'NAME',
            dataIndex: 'name',
            key: 'name',
            sorter: (a, b) => a.name.localeCompare(b.name),
            sortDirection: ['ascend', 'descend'],
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
                    borderRadius: 5,
                    textAlign: "left",
                    display: "flex",
                    alignItems: "center",
                }}>
                    <p>{record.name}</p>
                    <div style={{
                        borderRadius: "50%",
                        backgroundColor: "darkblue",
                        width: 20,
                        height: 20,
                        padding: 3,
                        marginLeft: 10
                    }}>
                        <img src="../../../src/assets/downward.png"
                             alt="trend icon"
                            style={{
                                width: 20,
                                height: 20
                            }}
                        />
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
                    <p>{record.dayGainValue}</p>
                    <div
                        style={{
                            height: 30,
                            backgroundColor: "#670202",
                            display: "flex",
                            alignItems: "center",
                            borderRadius: 10,
                            padding: 3,
                        }}
                    >
                        <img
                            src="../../../src/assets/arrow-down.png"
                            alt="direction icon"
                            width={20}
                            height={20}
                        />
                        <p style={{color: "#ff2343", marginLeft: 3}}>{record.dayGainPercentage}</p>
                    </div>
                </div>
            )
        },
        {
            title: 'ACQ DATE',
            dataIndex: 'acquisitionDate',
            key: 'acquisitionDate',
            sorter: (a, b) => a.acquisitionDate.localeCompare(b.acquisitionDate),
            sortDirection: ['ascend', 'descend'],
            width: 170,
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
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
            width: 170,
            render: (value, record) => (
                <div style={{
                    fontWeight: "bold",
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
    ];

    useEffect(() => {
        let currentTestY = 0.1;
        let currentWilshireY = -0.1;
        let generatedTestData = [];
        let generatedWilshireData = [];
        for (let hour = 0; hour < 24; hour++) {
            const currentDate = createDate(hour, 0);
            generatedTestData.push({ x: currentDate, y: parseFloat(currentTestY.toFixed(2)) });
            generatedWilshireData.push({ x: currentDate, y: parseFloat(currentWilshireY.toFixed(2)) });
            let testChange = (Math.random() - 0.6) * 0.8;
            let wilshireChange = (Math.random() - 0.5) * 0.5;
            if (hour > 8 && hour < 17) {
                testChange -= 0.2 * Math.random();
            }
            currentTestY += testChange;
            currentWilshireY += wilshireChange;
        }

        setTestData(generatedTestData);
        setWilshireData(generatedWilshireData);

    }, []);

    useEffect(() => {
        console.log(testData);
        console.log(wilshireData);
        drawPerformanceChart();
    }, [testData, wilshireData]);

    useEffect(() => {
        new Glider(gliderRef.current, {
            slidesToShow: 4,
            slidesToScroll: 1,
            draggable: true,
            dots: "#dots",
            arrows: {
                prev: "#glider-prev",
                next: "#glider-next"
            }
        });
    }, []);

    return (
        <div className="container portfolio-container">
            <div className="title" onClick={onClickBackBtn}>
                <button><img src="../../../../src/assets/left-arrow.png" alt="back icon"/></button>
                <p>Portfolio</p>
            </div>
            <div className="body">
                <div className="portfolio-info">
                    <div className="prop">
                        <img src="../../../src/assets/user.png" alt="user icon" />
                        <p>User ID: 7ed3f4fc-6847-489f-b94f-68a6ea833fa3</p>
                    </div>
                    <div className="prop">
                        <img src="../../../src/assets/stocks.png" alt="account icon" />
                        <p>Account ID: 53bafe7c-95cd-49eb-872e-645634208c61</p>
                    </div>
                    <div className="value-info">
                        <p className="portfolio-name">My Portfolio</p>
                        <p className="portfolio-id">3c1b158d-8f5d-4e4b-a75c-ec25c5cde3da</p>
                        <div className="total-value-info">
                            <p className="total-value">$590,173,920.21</p>
                            <div className="gain-percentage-wrapper">
                                <img src="../../../src/assets/arrow-down.png" alt="direction icon" />
                                <p className="gain-percentage">1.72%</p>
                            </div>
                            <p className="gain-value">-10,353,162.61</p>
                        </div>
                        <p className="date">Apr 22, 09:03:14 AM</p>
                    </div>
                </div>
                <div className="chart-and-highlight-container">
                    <div className="performance-chart-container">
                        <canvas id="performance-chart" />
                        <div className="comparison-object-container">
                            {comparisonData.map((comparisonObject, index) => (
                                <div className="comparison-object">
                                    <div className="color-streak" style={{background: index === 0 ? "#4285F4" : "#F9AB00"}}/>
                                    <p className="name">
                                        {comparisonObject.name.length > 18
                                        ? comparisonObject.name.substring(0, 18) + "..."
                                        : comparisonObject.name
                                        }
                                    </p>
                                    <p className="total-value">{comparisonObject.totalValue}</p>
                                    <p className="total-gain-value">{comparisonObject.totalGainValue}</p>
                                    <div className="total-gain-percentage">
                                        <img src="../../../src/assets/up-arrow.png" alt="direction icon" />
                                        <p>{comparisonObject.totalGainPercentage}</p>
                                    </div>
                                    <button className="remove-btn">
                                        <img src="../../../src/assets/x-white.png" alt="remove icon" />
                                    </button>
                                </div>
                            ))}
                        </div>
                        <div className="chart-buttons">
                            <button className="add-comparison-btn" onClick={() => {setOpenComparisonObjectsList(true)}}>
                                <img src="../../../src/assets/add.png" alt="add icon" />
                                <p>Add comparison</p>
                            </button>
                            {comparisonData && comparisonData.length > 0 && (
                                <button className="clear-all-btn">
                                    Clear all
                                </button>
                            )}
                            {openComparisonObjectsList && (
                                <div className="comparison-objects-list-container">
                                <div className="search-bar">
                                    <input placeholder="Search for stocks, ETFs & more to compare with"></input>
                                    <button className="close-btn" onClick={() => {
                                        setOpenComparisonObjectsList(false)
                                    }}>
                                        <img src="../../../src/assets/x-white.png" alt="close icon"/>
                                    </button>
                                </div>
                                <div className="comparison-objects-list">
                                    {comparisonData.map(comparisonObject => (
                                        <div className="comparison-object">
                                            <div className="comparison-object-info">
                                                <p className="name">{comparisonObject.name}</p>
                                                <p className="symbol">{comparisonObject.symbol}</p>
                                            </div>
                                            <div className="value">
                                                <p className="total-value">{comparisonObject.totalValue}</p>
                                                <p className="total-gain-percentage">
                                                    <img src="../../../src/assets/up-arrow.png" alt="direction icon"/>
                                                    <p>{comparisonObject.totalGainPercentage}</p>
                                                </p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                            )}
                        </div>
                        <div className="glider-container">
                            <div className="glider" ref={gliderRef}>
                                {/*<div>Slide 1</div>*/}
                                {/*<div>Slide 2</div>*/}
                                {/*<div>Slide 3</div>*/}
                                {/*<div>Slide 4</div>*/}
                            </div>

                            <button id="glider-prev">«</button>
                            <button id="glider-next">»</button>
                        </div>
                    </div>
                    <div className="highlight-container container">
                        <p className="title">Portfolio highlights</p>
                        <div className="categories">
                            <div className="category-wrapper">
                                <p className="category-name">DAY GAIN</p>
                                <div className="value-wrapper">
                                    <p className="value">-$29,306,412</p>
                                    <div className="percentage">
                                        <img src="../../../src/assets/arrow-down.png" alt="direction icon" />
                                        <p>4.73%</p>
                                    </div>
                                </div>
                            </div>
                            <div className="category-wrapper">
                                <p className="category-name">TOTAL GAIN</p>
                                <div className="value-wrapper">
                                    <p className="value">-$523,651,693,408</p>
                                    <div className="percentage">
                                        <img src="../../../src/assets/arrow-down.png" alt="direction icon" />
                                        <p>99.58%</p>
                                    </div>
                                </div>
                            </div>
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
                            dataSource={mockData}
                            columns={columns}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Portfolio;
