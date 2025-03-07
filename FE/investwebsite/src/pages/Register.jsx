import axios from "axios";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const Register = () => {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState({});

  const validate = () => {
    let newError = {};
    if (!name.trim()) newError.name = "Name is required";
    if (!email.includes("@gmail.com")) newError.email = "Email is Invalid";
    if (password.length < 6) newError.password = "At least 6 characters";
    setErrors(newError);
    return Object.keys(newError).length === 0;
  };
  const handleRegister = async () => {
    if (!validate()) return;
    try {
      const response = await axios.post(
        "/api/users/api/v1/auth/register", // Đã đổi URL dùng proxy
        { name, email, password }
      );

      console.log("Register success:", response.data);
      navigate("/");
    } catch (error) {
      console.error("Register failed:", error);
      alert("Đăng ký thất bại! Vui lòng kiểm tra lại.");
    }
  };

  return (
    <div className="container">
      <h2>Register Page</h2>
      <input
        type="text"
        placeholder="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      {errors.name && <p style={{ color: "red" }}>{errors.name}</p>}

      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      {errors.email && <p style={{ color: "red" }}>{errors.email}</p>}
      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      {errors.password && <p style={{ color: "red" }}>{errors.password}</p>}
      <button onClick={handleRegister}>Register</button>
      <p>
        Already have an account?{" "}
        <button onClick={() => navigate("/")}>Login</button>
      </p>
    </div>
  );
};

export default Register;
