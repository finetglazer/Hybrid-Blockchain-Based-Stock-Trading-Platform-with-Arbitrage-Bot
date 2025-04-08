import React from "react";
import { useNavigate } from "react-router-dom";
import "./NavbarSide.css";
const NavbarSide = () => {
  const navigate = useNavigate();

  return (
    <div className="sidebar">
      <img className="symbol" src="/img/symbol.png" alt="Invest"/>
      <ul className="nav flex-column">
        <ol className="nav-item">
          <button onClick={() => navigate("/home")}><img className="sidebar-icon" src="/img/home.png"/>Home</button>
        </ol>
        <ol className="nav-item">
          <button onClick={() => navigate("/wallet")}><img className="sidebar-icon" src="/img/wallet.png"/>Wallet</button>
        </ol>
        <ol className="nav-item">
          <button onClick={() => navigate("/support")}><img className="sidebar-icon" src="/img/support.png"/>Support</button>
        </ol>
      </ul>
    </div>
  );
};
export default NavbarSide;
