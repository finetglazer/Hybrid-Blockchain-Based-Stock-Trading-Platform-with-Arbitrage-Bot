import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from "@mui/material";
import React, { useEffect, useState } from "react";
import CreateAccount from "../CreateAccount";
import "./Home.css";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const Home = () => {
  /*const [wallets, setWallets] = useState([
    {
      nickname: "MB Bank",
      currency: "VND",
    },
  ]);*/
  const [tradingAccount, setTradingAccount] = useState([]);
  const [open, setOpen] = useState(false);
  const nativigate = useNavigate();
  const handleAccountCreated = () => {
    setOpen(false);
    fetchTradingAccounts();
  };

  const fetchTradingAccounts = async () => {
    const token = localStorage.getItem("token");
    try {
      const res = await axios.get(
        "https://good-musical-joey.ngrok-free.app//accounts/api/v1/get",
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      const accounts = res.data.items;
      console.log(res.data);
      //console.log("Array.isArray?", Array.isArray(accounts));
      setTradingAccount(accounts);
    } catch (error) {
      console.error(error);
    }
  };
  useEffect(() => {
    fetchTradingAccounts();
  }, []);
  return (
    <>
      <div className="main-background">
        <div className="main-content-row">
          {/* Wallets Section - BÊN TRÁI */}
          <div className="wallets-section">
            <div className="wallets-header">
              <h1 style={{ color: "#ffffff" }}>Wallets</h1>
              <p
                onClick={() => nativigate("/wallet")}
                style={{
                  color: "#ffffff",
                  fontWeight: "bold",
                  textDecoration: "none",
                  left: 0,
                }}
              >
                View all
              </p>
            </div>

            <div className="wallets-list">
              {Array.isArray(tradingAccount) &&
                tradingAccount.map((account, index) => (
                  <div key={index} className="wallet-card">
                    <div className="wallet-info">
                      <div className="wallet-icon" />
                      <div className="wallet-name">{account.nickname}</div>
                    </div>
                    <div className="wallet-currency">{account.currency}</div>
                    <div className="wallet-actions">
                      <button className="wallet-btn">Deposit</button>
                      <button className="wallet-btn">Withdraw</button>
                    </div>
                  </div>
                ))}
            </div>
          </div>

          {/* Trade Section - BÊN PHẢI */}
          <div className="trade-section">
            <h1 style={{ color: "#ffffff" }}>Trade</h1>
            <div className="trade-options">
              {[
                { label: "Buy", icon: "+" },
                { label: "Sell", icon: "-" },
                { label: "Swap", icon: "⇄" },
              ].map((action, idx) => (
                <div key={idx} className="trade-item">
                  <div className="trade-icon">{action.icon}</div>
                  <div className="trade-label">{action.label}</div>
                </div>
              ))}
            </div>
          </div>
          <div className="create-trading-account">
            <Button
              variant="contained"
              color="primary"
              onClick={() => setOpen(true)}
              className="btn-add-account"
            >
              +
            </Button>
            {/* Dialog tạo tài khoản */}
            <Dialog
              open={open}
              onClose={() => setOpen(false)}
              maxWidth="sm"
              fullWidth
            >
              <DialogTitle style={{ color: "black", paddingBottom: "10px" }}>
                Create a Trading Account
              </DialogTitle>
              <DialogContent>
                <CreateAccount onSuccess={handleAccountCreated} />
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setOpen(false)} color="secondary">
                  Cancel
                </Button>
              </DialogActions>
            </Dialog>
          </div>
        </div>
      </div>
    </>
  );
};

export default Home;
