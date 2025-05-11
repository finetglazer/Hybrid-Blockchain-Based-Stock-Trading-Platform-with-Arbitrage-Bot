import axios from "axios"; // Import axios
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const Login = () => {
  // Dùng useNavigate để chuyển trang sau khi đăng nhập
  const navigate = useNavigate();

  // State cho email & password
  const [usernameOrEmail, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // Hàm xử lý đăng nhập
  const handleLogin = async () => {
    try {
      const response = await axios.post("api/users/api/v1/auth/login", {
        usernameOrEmail,
        password,
      });

      const token = response.data.data; // Đảm bảo đây là chuỗi token thực sự
      if (token) {
        localStorage.setItem("token", token);
        console.log("Login success:", response.data.data);
      }
      // Chuyển hướng sau khi đăng nhập thành công
      if (response.data.status === 1) {
        localStorage.setItem("username", usernameOrEmail);
        console.log(usernameOrEmail);
        navigate("/home");
        return; // Dừng hàm, không điều hướng
      }
    } catch (err) {
      if (err.response) {
        // Lỗi từ phía server có response (server phản hồi)
        if (err.response.status === 400 || err.response.status === 401) {
          alert("Sai email hoặc mật khẩu. Vui lòng thử lại!");
        } else if (err.response.status >= 500) {
          alert("Lỗi server! Vui lòng thử lại sau.");
        } else {
          alert(`Lỗi không xác định: ${err.response.status}`);
        }
      } else if (err.request) {
        // Không nhận được phản hồi từ server (server có thể đang down)
        alert("Không thể kết nối đến server. Vui lòng kiểm tra mạng!");
      } else {
        // Lỗi khác (có thể do lỗi code)
        alert("Đã xảy ra lỗi! Vui lòng thử lại.");
      }
      console.error("Login failed:", err);
    }
  };

  return (
    <div className="container">
      <h2>Login Page</h2>
      <input
        type="text"
        placeholder="Email"
        value={usernameOrEmail}
        onChange={(e) => setEmail(e.target.value)}
      />
      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      <button onClick={handleLogin}>Login</button>

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
