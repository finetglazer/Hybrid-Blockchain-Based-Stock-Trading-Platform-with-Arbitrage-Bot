import {Alert, Breadcrumb, Table, Tag} from "antd";
import React, {useEffect, useState} from "react";
import "./TransactionHistory.css";
import {useNavigate} from "react-router-dom";
import FilterForm from "./forms/FilterForm.jsx";
import axios from "axios";

const TransactionHistory = () => {
    const navigate = useNavigate();

    const [openFilterForm, setOpenFilterForm] = useState(false);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [totalTransactions, setTotalTransactions] = useState(0);

    const fetchTransactions = async () => {
        const token = localStorage.getItem("token");
        try {
            setLoading(true);
            const response = await axios.post("/accounts/transactions/api/v1/get", {
                page: 0,
                size: 9999,
            }, {
                headers: {
                    "Authorization": "Bearer " + token,
                    "Content-Type": "application/json"
                }
            });
            setLoading(false);
            if (response.data && response.data.status === 1) {
                setError("");
                setTransactions(response.data.data.items);
                setTotalTransactions(response.data.data.paging.totalItems);
            }
            else {
                setError(response.data.msg);
            }
        }
        catch (e) {
            setLoading(false);
            setError(e.message);
        }
    };

    const onClickOpenAdvanceFilterBtn = () => {
        setOpenFilterForm(true);
        const body = document.querySelector(".transaction-history-container");
        body.classList.add("blurred");
    };

    const onSuccess = (transactionsFiltered) => {
        setTransactions(transactionsFiltered);
        closeFilterForm();
    };

    const tableOnChange = (page, pageSize) => {
        setPage(page);
        setPageSize(pageSize);
    };

    const closeFilterForm = () => {
        setOpenFilterForm(false);
        const body = document.querySelector(".transaction-history-container");
        body.classList.remove("blurred");
    };

    useEffect(() => {
        fetchTransactions().then(() => {});
    }, []);

    const columns = [
        {
            title: 'Id',
            dataIndex: 'id',
            key: 'id',
            sorter: (a, b) => a.id.localeCompare(b.id),
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'Account Id',
            dataIndex: 'accountId',
            key: 'accountId',
            sorter: (a, b) => a.accountId.localeCompare(b.accountId),
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'Type',
            dataIndex: 'type',
            key: 'type',
            sorter: (a, b) => a.type.localeCompare(b.type),
            sortDirection: ['ascend', 'descend'],
            render: (value, record) => {
                switch (record.type) {
                    case "DEPOSIT":
                        return (
                            <Tag bordered={false} color="magenta">DEPOSIT</Tag>
                        );
                    case "WITHDRAWAL":
                        return (
                            <Tag bordered={false} color="purple">WITHDRAWAL</Tag>
                        );
                    case "INTEREST":
                        return (
                            <Tag bordered={false} color="cyan">INTEREST</Tag>
                        );
                    case "FEE":
                        return (
                            <Tag bordered={false} color="red">FEE</Tag>
                        );
                    case "TRANSFER":
                        return (
                            <Tag bordered={false} color="gold">TRANSFER</Tag>
                        );
                    case "ORDER_PAYMENT":
                        return (
                            <Tag bordered={false} color="lime">ORDER PAYMENT</Tag>
                        );
                    case "REFUND":
                        return (
                            <Tag bordered={false} color="geekblue">REFUND</Tag>
                        );
                }
            },
            filters: [
                {
                    text: "DEPOSIT",
                    value: "DEPOSIT"
                },
                {
                    text: "WITHDRAWAL",
                    value: "WITHDRAWAL"
                },
                {
                    text: "INTEREST",
                    value: "INTEREST"
                },
                {
                    text: "FEE",
                    value: "FEE"
                },
                {
                    text: "TRANSFER",
                    value: "TRANSFER"
                },
                {
                    text: "ORDER PAYMENT",
                    value: "ORDER_PAYMENT"
                },
                {
                    text: "REFUND",
                    value: "REFUND"
                },
            ],
            onFilter: (value, record) => {
                return value === record.type;
            }
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            sorter: (a, b) => a.status.localeCompare(b.status),
            sortDirection: ['ascend', 'descend'],
            render: (value, record) => {
                switch (record.status) {
                    case "PENDING":
                        return (
                            <Tag bordered={false} color="cyan-inverse">PENDING</Tag>
                        );
                    case "COMPLETED":
                        return (
                            <Tag bordered={false} color="green-inverse">COMPLETED</Tag>
                        );
                    case "FAILED":
                        return (
                            <Tag bordered={false} color="red-inverse">FAILED</Tag>
                        );
                    case "CANCELLED":
                        return (
                            <Tag bordered={false} color="default">CANCELLED</Tag>
                        );
                }
            },
            filters: [
                {
                    text: "PENDING",
                    value: "PENDING"
                },
                {
                    text: "COMPLETED",
                    value: "COMPLETED"
                },
                {
                    text: "FAILED",
                    value: "FAILED"
                },
                {
                    text: "CANCELLED",
                    value: "CANCELLED"
                },
            ],
            onFilter: (value, record) => record.status === value
        },
        {
            title: 'Amount',
            dataIndex: 'amount',
            key: 'amount',
            sorter: (a, b) => a.amount - b.amount,
            render: (record) => {return <p style={{fontWeight: "bold", color: "#ffe58f"}}>{record}</p> },
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'Currency',
            dataIndex: 'currency',
            key: 'currency',
            sorter: (a, b) => a.currency.localeCompare(b.currency),
            sortDirection: ['ascend', 'descend'],
            render: (record) => <p style={{fontWeight: 500, color: "lightcyan"}}>{record}</p>
        },
        {
            title: 'Fee',
            dataIndex: 'fee',
            key: 'fee',
            sorter: (a, b) => a.fee - b.fee,
            render: (record) => {return <p style={{fontWeight: "bold", color: "#fa8c16"}}>{record}</p> },
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'Description',
            dataIndex: 'description',
            key: 'description',
        },
        {
            title: 'Created at',
            dataIndex: 'createdAt',
            key: 'createdAt',
            sorter: (a, b) => a.createdAt.localeCompare(b.createdAt),
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'Updated at',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            sorter: (a, b) => a.updatedAt.localeCompare(b.updatedAt),
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'Completed at',
            dataIndex: 'completedAt',
            key: 'completedAt',
            sorter: (a, b) => a.completedAt.localeCompare(b.completedAt),
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'Payment method Id',
            dataIndex: 'paymentMethodId',
            key: 'paymentMethodId',
            sorter: (a, b) => a.paymentMethodId.localeCompare(b.paymentMethodId),
            sortDirection: ['ascend', 'descend'],
        },
        {
            title: 'External reference Id',
            dataIndex: 'externalReferenceId',
            key: 'externalReferenceId',
            sorter: (a, b) => a.externalReferenceId.localeCompare(b.externalReferenceId),
            sortDirection: ['ascend', 'descend'],
        },
    ];

    return (
    <>
        {openFilterForm &&
            <FilterForm
                onSuccess={onSuccess}
                onCancel={closeFilterForm}
            />
        }
        <div className="container transaction-history-container">
            <Breadcrumb
                className="breadcrumb"
                separator=" "
                items={[
                    {
                        title: (
                            <span style={{ color: "rgba(255, 255, 255, 0.6)" }}>
                                Home
                            </span>
                        ),
                        href: "/home",
                    },
                    {
                        title: (
                            <span style={{ color: "rgba(255, 255, 255, 0.6)" }}> {">"} </span>
                        ),
                    },
                    {
                        title: (
                            <span style={{ color: "rgba(255, 255, 255, 1)" }}>
                                Transaction history
                            </span>
                        ),
                        href: "/home/transaction-history",
                    },
                ]}
            />
            <div className="title">
                <p className="name">Transaction History</p>
                <p className="description">The Transaction History page provides a comprehensive overview of all account activities across your accounts</p>
            </div>
            {loading && (
                <div style={{display: "flex", margin: '10px 0 0 10px', alignItems: 'center'}}>
                    <div className="spinner" style={{width: 30, height: 30}}/>
                    <p style={{marginLeft: 10, fontSize: '1.4rem'}}>Fetching transactions...</p>
                </div>
            )}
            {!loading && error && (
                <Alert type="error" showIcon description={error}/>
            )}
            {!loading && !error && (
            <>
                <button className="open-filter-form-btn" onClick={onClickOpenAdvanceFilterBtn}>
                    <img src="../../../src/assets/filter.png" alt="filter icon" />
                    <p className="title">Advance filter</p>
                </button>
                <Table
                    columns={columns}
                    dataSource={transactions}
                    size="small"
                    showSorterTooltip={{ target: 'sorter-icon' }}
                    onRow={(record) => ({
                        style: {
                            color: "#ecebeb",
                            background: "#45584bc5",
                            cursor: "pointer",
                        },
                        onClick: () => {
                            navigate(`/transaction-history/${record.id}/details`);
                        }
                    })}
                    pagination={{
                        pageSize: pageSize,
                        current: page,
                        total: totalTransactions,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        pageSizeOptions: ['5', '10', '20', '50'],
                        onChange: (page, pageSize) => tableOnChange(page, pageSize),
                    }}
                />
            </>)}
        </div>
    </>
    );
};

export default TransactionHistory;
