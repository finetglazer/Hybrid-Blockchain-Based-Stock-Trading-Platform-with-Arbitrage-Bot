import React from "react";
import {
  Route,
  BrowserRouter as Router,
  Routes,
  useLocation,
} from "react-router-dom";
import { AppContextProvider } from "./AppContextProvider.jsx";
import Deposit from "./pages/Deposit.jsx";
import Disable2FA from "./pages/Disable2FA/Disable2FA.jsx";
import ForgetPassword from "./pages/ForgetPassword";
import ResetPassword from "./pages/ResetPassword/ResetPassword.jsx";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import Home from "./pages/Home/Home.jsx";
import NavbarSide from "./pages/NavbarSide/NavbarSide.jsx";
import PaymentMethodsManagement from "./pages/PaymentMethod/PaymentMethodsManagement.jsx";
import TwoFactorAuthenticationSettings from "./pages/SettingPage/2FASettings/2FASettings.jsx";
import ChangePassword from "./pages/SettingPage/ChangePassword/ChangePassword.jsx";
import Setting from "./pages/SettingPage/Setting.jsx";
import Support from "./pages/Support.jsx";
import TwoFactorAuth from "./pages/TwoFactorAuth/TwoFactorAuth.jsx";
import UpdatePhoneNumber from "./pages/UpdatePhoneNumber/UpdatePhoneNumber.jsx";
import Withdraw from "./pages/Withdraw.jsx";
import Wallet from "./pages/Wallet/Wallet.jsx";
import HeaderBar from "./pages/HeaderNavbar/HeaderNavbar.jsx";

const Layout = () => {
  const location = useLocation();
  const showNavbar = [
    "/home",
    "/trading-accounts",
    "/support",
    "/setting",
  ].includes(location.pathname);

  return (
    <>
      {showNavbar && <NavbarSide />}
      {showNavbar && <HeaderBar />}
      <AppContextProvider>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forget-password" element={<ForgetPassword />} />
          <Route path="/home" element={<Home />} />
          <Route path="/withdraw" element={<Withdraw />} />
          <Route path="/deposit" element={<Deposit />} />
          <Route path="/trading-accounts" element={<Wallet />} />
          <Route path="/support" element={<Support />} />
          <Route path="/setting" element={<Setting />} />
          <Route
            path="/setting/2fa-settings"
            element={<TwoFactorAuthenticationSettings />}
          />
          <Route path="/setting/change-password" element={<ChangePassword />} />
          <Route
            path="/account-dashboard/payment-methods"
            element={<PaymentMethodsManagement />}
          />
          <Route path="/forget-password" element={<ForgetPassword />} />
          <Route path="/two-factor-auth" element={<TwoFactorAuth />} />{" "}
          {/* Add this route */}
          <Route path="/profile/update-phone" element={<UpdatePhoneNumber />} />
          <Route path="/profile/disable2FA" element={<Disable2FA />} />
          <Route path="/reset-password" element={<ResetPassword />} />
        </Routes>
      </AppContextProvider>
    </>
  );
};

const App = () => {
  return (
    <Router>
      <Layout />
    </Router>
  );
};

export default App;
