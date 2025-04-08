// import React, { useEffect, useState } from "react";
// import { useLocation, useNavigate } from "react-router-dom";
// import "./HeaderNavbar.css";

// const HeaderNavbar = () => {
//   const location = useLocation();
//   const pageNames = {
//     "/home": "Home",
//     "/wallet": "Wallet",
//     "/support": "Support",
//     "/": "Login",
//     "/register": "Register",
//     "/forget-password": "Forget Password",
//     "/setting": "Setting",
//   };

//   const [username, setUsername] = useState("");
//   const [menuOpen, setMenuOpen] = useState(false);
//   const navigate = useNavigate();
//   useEffect(() => {
//     const name = localStorage.getItem("username");
//     if (name) {
//       setUsername(name);
//     }
//   }, []);

//   const showMenu = () => {
//     setMenuOpen(!menuOpen);
//   };
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
//   return (
//     <div className="topbar">
//       <h2 className="pageTitle">{pageNames[location.pathname]}</h2>
//       <div className="info">
//         <p className="avatar">{username.charAt(0).toUpperCase()}</p>
//         <p className="user">@{username || "Guest"} </p>
//         <img src="/downButton.svg" alt="Menu" onClick={showMenu} />

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
//     </div>
//   );
// };

// export default HeaderNavbar;


import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./HeaderNavbar.css";

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
  const [transactionHistoryOpen, setTransactionHistoryOpen] = useState(false); // Trạng thái để hiển thị lịch sử giao dịch
  const [transactionHistory, setTransactionHistory] = useState([]); // Trạng thái lưu trữ lịch sử giao dịch
  const navigate = useNavigate();

  useEffect(() => {
    const name = localStorage.getItem("username");
    if (name) {
      setUsername(name);
    }

    // Giả lập lấy lịch sử giao dịch từ API
    const getTransactionHistory = async () => {
      // Giả lập lịch sử giao dịch từ API
      const history = [
        { id: 1, date: "2025-03-20", description: "Deposit", amount: "$1000" },
        { id: 2, date: "2025-03-19", description: "Withdrawal", amount: "$200" },
        { id: 3, date: "2025-03-18", description: "Deposit", amount: "$500" },
      ];
      setTransactionHistory(history);
    };

    getTransactionHistory();
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
          <img src="/img/notification.png" alt="Notifications" />
        </div>

        <img className="dropdown-button" src="/downButton.svg" alt="Menu" onClick={showMenu} />

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
        <div className="transaction-history">
          <h3>Transaction History</h3>
          <ul>
            {transactionHistory.map((transaction) => (
              <li key={transaction.id}>
                <p><strong>Date:</strong> {transaction.date}</p>
                <p><strong>Description:</strong> {transaction.description}</p>
                <p><strong>Amount:</strong> {transaction.amount}</p>
              </li>
            ))}
          </ul>
          <button onClick={toggleTransactionHistory}>Close</button>
        </div>
      )}
    </div>
  );
};

export default HeaderNavbar;
