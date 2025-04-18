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
      const token = localStorage.getItem("token");
      if (!token) {
        console.warn("No token found, redirecting to login...");
        navigate("/");
        return;
      }

      const response = await fetch(
        "/users/api/v1/auth/logout",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        console.warn(`Logout failed with status: ${response.status}`);
        if (response.status === 401) {
          console.warn("Unauthorized (401) - Token có thể đã hết hạn.");
        }
        return;
      }

      // Kiểm tra response có JSON không trước khi parse
      const text = await response.text();
      if (text) {
        console.log("Server response:", JSON.parse(text));
      }

      localStorage.removeItem("token");
      localStorage.removeItem("username");
      setUsername("");
      setMenuOpen(false);
      navigate("/");
    } catch (error) {
      console.error("Logout error:", error);
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
                  console.log(
                    "Token trước khi logout:",
                    localStorage.getItem("token")
                  );

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
