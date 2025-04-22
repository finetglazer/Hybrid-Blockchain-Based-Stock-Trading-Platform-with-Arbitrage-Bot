import axios from "axios";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const ForgetPassword = () => {
  const [email, setEmail] = useState("");
  const navigate = useNavigate();
  const [message, setMessage] = useState("");

  const handleSubmit = async () => {
    try {
      const res = await axios.post("/users/api/v1/auth/forgot-password", {
        email,
      });
      setMessage("Check your email for the reset link!");
    } catch (error) {
      setMessage("Error sending reset link.");
    }
  };

  return (
    <div className="container">
      <h2>Forget Password</h2>
      <input
        type="email"
        placeholder="Enter your email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <button onClick={handleSubmit}>Send Reset Link</button>
      {message && <p>{message}</p>}
      <p>
        Remember your password?{" "}
        <button onClick={() => navigate("/")}>Login</button>
      </p>
    </div>
  );
};

export default ForgetPassword;
