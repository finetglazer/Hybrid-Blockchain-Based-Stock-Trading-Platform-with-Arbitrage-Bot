import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from "@mui/material";
import React, { useState } from "react";
import CreateAccount from "../CreateAccount";
import "./Home.css";

const wallets = [
  {
    name: "MB Bank",
    balance: "$100.000",
    icon: "ETH",
    symbol: "ETH",
  },
];

const Home = () => {
  const [open, setOpen] = useState(false);
  const [numOfAccount, setNumOfAccount] = useState(0);

  const handleAccountCreated = () => {
    setNumOfAccount((prev) => prev + 1);
    setOpen(false);
  };

  return (
    <>
      <div className="main-content-row">
        {/* Wallets Section - BÊN TRÁI */}
        <div className="wallets-section">
          <div className="wallets-header">
            <h1 style={{ color: "#ffffff" }}>Wallets</h1>
            <a
              href="#"
              style={{
                color: "#ffffff",
                fontWeight: "bold",
                textDecoration: "none",
              }}
            >
              View all
            </a>
          </div>

          <div className="wallets-list">
            {wallets.map((wallet, index) => (
              <div key={index} className="wallet-card">
                <div className="wallet-info">
                  <div className="wallet-icon" />
                  <div className="wallet-name">{wallet.name}</div>
                </div>
                <div className="wallet-balance">{wallet.balance}</div>
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
              { label: "Withdraw", icon: "+" },
              { label: "Deposit", icon: "-" },
            ].map((action, idx) => (
              <div key={idx} className="trade-item">
                <div className="trade-icon">{action.icon}</div>
                <div className="trade-label">{action.label}</div>

                
              </div>
            ))}
          </div>

          <div className="create-trading-account">
            <p className="account-question">
              {numOfAccount === 0 ? "Do you have any account?" : ""}
            </p>
            <Button
              variant="contained"
              color="primary"
              onClick={() => setOpen(true)}
              className="btn-add-account"
            >
              +
            </Button>
          </div>
        </div>
      </div>
      {/* Dialog tạo tài khoản */}
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create a Trading Account</DialogTitle>
        <DialogContent>
          <CreateAccount
            numOfAccount={numOfAccount}
            onSuccess={handleAccountCreated}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)} color="secondary">
            Cancel
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default Home;
