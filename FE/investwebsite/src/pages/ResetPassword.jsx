import axios from "axios";
import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

const ResetPassword = () => {
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const navigate = useNavigate();
  const location = useLocation();

  // Lấy token từ URL
  const queryParams = new URLSearchParams(location.search);
  const token = queryParams.get("token");

  const handleReset = async () => {
    if (password !== confirmPassword) {
      setMessage("Passwords do not match!");
      return;
    }

    try {
      await axios.post("/api/v1/auth/reset-password", {
        token,
        password,
      });
      setMessage("Password has been reset!");
      setTimeout(() => navigate("/"), 2000); // quay lại login
    } catch (error) {
      setMessage("Reset failed. Token might be invalid or expired.");
    }
  };

  return (
    <div className="container">
      <h2>Reset Password</h2>
      <input
        type="password"
        placeholder="New password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <input
        type="password"
        placeholder="Re-enter password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
      />
      <button onClick={handleReset}>Send</button>
      {message && <p>{message}</p>}
      <p>
        Remember your password?{" "}
        <button onClick={() => navigate("/")}>Login</button>
      </p>
    </div>
  );
};

export default ResetPassword;
