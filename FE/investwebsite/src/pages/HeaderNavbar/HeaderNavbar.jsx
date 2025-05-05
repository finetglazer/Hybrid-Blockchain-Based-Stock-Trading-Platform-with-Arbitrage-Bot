import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./HeaderNavbar.css";
import TransactionHistory from "./TransactionHistoryHeader/TransactionHistoryHeader";

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
  //const [transactionHistory, setTransactionHistory] = useState([]); // Trạng thái lưu trữ lịch sử giao dịch
  const navigate = useNavigate();

  const showMenu = () => {
    setMenuOpen(!menuOpen);
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
    setTransactionHistoryOpen(!transactionHistoryOpen);
  };

  return (
    <div className="topbar">
      <h2 className="pageTitle">{pageNames[location.pathname]}</h2>
      <div className="info">
        <p className="avatar">{username.charAt(0).toUpperCase()}</p>
        <p className="user">{username || "Guest"} </p>

        {/* Thêm biểu tượng chuông */}
        <div className="notification-icon" onClick={toggleTransactionHistory}>
          <img src="/notification.png" alt="Notifications" />
        </div>

        <img
          className="dropdown-button"
          src="/downButton.svg"
          alt="Menu"
          onClick={showMenu}
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
      {transactionHistoryOpen && <TransactionHistory />}
    </div>
  );
};

export default HeaderNavbar;
