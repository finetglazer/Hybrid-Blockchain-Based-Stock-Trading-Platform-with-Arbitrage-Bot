import axios from "axios"; // Import axios
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const Login = () => {
  // Dùng useNavigate để chuyển trang sau khi đăng nhập
  const navigate = useNavigate();

  // State cho email & password
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // Hàm xử lý đăng nhập
  const handleLogin = async () => {
    try {
      // Gửi yêu cầu POST tới API kèm email, password
      const response = await axios.post("https://hybrid-blockchain-based-stock-tr-test.up.railway.app/users/api/v1/auth/login", {
        email,
        password,
      });

      console.log("Login success:", response.data);
      
      // Ví dụ: nếu server trả về token, bạn có thể lưu vào localStorage
      // localStorage.setItem("token", response.data.token);

      // Sau khi đăng nhập thành công, chuyển hướng sang trang chủ
      navigate("/");
    } catch (err) {
      // Nếu đăng nhập thất bại, log lỗi và hiện thông báo cho người dùng
      console.error("Login failed:", err);
      alert("Login thất bại! Vui lòng kiểm tra email/password hoặc server.");
    }
  };

  return (
    <div className="container">
      <h2>Login Page</h2>
      <input
        type="text"
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
