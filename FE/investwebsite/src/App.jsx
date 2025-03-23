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
import Home from "./pages/Home";
import Wallet from "./pages/Wallet";
import NavbarSide from "./pages/NavbarSide/NavbarSide";
import HeaderNavbar from "./pages/HeaderNavbar/HeaderNavbar";
import Support from "./pages/Support";
import Setting from "./pages/SettingPage/Setting";

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
