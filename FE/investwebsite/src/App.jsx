import React from "react";
import { Route, BrowserRouter as Router, Routes } from "react-router-dom";
import ForgetPassword from "./pages/ForgetPassword";
import Login from "./pages/Login";
import Register from "./pages/Register";
import TwoFactorAuth from "./pages/TwoFactorAuth/TwoFactorAuth.jsx";
import UpdatePhoneNumber from "./pages/UpdatePhoneNumber/UpdatePhoneNumber.jsx";
import Disable2FA from "./pages/Disable2FA/Disable2FA.jsx";

const App = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forget-password" element={<ForgetPassword />} />
        <Route path="/forget-password" element={<ForgetPassword />} />
        <Route path="/two-factor-auth" element={<TwoFactorAuth orAuth />} /> {/* Add this route */}
        <Route path="/profile/update-phone" element={<UpdatePhoneNumber />} />
        <Route path="/profile/disable2FA" element={<Disable2FA />} />
      </Routes>
    </Router>
  );
};
export default App;
