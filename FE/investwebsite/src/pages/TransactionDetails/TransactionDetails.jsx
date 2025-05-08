import {Link, useParams} from "react-router-dom";
import React, {useEffect, useState} from "react";
import {Alert, Badge, Breadcrumb, Tag} from "antd";
import axios from "axios";
import "./TransactionDetails.css";

const TransactionDetails = () => {
    const {transactionId} = useParams();

    const [transaction, setTransaction] = useState({
        id: "67e6ac805916dc16cad32fba",
        accountId: "67e7a49c2d17777178ad0164",
        type: "DEPOSIT",
        status: "COMPLETED",
        amount: 1000.5,
        currency: "USD",
        fee: 10.5,
        description: "Deposit into savings account",
        createdAt: "2025-03-20T07:15:00Z",
        updatedAt: "2025-03-20T07:20:00Z",
        completedAt: "2025-03-20T07:25:00Z",
        paymentMethodId: "67efe208e5ef12698df2070d",
        externalReferenceId: "extRef001",
    });
    const [paymentMethod, setPaymentMethod] = useState({
        id: "67efe208e5ef12698df2070d",
        nickname: "Hung's Updated Bank Account",
        maskedNumber: "*****4321",
        type: "BANK_ACCOUNT",
        status: "ACTIVE",
        metadata: {
            accountHolderName: "aa",
            accountNumber: "987654321",
            bankName: "MB",
            routingNumber: "123456789",
            verificationMethod: "MICRO_DEPOSITS",
            verificationRequired: true,
            verifiedAt: "2025-04-10T13:14:23.909+00:00",
        },
    });
    const [account, setAccount] = useState({
        id: "67e7a49c2d17777178ad0164",
        nickname: "VCB visa",
        accountNumber: "TRD-20250329-10030",
        status: "RESTRICTED",
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    
    const resetState = (obj) => {
        Object.keys(obj).forEach((key) => {
            if (typeof obj[key] !== "object") {
                obj[key] = "--";
            }
            else {
                Object.keys(obj[key]).forEach((subKey) => {
                    obj[key][subKey] = "--"; 
                });
            }
        });
        return obj;
    };

    useEffect(() => {
        const getTransactionDetails = async () => {
            const token = localStorage.getItem("token");
            try {
                const response = await axios.get(`/accounts/transactions/api/v1/${transactionId}/details`, {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                });
                setLoading(false);
                if (response.data && response.data.status === 1) {
                    setError("");
                    const transactionDetails = response.data.data;
                    setTransaction(transactionDetails.transaction);
                    setAccount(transactionDetails.account);
                    setPaymentMethod(transactionDetails.paymentMethod);
                }
                else {
                    setError(response.data.msg);
                    resetState(transaction);
                    resetState(paymentMethod);
                    resetState(account);
                }
            }
            catch (e) {
                setLoading(false);
                setError(e.message);
                resetState(transaction);
                resetState(paymentMethod);
                resetState(account);
            }
        }

        getTransactionDetails().then(() => {});
    }, [transactionId]);
    
    useEffect(() => {
        if (loading) {
            setTransaction(resetState({...transaction}));
            setPaymentMethod(resetState({...paymentMethod}));
            setAccount(resetState({...account}));
        }
    }, [loading]);

    return (
        <div className="container transaction-details-container">
            {error &&
                <Alert type="error" showIcon message={error} />
            }
            <Breadcrumb
                className="mb-4"
                separator=">"
                items={[
                    { title: <Link to="/home" style={{ color: "rgba(255, 255, 255, 0.6)" }}>Home</Link> },
                    { title: <Link to="/transaction-history" style={{ color: "rgba(255, 255, 255, 0.6)" }}>Transaction History</Link> },
                    { title: <span style={{ color: "rgba(255, 255, 255, 1)" }}>Transaction Details</span> },
                ]}
            />
            <div className="summary-section">
                <div className="icon">
                    <img src="../../../src/assets/coin.png" alt="amount icon" />
                    <p>AMOUNT</p>
                </div>
                <div className="amount-and-status-wrapper">
                    <p className="amount">{"$" + transaction.amount}</p>
                    <p className="currency">{transaction.currency}</p>
                    <div className="badge-wrapper">
                        {transaction.status === "COMPLETED" ? <Badge status="success" text="COMPLETED" /> : null}
                        {transaction.status === "FAILED" ? <Badge status="error" text="FAILED" /> : null}
                        {transaction.status === "PENDING" ? <Badge status="processing" text="PENDING" /> : null}
                        {transaction.status === "CANCELLED" ? <Badge status="default" text="CANCELLED" /> : null}
                        {loading && <p>--</p>}
                    </div>
                </div>
                <div className="time-wrapper">
                    <div className="label-info-pair created-at-wrapper">
                        <p className="label">Created At</p>
                        <p className="description">{transaction.createdAt}</p>
                    </div>
                    <div className="label-info-pair completed-at-wrapper">
                        <p className="label">Completed At</p>
                        <p className="description">{transaction.completedAt}</p>
                    </div>
                    <div className="label-info-pair updated-at-wrapper">
                        <p className="label">Updated At</p>
                        <p className="description">{transaction.updatedAt}</p>
                    </div>
                </div>
            </div>
            <div className="transaction-details-section">
                <p className="title">Transaction Details</p>
                <div className="properties-wrapper">
                    <div className="property">
                        <p className="name">Id</p>
                        <p className="value">{transaction.id}</p>
                    </div>
                    <div className="property">
                        <p className="name">Account Id</p>
                        <p className="value">{transaction.accountId}</p>
                    </div>
                    <div className="property">
                        <p className="name">Type</p>
                        {transaction.type === "DEPOSIT" && (
                            <Tag bordered={false} color="magenta">DEPOSIT</Tag>
                        )}
                        {transaction.type === "WITHDRAWAL" && (
                            <Tag bordered={false} color="purple">WITHDRAWAL</Tag>
                        )}
                        {transaction.type === "INTEREST" && (
                            <Tag bordered={false} color="cyan">INTEREST</Tag>
                        )}
                        {transaction.type === "FEE" && (
                            <Tag bordered={false} color="red">FEE</Tag>
                        )}
                        {transaction.type === "TRANSFER" && (
                            <Tag bordered={false} color="gold">TRANSFER</Tag>
                        )}
                        {transaction.type === "ORDER_PAYMENT" && (
                            <Tag bordered={false} color="lime">ORDER PAYMENT</Tag>
                        )}
                        {transaction.type === "REFUND" && (
                            <Tag bordered={false} color="geekblue">REFUND</Tag>
                        )}
                    </div>
                    <div className="property">
                        <p className="name">Status</p>
                        {transaction.status === "PENDING" && (
                            <Tag bordered={false} color="cyan-inverse">PENDING</Tag>
                        )}
                        {transaction.status === "COMPLETED" && (
                            <Tag bordered={false} color="green-inverse">COMPLETED</Tag>
                        )}
                        {transaction.status === "FAILED" && (
                            <Tag bordered={false} color="red-inverse">FAILED</Tag>
                        )}
                        {transaction.status === "CANCELLED" && (
                            <Tag bordered={false} color="default">CANCELLED</Tag>
                        )}
                    </div>
                    <div className="property">
                        <p className="name">Amount</p>
                        <p className="value">{transaction.amount}</p>
                    </div>
                    <div className="property">
                        <p className="name">Currency</p>
                        <p className="value">{transaction.currency}</p>
                    </div>
                    <div className="property">
                        <p className="name">Fee</p>
                        <p className="value">{transaction.fee}</p>
                    </div>
                    <div className="property">
                        <p className="name">Description</p>
                        <p className="value">{transaction.description}</p>
                    </div>
                </div>
            </div>

            <div className="account-details-section">
                <p className="title">Account Details</p>
                <div className="properties-wrapper">
                    <div className="property">
                        <p className="name">Id</p>
                        <p className="value">{account.id}</p>
                    </div>
                    <div className="property">
                        <p className="name">Nickname</p>
                        <p className="value">{account.nickname}</p>
                    </div>
                    <div className="property">
                        <p className="name">Account number</p>
                        <p className="value">{account.accountNumber}</p>
                    </div>
                    <div className="property">
                        <p className="name">Status</p>
                        {account.status === "ACTIVE" && (
                            <Tag bordered={false} color="green-inverse">ACTIVE</Tag>
                        )}
                        {account.status === "INACTIVE" && (
                            <Tag bordered={false} color="default">INACTIVE</Tag>
                        )}
                        {account.status === "RESTRICTED" && (
                            <Tag bordered={false} color="red-inverse">RESTRICTED</Tag>
                        )}
                    </div>
                </div>
            </div>

            <div className="payment-method-details-section">
                <p className="title">Payment Method Details</p>
                <div className="properties-wrapper">
                    <div className="property">
                        <p className="name">Id</p>
                        <p className="value">{paymentMethod.id}</p>
                    </div>
                    <div className="property">
                        <p className="name">Nickname</p>
                        <p className="value">{paymentMethod.nickname}</p>
                    </div>
                    <div className="property">
                        <p className="name">Masked number</p>
                        <p className="value">{paymentMethod.maskedNumber}</p>
                    </div>
                    <div className="property">
                        <p className="name">Type</p>
                        {paymentMethod.type === "BANK_ACCOUNT" && (
                            <Tag icon={<img src="../../../src/assets/atm-card.png" alt="bank account icon" />}
                                bordered={false} color="geekblue-inverse"
                            >Bank account</Tag>
                        )}
                    </div>
                    <div className="property">
                        <p className="name">Status</p>
                        {paymentMethod.status === "ACTIVE" && (
                            <Tag bordered={false} color="green-inverse">ACTIVE</Tag>
                        )}
                        {paymentMethod.status === "INACTIVE" && (
                            <Tag bordered={false} color="default">INACTIVE</Tag>
                        )}
                        {paymentMethod.status === "VERIFICATION_PENDING" && (
                            <Tag bordered={false} color="cyan-inverse">VERIFICATION PENDING</Tag>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TransactionDetails;
