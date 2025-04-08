import React from "react";
import {
  Route,
  BrowserRouter as Router,
  Routes,
  useLocation,
} from "react-router-dom";
import ForgetPassword from "./pages/ForgetPassword";
import HeaderNavbar from "./pages/HeaderNavbar/HeaderNavbar";
import Home from "./pages/Home";
import Login from "./pages/Login";
import NavbarSide from "./pages/NavbarSide/NavbarSide";
import Register from "./pages/Register";
import ResetPassword from "./pages/ResetPassword";
import Setting from "./pages/SettingPage/Setting";
import Support from "./pages/Support";
import Wallet from "./pages/Wallet";

const Layout = () => {
  const location = useLocation();
  const showNavbar = ["/home", "/wallet", "/support", "/setting"].includes(
    location.pathname
  );

  return (
    <>
      {showNavbar && <NavbarSide />}
      {showNavbar && <HeaderNavbar />}
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forget-password" element={<ForgetPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/home" element={<Home />} />
        <Route path="/wallet" element={<Wallet />} />
        <Route path="/support" element={<Support />} />
        <Route path="/setting" element={<Setting />} />
      </Routes>
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
