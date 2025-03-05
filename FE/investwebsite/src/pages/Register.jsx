import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
  });
  const [errors, setErrors] = useState({});

  const validate = () => {
    let newErrors = {};

    if (!formData.name.trim()) newErrors.name = "⚠ Name is required!";
    if (!formData.email.includes("@gmail.com"))
      newErrors.email = "⚠ Invalid Email!";
    if (formData.password.length < 6)
      newErrors.password = "⚠ Password must be at least 6 characters!";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault(); // Ngăn form gửi đi nếu có lỗi
    if (validate()) {
      console.log("Register user:", formData);
      navigate("/");
    }
  };

  return (
    <div className="register-container container">
      <h2>Register Page</h2>
      <form onSubmit={handleSubmit}>
        <div className="register-input">
          <input
            type="text"
            placeholder="Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          />
          {errors.name && <p className="error-text">{errors.name}</p>}
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
