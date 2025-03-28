import React from "react";
import { Route, BrowserRouter as Router, Routes } from "react-router-dom";
import ForgetPassword from "./pages/ForgetPassword";
import Login from "./pages/Login";
import Register from "./pages/Register";
import TwoFactorAuth from "./pages/TwoFactorAuth/TwoFactorAuth.jsx";

const App = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forget-password" element={<ForgetPassword />} />
          <Route path="/forget-password" element={<ForgetPassword />} />
          <Route path="/two-factor-auth" element={<TwoFactorAuth />} /> {/* Add this route */}
      </Routes>
    </Router>
  );
};
export default App;
