import React from "react";
import {
  Route,
  BrowserRouter as Router,
  Routes,
  useLocation,
} from "react-router-dom";
import ForgetPassword from "./pages/ForgetPassword";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Home from "./pages/Home/Home";
import Wallet from "./pages/Wallet";
import NavbarSide from "./pages/NavbarSide/NavbarSide";
import HeaderNavbar from "./pages/HeaderNavbar/HeaderNavbar";
import Support from "./pages/Support";
import Setting from "./pages/SettingPage/Setting";
import TwoFactorAuth from "./pages/TwoFactorAuth/TwoFactorAuth.jsx";
import UpdatePhoneNumber from "./pages/UpdatePhoneNumber/UpdatePhoneNumber.jsx";
import Disable2FA from "./pages/Disable2FA/Disable2FA.jsx";
import TwoFactorAuthenticationSettings from "./pages/SettingPage/2FASettings/2FASettings.jsx";
import { AppContextProvider } from "./AppContextProvider.jsx";
import ChangePassword from "./pages/SettingPage/ChangePassword/ChangePassword.jsx";
import PaymentMethodsManagement from "./pages/PaymentMethod/PaymentMethodsManagement.jsx";

const Layout = () => {
  const location = useLocation();
  const showNavbar = ["/home", "/wallet", "/support", "/setting"].includes(
    location.pathname
  );

  return (
    <>
      {showNavbar && <NavbarSide />}
      {showNavbar && <HeaderNavbar />}
      <AppContextProvider>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forget-password" element={<ForgetPassword />} />
          <Route path="/home" element={<Home />} />
          <Route path="/wallet" element={<Wallet />} />
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
