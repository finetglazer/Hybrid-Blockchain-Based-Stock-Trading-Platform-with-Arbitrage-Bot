import React from "react";
import { useNavigate } from "react-router-dom";
const Register = () => {
  const navigate = useNavigate();

  return (
    <div className="container">
      <h2>Register Page</h2>
      <input type="text" placeholder="Name" />
      <input type="email" placeholder="Email" />
      <input type="password" placeholder="Password" />
      <button>Register</button>
      <p>
        Already have an account?{" "}
        <button onClick={() => navigate("/")}>Login</button>
      </p>
    </div>
  );
};

export default Register;
