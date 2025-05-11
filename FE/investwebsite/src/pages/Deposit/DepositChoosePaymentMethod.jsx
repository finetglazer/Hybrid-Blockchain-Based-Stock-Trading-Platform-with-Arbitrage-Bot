import {useParams} from "react-router-dom";
import {Alert, Tag} from "antd";
import {useEffect, useState} from "react";
import axios from "axios";
import "./DepositChoosePaymentMethod.css";

const DepositChoosePaymentMethod = () => {
    const accountId = useParams().accountId;

    const [account, setAccount] = useState();
    const [fetchAccountLoading, setFetchAccountLoading] = useState(true);
    const [fetchPaymentMethodLoading, setFetchPaymentMethodLoading] = useState(true);
    const [fetchAccountError, setFetchAccountError] = useState("");
    const [fetchPaymentMethodError, setFetchPaymentMethodError] = useState("");
    const [category, setCategory] = useState("all");
    const [paymentMethods, setPaymentMethods] = useState([]);
    const [displayedPaymentMethods, setDisplayedPaymentMethods] = useState([]);

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
        return (decimal < 0 || decimal[0] === "-" ? "-" : "") + res + (isDecimal ? "." + str.split(".")[1] : "");
    };

    const onClickAddNewPaymentMethodBtn = () => {
        window.location.replace("/payment-methods");
    };

    useEffect(() => {
        const fetchAccount = async() => {
            const token = localStorage.getItem("token");
            try {
                setFetchAccountLoading(true);
                const response = await axios.get(`/accounts/api/v1/${accountId}`, {
                    headers: {
                        "Authorization": "Bearer " + token,
                        "Content-Type": "application/json"
                    }
                });
                setFetchAccountLoading(false);
                if (response.data && response.data.status === 1) {
                    setFetchAccountError("");
                    setAccount(response.data.data);
                } else {
                    setFetchAccountError(response.data.msg);
                }
            } catch (e) {
                setFetchAccountLoading(false);
                setFetchAccountError(e.message);
            }
        }

        fetchAccount().then(() => {});
    }, [accountId]);

    useEffect(() => {
        const fetchPaymentMethod = async() => {
            const token = localStorage.getItem("token");
            try {
                setFetchPaymentMethodLoading(true);
                const response = await axios.get("/accounts/payment-methods/api/v1/me/get", {
                    headers: {
                        "Authorization": "Bearer " + token,
                        "Content-Type": "application/json"
                    }
                });
                setFetchPaymentMethodLoading(false);
                if (response.data && response.data.status === 1) {
                    setFetchPaymentMethodError("");
                    setPaymentMethods(response.data.data.items);
                    setCategory("all");
                    setDisplayedPaymentMethods(response.data.data.items);
                } else {
                    setFetchPaymentMethodError(response.data.msg);
                }
            } catch (e) {
                setFetchPaymentMethodLoading(false);
                setFetchPaymentMethodError(e.message);
            }
        }

        fetchPaymentMethod().then(() => {});
    }, [accountId]);

    useEffect(() => {
        switch (category) {
            case "all":
                setDisplayedPaymentMethods(paymentMethods);
                break;
            case "bank_account":
                setDisplayedPaymentMethods(paymentMethods.filter(paymentMethod => paymentMethod.type === "BANK_ACCOUNT"));
                break;
            case "credit_card":
                setDisplayedPaymentMethods(paymentMethods.filter(paymentMethod => paymentMethod.type === "CREDIT_CARD"));
                break;
            case "debit_card":
                setDisplayedPaymentMethods(paymentMethods.filter(paymentMethod => paymentMethod.type === "DEBIT_CARD"));
                break;
            case "digital_wallet":
                setDisplayedPaymentMethods(paymentMethods.filter(paymentMethod => paymentMethod.type === "DIGITAL_WALLET"));
                break;
        }
    }, [category]);


    return (
        <div className="container"
            style={{
                minWidth: "70vw",
                minHeight: "100vh",
                fontFamily: "Open Sans"
            }}
        >
            {fetchAccountLoading &&
                <div style={{display: "flex"}}>
                    <div className="spinner" style={{width: 40, height: 40}}/>
                    <p style={{
                        marginLeft: 20
                    }}> Fetching your account... </p>
                </div>
            }
            {fetchAccountError &&
                <Alert showIcon type="error" description={fetchAccountError} />
            }
            {fetchPaymentMethodError &&
                <Alert showIcon type="error" description={fetchPaymentMethodError} />
            }
            {!fetchAccountLoading && !fetchAccountError &&
                <>
                    <div>
                        <p style={{
                            textAlign: "left",
                            fontSize: "2.2rem",
                            marginTop: 0,
                            marginBottom: 40
                        }}>Deposit</p>
                        <div style={{
                            backgroundColor: "rgba(52,133,86,0.58)",
                            borderRadius: 15,
                            display: "flex"
                        }}>
                            <img src="../../../src/assets/dollar.png" alt="account icon"
                                 style={{
                                    width: 60,
                                    height: 60,
                                    margin: "10px 0 0 5px"
                                }}
                            />
                            <div style={{
                                textAlign: "left",
                                marginLeft: 10
                            }}>
                                <div style={{
                                    display: "flex",
                                    alignItems: "center",
                                }}>
                                    <Tag bordered={false} style={{height: 22}} color={(() => {
                                        switch (account.status) {
                                            case "ACTIVE":
                                                return "green";
                                            case "INACTIVE":
                                                return "red";
                                            case "RESTRICTED":
                                                return "warning";
                                            default:
                                                return "black";
                                        }
                                    })()}>{account.status ?? "UNDEFINED"}</Tag>
                                    <p className="mt-2.5">{account.nickname}</p>
                                </div>
                                <p style={{
                                    marginTop: -5,
                                }}>${convert(account.balance.total.toFixed(2))}</p>
                            </div>
                        </div>
                    </div>

                    <div>
                        <div style={{
                            display: "flex",
                            marginTop: 20,
                        }}>
                            <button className="payment-method-list-btn"
                                    onClick={() => {setCategory("all")}}
                                    style={{background: category === "all" ? "tomato" : "#46ad46"}}
                            >
                                All
                            </button>
                            <button className="payment-method-list-btn"
                                    onClick={() => {setCategory("bank_account")}}
                                    style={{background: category === "bank_account" ? "tomato" : "#46ad46"}}
                            >
                                Bank Account
                            </button>
                            <button className="payment-method-list-btn"
                                    onClick={() => {setCategory("credit_card")}}
                                    style={{background: category === "credit_card" ? "tomato" : "#46ad46"}}
                            >
                                Credit Card
                            </button>
                            <button className="payment-method-list-btn"
                                    onClick={() => {setCategory("debit_card")}}
                                    style={{background: category === "debit_card" ? "tomato" : "#46ad46"}}
                            >
                                Debit Card
                            </button>
                            <button className="payment-method-list-btn"
                                    onClick={() => {setCategory("digital_wallet")}}
                                    style={{background: category === "digital_wallet" ? "tomato" : "#46ad46"}}
                            >
                                Digital Wallet
                            </button>
                        </div>
                    </div>
                    {fetchPaymentMethodLoading &&
                        <div style={{display: "flex"}}>
                            <div className="spinner" style={{width: 40, height: 40}}/>
                            <p style={{
                                marginLeft: 20
                            }}> Fetching your payment methods... </p>
                        </div>
                    }
                    {!fetchPaymentMethodError && !fetchPaymentMethodError &&
                        <>
                            <div style={{marginTop: 40, textAlign: "right"}}>
                                <button style={{background: "#1043a4", fontSize: "1.1rem", display: "flex", height: 40}} onClick={onClickAddNewPaymentMethodBtn}>
                                    <img src="../../../src/assets/add.png" width={15} height={10} alt="add icon"/>
                                    <p style={{marginLeft: 10, marginTop: -5}}>Add new payment method</p>
                                </button>
                            </div>
                            <div style={{
                                display: "grid",
                                gridTemplateColumns: `repeat(2, 50%)`,
                                marginTop: 10,
                            }}>
                                {!displayedPaymentMethods.length &&
                                    <div style={{
                                        display: "flex",
                                        alignItems: "center",
                                        marginTop: "45%",
                                        marginLeft: "80%",
                                    }}>
                                        <img src="../../../src/assets/empty.png" width={60} height={60} alt="not found icon"/>
                                        <p style={{fontSize: "2.2rem", marginTop: 10, marginLeft: 20}}>Empty</p>
                                    </div>
                                }
                                {displayedPaymentMethods.map(paymentMethod => (
                                    <div style={{
                                        borderRadius: 10,
                                        background: "rgba(52,133,86,0.58)",
                                        boxShadow: "none",
                                        cursor: "pointer",
                                        height: "210px"
                                    }}
                                        className="container"
                                        onClick={() => {window.location.replace(`/${accountId}/deposit/${paymentMethod.id}`)}}
                                    >
                                        <div style={{
                                            display: "flex",
                                            justifyContent: "space-between",
                                            alignItems: "center",
                                            marginTop: -10,
                                            textAlign: "left"
                                        }}>
                                            <p style={{
                                                fontSize: "1.1rem"
                                            }}>
                                                {paymentMethod.nickname}
                                            </p>
                                            <img src={"../../../src/assets/" +
                                            (paymentMethod.type === "BANK_ACCOUNT" ? "atm-card.png"
                                                : (paymentMethod.type === "CREDIT_CARD" ? "credit-card.png"
                                                    : (paymentMethod.type === "DEBIT_CARD" ? "debit-card.png" : "digital-wallet.png")))
                                            }
                                                 style={{
                                                     width: 50,
                                                     height: 50,
                                                     marginTop: -10
                                                 }}
                                                 alt="payment method icon" />
                                        </div>
                                        <div style={{textAlign: "left", marginTop: -5}}>
                                            <Tag bordered={false}
                                                 color={(() => {
                                                     switch (paymentMethod.status) {
                                                         case "ACTIVE":
                                                             return "green";
                                                         case "INACTIVE":
                                                             return "red";
                                                         case "VERIFICATION_PENDING":
                                                             return "warning";
                                                         default:
                                                             return "black";
                                                     }
                                                 })()}>{paymentMethod.status}</Tag>
                                        </div>
                                        <div style={{
                                            display: "flex",
                                            marginTop: 20
                                        }}>
                                            <p style={{
                                                width: "45%",
                                                textAlign: "left",
                                                color: "#d7d6d6",
                                                fontSize: "0.9rem"
                                            }}>
                                                Commission
                                            </p>
                                            <p style={{
                                                fontSize: "0.9rem"
                                            }}>
                                                0%
                                            </p>
                                        </div>
                                        <div style={{
                                            display: "flex",
                                            marginTop: -10
                                        }}>
                                            <p style={{
                                                width: "45%",
                                                textAlign: "left",
                                                color: "#d7d6d6",
                                                fontSize: "0.9rem"
                                            }}>
                                                Average time
                                            </p>
                                            <p style={{
                                                fontSize: "0.9rem"
                                            }}>
                                                15-20 min
                                            </p>
                                        </div>
                                        <div style={{
                                            display: "flex",
                                            marginTop: -10,
                                        }}>
                                            <p style={{
                                                width: "45%",
                                                textAlign: "left",
                                                color: "#d7d6d6",
                                                fontSize: "0.9rem"
                                            }}>
                                                Payment limit
                                            </p>
                                            <p style={{
                                                fontSize: "0.9rem"
                                            }}>
                                                {convert(1000.0.toFixed(2)) + " - " + convert(3000000.0.toFixed(2))}$
                                            </p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </>
                    }
                </>
            }
        </div>
    );
};

export default DepositChoosePaymentMethod;
