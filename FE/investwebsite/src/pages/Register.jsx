import axios from "axios";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
  });
  const [errors, setErrors] = useState({});

  const validate = () => {
    let newErrors = {};

    if (!formData.username.trim()) newErrors.username = "⚠ Name is required!";
    if (!formData.email.includes("@gmail.com"))
      newErrors.email = "⚠ Invalid Email!";
    if (formData.password.length < 6)
      newErrors.password = "⚠ Password must be at least 6 characters!";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); // Ngăn form gửi đi nếu có lỗi
    if (validate()) {
      console.log("Register user:", formData);
      navigate("/");
    }
    try {
      const response = await axios.post(
        "/api/users/api/v1/auth/register", // Đã đổi URL dùng proxy
        formData
      );

      console.log("Register success:", response.data);
      navigate("/");
    } catch (error) {
      console.error("Register failed:", error);
      alert("Đăng ký thất bại! Vui lòng kiểm tra lại.");
    }
  };

  return (
    <div className="register-container container">
      <h2>Register Page</h2>
      <form onSubmit={handleSubmit}>
        <div className="register-input">
          <input
            type="text"
            placeholder="Username"
            value={formData.username}
            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
          />
          {errors.username && <p className="error-text">{errors.username}</p>}
        </div>

        <div className="register-input">
          <input
            type="email"
            placeholder="Email"
            value={formData.email}
            onChange={(e) =>
              setFormData({ ...formData, email: e.target.value })
            }
          />
          {errors.email && <p className="error-text">{errors.email}</p>}
        </div>

        <div className="register-input">
          <input
            type="password"
            placeholder="Password"
            value={formData.password}
            onChange={(e) =>
              setFormData({ ...formData, password: e.target.value })
            }
          />
          {errors.password && <p className="error-text">{errors.password}</p>}
        </div>

        <button type="submit" className="submit-btn">
          Register
        </button>
      </form>

      <p>
        Already have an account?{" "}
        <button onClick={() => navigate("/")}>Login</button>
      </p>
    </div>
  );
};

export default Register;
