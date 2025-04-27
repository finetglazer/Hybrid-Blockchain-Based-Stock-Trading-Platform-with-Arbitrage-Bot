import axios from "axios";
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

const ForgetPassword = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [isError, setIsError] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!email) {
            setMessage("Please enter your email");
            setIsError(true);
            return;
        }

        setLoading(true);
        setMessage("");
        setIsError(false);

        try {

            //full url in the file: src/utils/axios.js
            const response = await axios.post("/users/api/v1/auth/forgot-password", {
                email: email
            });

            // Note: For security reasons, the backend might return the same message
            // regardless of whether the email exists or not
            setMessage("If your email is registered, you will receive a password reset link shortly.");
            setIsError(false);
        } catch (error) {
            setMessage(error.response?.data?.message || "An error occurred. Please try again.");
            setIsError(true);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container">
            <h2>Forgot Password</h2>

            {message && (
                <div className={isError ? "error-message" : "success-message"}>
                    {message}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="email">Email Address</label>
                    <input
                        type="email"
                        id="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="Enter your email"
                        required
                    />
                </div>

                <button type="submit" disabled={loading}>
                    {loading ? "Sending..." : "Send Reset Link"}
                </button>
            </form>

            <p>
                Remember your password?{" "}
                <button onClick={() => navigate("/")}>Back to Login</button>
            </p>
        </div>
    );
};

export default ForgetPassword;