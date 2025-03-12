import React from "react";
import { useNavigate } from "react-router-dom";
import "./NavbarSide.css";
const NavbarSide = () => {
  const navigate = useNavigate();

  return (
    <div className="sidebar">
      <h3>App name</h3>
      <ul className="nav flex-column">
        <ol className="nav-item">
          <button onClick={() => navigate("/home")}>HOME</button>
        </ol>
        <ol className="nav-item">
          <button onClick={() => navigate("/wallet")}>WALLET</button>
        </ol>
        <ol className="nav-item">
          <button onClick={() => navigate("information")}>INFORMATION</button>
        </ol>
      </ul>
    </div>
  );
};
export default NavbarSide;
