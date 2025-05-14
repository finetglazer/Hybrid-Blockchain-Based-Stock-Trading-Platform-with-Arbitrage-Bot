// import React, { useEffect, useState } from "react";
// import { useLocation, useNavigate } from "react-router-dom";
// import "./HeaderNavbar.css";
// import axios from "axios";
//
// const HeaderNavbar = () => {
//   const location = useLocation();
//   const pageNames = {
//     "/home": "Home",
//     "/trading-accounts": "Trading Accounts",
//     "/support": "Support",
//     "/": "Login",
//     "/register": "Register",
//     "/forget-password": "Forget Password",
//     "/setting": "Setting",
//   };
//
//   const [username, setUsername] = useState("");
//   const [menuOpen, setMenuOpen] = useState(false);
//   const [transactionHistoryOpen, setTransactionHistoryOpen] = useState(false); // Trạng thái để hiển thị lịch sử giao dịch
//   // eslint-disable-next-line no-unused-vars
//   const [transaction, setTransactions] = useState([]); // Trạng thái lưu trữ lịch sử giao dịch
//   const navigate = useNavigate();
//
//   const showMenu = () => {
//     setMenuOpen((prev) => {
//       if (!prev) setTransactionHistoryOpen(false); // Tắt notification nếu đang mở
//       return !prev;
//     });
//   };
//
//   const handleLogout = async () => {
//     try {
//       await fetch("/api/users/api/v1/auth/logout", {
//         method: "POST",
//         headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
//       });
//       localStorage.removeItem("token");
//       localStorage.removeItem("username");
//       setUsername("");
//       setMenuOpen(false);
//       navigate("/");
//     } catch (error) {
//       console.error("Logout failed", error);
//     }
//   };
//
//   const toggleTransactionHistory = () => {
//     setTransactionHistoryOpen((pre) => {
//       if (!pre) setMenuOpen(false);
//       return !pre;
//     });
//   };
//   useEffect(() => {
//     const savedUsername = localStorage.getItem("username");
//     if (savedUsername) {
//       setUsername(savedUsername);
//     }
//   }, []);
//   useEffect(() => {
//     const fetchTransactions = async () => {
//       const res = await axios.get("accounts/transactions/api/v1/get", {
//         headers: { Authorization: `Bear ${localStorage.getItem("token")}` },
//       });
//       const data = await res.json();
//       setTransactions(data);
//     };
//
//     fetchTransactions();
//   }, []);
//   return (
//     <div className="topbar">
//       <h2 className="pageTitle">{pageNames[location.pathname]}</h2>
//       <div className="info">
//         <p className="avatar">{username.charAt(0).toUpperCase()}</p>
//         <p className="user">{username || "Guest"} </p>
//
//         {/* Thêm biểu tượng chuông */}
//         <div className="notification-icon" onClick={toggleTransactionHistory}>
//           <img
//             src="/notification.svg"
//             alt="Notifications"
//             style={{ marginTop: "20px" }}
//           />
//         </div>
//
//         <img
//           className="dropdown-button"
//           src="/dropdownButtom.svg"
//           alt="Menu"
//           onClick={showMenu}
//           style={{ marginRight: "10px" }}
//         />
//
//         {menuOpen && (
//           <div>
//             <ul className="dropdown-menu">
//               <li
//                 onClick={() => {
//                   showMenu();
//                   navigate("/setting");
//                 }}
//               >
//                 Setting
//               </li>
//
//               <li
//                 onClick={() => {
//                   handleLogout();
//                 }}
//                 style={{ color: "red", fontWeight: "bold" }}
//               >
//                 Log Out
//               </li>
//             </ul>
//           </div>
//         )}
//       </div>
//
//       {/* Hiển thị lịch sử giao dịch */}
//       {transactionHistoryOpen && (
//         <div className="transaction-dropdown">
//           <ul>
//             {transaction.length > 0 ? (
//               transaction.map((tx) => (
//                 <li key={tx.id}>
//                   <p>{tx.content}</p>
//                   <small>{tx.date}</small>
//                 </li>
//               ))
//             ) : (
//               <li>
//                 <p>No transactions</p>
//               </li>
//             )}
//           </ul>
//         </div>
//       )}
//     </div>
//   );
// };
//
// export default HeaderNavbar;


import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./HeaderNavbar.css"; // We will define this CSS below
import axios from "axios";

