import React, { useEffect, useState, useRef } from "react";
import "./../../index.css";
import "./Wallet.css";
//import { useNavigate } from "react-router-dom";
import axios from "axios";
const Wallet = () => {
  const [tradingAccount, setTradingAccount] = useState([]);
  //const navigate = useNavigate();
  const walletListRef = useRef(null);

  const getRandomColorClass = () => {
    const colorIndex = Math.floor(Math.random() * 6) + 1; // 1 đến 6
    return `color-${colorIndex}`;
  };
  const fetchTradingAccounts = async () => {
    const token = localStorage.getItem("token");
    try {
      const res = await axios.get("/accounts/api/v1/get", {
        headers: { Authorization: `Bearer ${token}` },
      });
      const account = res.data.data.items;
      setTradingAccount(account);
    } catch (error) {
      console.log(error);
    }
  };
  useEffect(() => {
    fetchTradingAccounts();
  }, []);
  return (
    <div className="main-background">
      <div className="wallets-grid" ref={walletListRef}>
        {Array.isArray(tradingAccount) &&
          tradingAccount.map((account, index) => {
            const total =
              parseFloat(account.balance.available) +
              parseFloat(account.balance.reserved);
            return (
              <div key={index} className="wallet-card">
                {/* PHẦN TRÊN */}
                <div className="wallet-main">
                  <div className="wallet-info">
                    <div className={`wallet-icon ${getRandomColorClass()}`} />

                    <div className="wallet-name">{account.nickname}</div>
                  </div>
                  <div className="wallet-row">
                    <span
                      className="wallet-currency"
                      style={{
                        fontSize: "20px",
                        fontWeight: "bold",
                        color: "white",
                      }}
                    >
                      {account.balance.currency}
                    </span>
                    <span className="wallet-balance">{total.toFixed(2)}</span>
                  </div>
                </div>

                {/* PHẦN DƯỚI */}
                <div className="wallet-details">
                  <div className="wallet-detail-row">
                    <span className="wallet-label">Available:</span>
                    <span className="wallet-balance">
                      {account.balance.available}
                    </span>
                  </div>
                  <div className="wallet-detail-row">
                    <span className="wallet-label">Reserved:</span>
                    <span className="wallet-balance">
                      {account.balance.reserved}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
};
export default Wallet;
