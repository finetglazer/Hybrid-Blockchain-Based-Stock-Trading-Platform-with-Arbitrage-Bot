import React from "react";
import { useNavigate } from "react-router-dom";

const Login = () => {
  const navigate = useNavigate();

  return (
    <div className="container">
      <h2>Login Page</h2>
      <input type="text" placeholder="Email" />
      <input type="password" placeholder="Password" />
      <button>Login</button>
      <p>
        Don't have an account?{" "}
        <button onClick={() => navigate("/register")}>Register</button>
      </p>
      <p>
        Forgot password?{" "}
        <button onClick={() => navigate("/forget-password")}>
          Reset Password
        </button>
      </p>
    </div>
  );
};

export default Login;
