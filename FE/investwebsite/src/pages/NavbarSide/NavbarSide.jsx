import React from "react";
import { useNavigate } from "react-router-dom";
import "./NavbarSide.css";
const NavbarSide = () => {
  const navigate = useNavigate();

  return (
    <div className="sidebar">
      <img src="/symbol.png"/>
      <ul className="nav flex-column">
        <ol className="nav-item">
          <button onClick={() => navigate("/home")}>Home</button>
        </ol>
        <ol className="nav-item">
          <button onClick={() => navigate("/payment-methods")}>Payment method</button>
        </ol>
        <ol className="nav-item">
          <button onClick={() => navigate("/support")}>Support</button>
        </ol>
      </ul>
    </div>
  );
};
export default NavbarSide;
