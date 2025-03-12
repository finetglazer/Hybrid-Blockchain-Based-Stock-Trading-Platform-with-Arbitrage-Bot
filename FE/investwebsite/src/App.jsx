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
import Information from "./pages/Information";
import Wallet from "./pages/Wallet";
import NavbarSide from "./pages/NavbarSide/NavbarSide";

const Layout = () => {
  const location = useLocation();
  const showNavbar = ["/home", "/wallet", "/information"].includes(
    location.pathname
  );

  return (
    <>
      {showNavbar && <NavbarSide />}
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forget-password" element={<ForgetPassword />} />
        <Route path="/home" element={<Home />} />
        <Route path="/wallet" element={<Wallet />} />
        <Route path="/information" element={<Information />} />
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