const HeaderNavbar = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const pageNames = {
    "/home": "Home",
    "/trading-accounts": "Trading Accounts",
    "/support": "Support",
    "/": "Login", // Assuming login page is at root if not authenticated
    "/register": "Register",
    "/forget-password": "Forget Password",
    "/setting": "Setting",
  };

  const [username, setUsername] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [transactionHistoryOpen, setTransactionHistoryOpen] = useState(false);
  const [transactions, setTransactions] = useState([]);

  // Initialize username from localStorage
  useEffect(() => {
    const savedUsername = localStorage.getItem("username");
    if (savedUsername) {
      setUsername(savedUsername);
    }
  }, []);

  // Fetch transactions (or notifications)
  useEffect(() => {
    const fetchTransactions = async () => {
      const token = localStorage.getItem("token");
      if (!token) {
        // console.log("No token, skipping transaction fetch.");
        return;
      }
      try {
        // Adjust API path if needed (e.g., if it needs /api at the start)
        const res = await axios.get("/api/accounts/transactions/api/v1/get", {
          headers: { Authorization: `Bearer ${token}` }, // Corrected: Bearer
        });
        setTransactions(res.data || []); // Corrected: use res.data for axios, ensure it's an array
      } catch (error) {
        console.error("Failed to fetch transactions:", error);
        setTransactions([]); // Set to empty array on error
      }
    };

    if (username) { // Only fetch if user is logged in
      fetchTransactions();
    }
  }, [username]); // Re-fetch if username changes

  const toggleUserMenu = () => {
    setMenuOpen((prev) => {
      if (!prev) setTransactionHistoryOpen(false); // Close other dropdown
      return !prev;
    });
  };

  const toggleTransactionHistory = () => {
    setTransactionHistoryOpen((prev) => {
      if (!prev) setMenuOpen(false); // Close other dropdown
      return !prev;
    });
  };

  const handleLogout = async () => {
    try {
      await fetch("/api/users/api/v1/auth/logout", { // Ensure this path is correct
        method: "POST",
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      });
    } catch (error) {
      console.error("Logout API call failed, proceeding with client-side logout:", error);
    } finally {
      localStorage.removeItem("token");
      localStorage.removeItem("username");
      setUsername("");
      setMenuOpen(false);
      setTransactionHistoryOpen(false);
      navigate("/");
    }
  };

  const navigateToSettings = () => {
    navigate("/setting");
    setMenuOpen(false);
  };

  const currentPageName = pageNames[location.pathname] || "Page";

  return (
      <div className="topbar">
        <h2 className="pageTitle">{currentPageName}</h2>

        {username && ( // Only show this section if user is logged in
            <div className="user-actions-area">
              <div className="user-profile">
                <span className="avatar">{username.charAt(0).toUpperCase()}</span>
                <span className="username-text">{username}</span>
              </div>

              <div className="icon-group">
                <div className="notification-wrapper">
                  <button
                      type="button"
                      className="icon-button"
                      onClick={toggleTransactionHistory}
                      aria-label="View notifications"
                  >
                    <img src="/notification.svg" alt="Notifications" />
                  </button>
                  {transactionHistoryOpen && (
                      <div className="dropdown transaction-dropdown">
                        <ul className="dropdown-list">
                          {transactions.length > 0 ? (
                              transactions.map((tx) => (
                                  <li key={tx.id || tx.date} className="dropdown-list-item"> {/* Use unique key */}
                                    <p className="transaction-content">{tx.content}</p>
                                    <small className="transaction-date">
                                      {new Date(tx.date).toLocaleDateString()}
                                    </small>
                                  </li>
                              ))
                          ) : (
                              <li className="dropdown-list-item-empty">
                                <p>No new notifications</p>
                              </li>
                          )}
                        </ul>
                      </div>
                  )}
                </div>

                <div className="user-menu-wrapper">
                  <button
                      type="button"
                      className="icon-button"
                      onClick={toggleUserMenu}
                      aria-label="User menu"
                  >
                    <img src="/dropdownButton.svg" alt="Open user menu" /> {/* Verify filename */}
                  </button>
                  {menuOpen && (
                      <div className="dropdown user-menu-dropdown">
                        <ul className="dropdown-list">
                          <li
                              className="dropdown-list-item"
                              onClick={navigateToSettings}
                              role="menuitem"
                              tabIndex={0}
                              onKeyPress={(e) => e.key === 'Enter' && navigateToSettings()}
                          >
                            Setting
                          </li>
                          <li
                              className="dropdown-list-item dropdown-list-item-logout"
                              onClick={handleLogout}
                              role="menuitem"
                              tabIndex={0}
                              onKeyPress={(e) => e.key === 'Enter' && handleLogout()}
                          >
                            Log Out
                          </li>
                        </ul>
                      </div>
                  )}
                </div>
              </div>
            </div>
        )}
      </div>
  );
};

export default HeaderNavbar;
