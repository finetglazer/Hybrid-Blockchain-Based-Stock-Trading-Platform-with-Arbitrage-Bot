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
  const navigate = useNavigate();

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
      setTradingAccount(accounts);
    } catch (error) {
      console.error(error);
    }
  };
  const getRandomColorClass = () => {
    const colorIndex = Math.floor(Math.random() * 6) + 1; // 1 đến 6
    return `color-${colorIndex}`;
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
              <h1 style={{ color: "#ffffff", marginLeft: "10px", fontWeight: "bold" }}>
                Trading Accounts
              </h1>
            </div>

            <div className="wallets-scroll-wrapper">
              {Array.isArray(tradingAccount) && tradingAccount.length > 0 && (
                <button
                  className="scroll-btn left"
                  onClick={() => scrollWallets(-1)}
                >
                  ←
                </button>
              )}

              <div className="wallets-list" ref={walletListRef}>
                {Array.isArray(tradingAccount) &&
                  tradingAccount.map((account, index) => (
                    <div key={index} className="wallet-card">
                      <div className="wallet-info">
                        <div
                          className={`wallet-icon ${getRandomColorClass()}`}
                        />
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
                        <button className="wallet-btn" onClick={() => navigate(`/${account.id}/deposit/choose-payment-method`)}>Deposit</button>
                        <button className="wallet-btn" onClick={() => navigate(`/${account.id}/withdraw/choose-payment-method`)}>Withdraw</button>
                      </div>
                    </div>
                  ))}
              </div>
              {Array.isArray(tradingAccount) && tradingAccount.length > 0 && (
                <button
                  className="scroll-btn right"
                  onClick={() => scrollWallets(1)}
                >
                  →
                </button>
              )}
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
