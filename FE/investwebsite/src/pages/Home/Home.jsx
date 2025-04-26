import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from "@mui/material";
import React, { useEffect, useRef, useState } from "react";
import CreateAccount from "../CreateAccount";
import "./Home.css";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const Home = () => {
  const [tradingAccount, setTradingAccount] = useState([]);
  const [open, setOpen] = useState(false);
  const walletListRef = useRef(null);
  const nativigate = useNavigate();

  const handleAccountCreated = () => {
    setOpen(false);
    fetchTradingAccounts();
  };

  const fetchTradingAccounts = async () => {
    const token = localStorage.getItem("token");
    try {
      const res = await axios.get("/accounts/api/v1/get", {
        headers: { Authorization: `Bearer ${token}` },
      });

      const accounts = res.data.data.items;
      console.log(accounts);
      setTradingAccount(accounts);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchTradingAccounts();
  }, []);

  const scrollWallets = (direction) => {
    const container = walletListRef.current;
    const scrollAmount = 236;

    if (container) {
      container.scrollBy({
        left: direction * scrollAmount,
        behavior: "smooth",
      });
    }
  };

  return (
    <>
      <div className="main-background">
        <div className="main-content-row">
          {/* Wallets Section - BÊN TRÁI */}
          <div className="wallets-section">
            <div className="wallets-header">
              <h1 style={{ color: "#ffffff", marginLeft: "10px" }}>
                Trading Accounts
              </h1>
              <p
                onClick={() => nativigate("/trading-accounts")}
                style={{
                  color: "#ffffff",
                  fontWeight: "bold",
                  textDecoration: "none",
                  right: 0,
                }}
              >
                View all
              </p>
            </div>

            <div className="wallets-scroll-wrapper">
              <button
                className="scroll-btn left"
                onClick={() => scrollWallets(-1)}
              >
                ←
              </button>

              <div className="wallets-list" ref={walletListRef}>
                {Array.isArray(tradingAccount) &&
                  tradingAccount.map((account, index) => (
                    <div key={index} className="wallet-card">
                      <div className="wallet-info">
                        <div className="wallet-icon" />
                        <div className="wallet-name">{account.nickname}</div>
                      </div>
                      <div className="wallet-subinfo">
                        <div className="wallet-currency">
                          {account.balance.currency}
                        </div>
                        <div className="wallet-balance">
                          {account.balance.total}
                        </div>
                      </div>
                      <div className="wallet-actions">
                        <button className="wallet-btn">Deposit</button>
                        <button className="wallet-btn">Withdraw</button>
                      </div>
                    </div>
                  ))}
              </div>

              <button
                className="scroll-btn right"
                onClick={() => scrollWallets(1)}
              >
                →
              </button>
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
