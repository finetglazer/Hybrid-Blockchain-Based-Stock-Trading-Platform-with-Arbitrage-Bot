import React from "react";
import { useNavigate } from "react-router-dom";

const ForgetPassword = () => {
  const navigate = useNavigate();

  return (
    <div className="container">
      <h2>Forget Password</h2>
      <input type="email" placeholder="Enter your email" />
      <button>Send Reset Link</button>
      <p>
        Remember your password?{" "}
        <button onClick={() => navigate("/")}>Login</button>
      </p>
    </div>
  );
};

export default ForgetPassword;
