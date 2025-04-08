import React from "react";
import { useNavigate } from "react-router-dom";

const ResetPassword = () => {
  const navigate = useNavigate();

  return (
    <div className="container">
      <h2>Reset Password</h2>
      <input type="password" placeholder="New password" />
      <input type="password" placeholder="Retype new password" />

      <button>Reset Password</button>
    </div>
  );
};

export default ResetPassword;
