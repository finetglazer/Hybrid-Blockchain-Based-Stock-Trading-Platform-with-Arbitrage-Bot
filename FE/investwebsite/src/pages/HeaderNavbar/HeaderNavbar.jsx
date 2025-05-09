import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./HeaderNavbar.css";
import axios from "axios";

const HeaderNavbar = () => {
  const location = useLocation();
  const pageNames = {
    "/home": "Home",
    "/trading-accounts": "Trading Accounts",
    "/support": "Support",
    "/": "Login",
    "/register": "Register",
    "/forget-password": "Forget Password",
    "/setting": "Setting",
  };

  const [username, setUsername] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [transactionHistoryOpen, setTransactionHistoryOpen] = useState(false); // Trạng thái để hiển thị lịch sử giao dịch
  // eslint-disable-next-line no-unused-vars
  const [transaction, setTransactions] = useState([]); // Trạng thái lưu trữ lịch sử giao dịch
  const navigate = useNavigate();

  const showMenu = () => {
    setMenuOpen((prev) => {
      if (!prev) setTransactionHistoryOpen(false); // Tắt notification nếu đang mở
      return !prev;
    });
  };

  const handleLogout = async () => {
    try {
      await fetch("/api/users/api/v1/auth/logout", {
        method: "POST",
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      });
      localStorage.removeItem("token");
      localStorage.removeItem("username");
      setUsername("");
      setMenuOpen(false);
      navigate("/");
    } catch (error) {
      console.error("Logout failed", error);
    }
  };

  const toggleTransactionHistory = () => {
    setTransactionHistoryOpen((pre) => {
      if (!pre) setMenuOpen(false);
      return !pre;
    });
  };
  useEffect(() => {
    const savedUsername = localStorage.getItem("username");
    if (savedUsername) {
      setUsername(savedUsername);
    }
  }, []);
  useEffect(() => {
    const fetchTransactions = async () => {
      const res = await axios.get("accounts/transactions/api/v1/get", {
        headers: { Authorization: `Bear ${localStorage.getItem("token")}` },
      });
      const data = await res.json();
      setTransactions(data);
    };

    fetchTransactions();
  }, []);
  return (
    <div className="topbar">
      <h2 className="pageTitle">{pageNames[location.pathname]}</h2>
      <div className="info">
        <p className="avatar">{username.charAt(0).toUpperCase()}</p>
        <p className="user">{username || "Guest"} </p>

        {/* Thêm biểu tượng chuông */}
        <div className="notification-icon" onClick={toggleTransactionHistory}>
          <img
            src="/notification.svg"
            alt="Notifications"
            style={{ marginTop: "20px" }}
          />
        </div>

        <img
          className="dropdown-button"
          src="/dropdownButtom.svg"
          alt="Menu"
          onClick={showMenu}
          style={{ marginRight: "10px" }}
        />

        {menuOpen && (
          <div>
            <ul className="dropdown-menu">
              <li
                onClick={() => {
                  showMenu();
                  navigate("/setting");
                }}
              >
                Setting
              </li>

              <li
                onClick={() => {
                  handleLogout();
                }}
                style={{ color: "red", fontWeight: "bold" }}
              >
                Log Out
              </li>
            </ul>
          </div>
        )}
      </div>

      {/* Hiển thị lịch sử giao dịch */}
      {transactionHistoryOpen && (
        <div className="transaction-dropdown">
          <ul>
            {transaction.length > 0 ? (
              transaction.map((tx) => (
                <li key={tx.id}>
                  <p>{tx.content}</p>
                  <small>{tx.date}</small>
                </li>
              ))
            ) : (
              <li>
                <p>Không có giao dịch nào</p>
              </li>
            )}
          </ul>
        </div>
      )}
    </div>
  );
};

export default HeaderNavbar;
