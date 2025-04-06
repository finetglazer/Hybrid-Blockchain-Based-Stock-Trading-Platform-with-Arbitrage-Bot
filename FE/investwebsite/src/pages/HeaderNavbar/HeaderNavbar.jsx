import React, { useEffect, useState } from "react";
import "./HeaderNavbar.css";
import { useLocation, useNavigate } from "react-router-dom";

const HeaderNavbar = () => {
  const location = useLocation();
  const pageNames = {
    "/home": "Home",
    "/wallet": "Wallet",
    "/support": "Support",
    "/": "Login",
    "/register": "Register",
    "/forget-password": "Forget Password",
    "/setting": "Setting",
  };

  const [username, setUsername] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  useEffect(() => {
    const name = localStorage.getItem("username");
    if (name) {
      setUsername(name);
    }
  }, []);

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
  return (
    <div className="topbar">
      <h2 className="pageTitle">{pageNames[location.pathname]}</h2>
      <div className="info">
        <p className="avatar">{username.charAt(0).toUpperCase()}</p>
        <p className="user">@{username || "Guest"} </p>
        <img src="/downButton.svg" alt="Menu" onClick={showMenu} />

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
    </div>
  );
};

export default HeaderNavbar;
