import axios from "axios";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const Register = () => {
  const navigate = useNavigate();

  // State cho form: name, email, password
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // Hàm xử lý khi bấm nút "Register"
  const handleRegister = async () => {
    try {
      // Gửi request POST tới API register
      const response = await axios.post(
        "https://hybrid-blockchain-based-stock-tr-test.up.railway.app/users/api/v1/auth/register",
        {
          name,
          email,
          password,
        }
      );

      console.log("Register success:", response.data);
      // Nếu cần, bạn có thể lưu token hoặc gì đó vào localStorage
      // localStorage.setItem("token", response.data.token);

      // Chuyển hướng sang trang login (hoặc trang chủ tuỳ ý)
      navigate("/");
    } catch (error) {
      console.error("Register failed:", error);
      alert("Đăng ký thất bại! Vui lòng kiểm tra lại thông tin hoặc server.");
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

      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />

      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      <button onClick={handleRegister}>Register</button>

      <p>
        Already have an account?{" "}
        <button onClick={() => navigate("/")}>Login</button>
      </p>
    </div>
  );
};

export default Register;
