// FE/investwebsite/src/pages/UpdatePhoneNumber/UpdatePhoneNumber.jsx
import React, { useState, useEffect } from "react";
import { RecaptchaVerifier, signInWithPhoneNumber } from "firebase/auth";
import { auth } from "../../firebase/firebaseConfig";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./UpdatePhoneNumber.css"; // Create a matching CSS file based on Enable2FA.css

const UpdatePhoneNumber = () => {
    const navigate = useNavigate();
    const [phoneNumber, setPhoneNumber] = useState("");
    const [verificationId, setVerificationId] = useState("");
    const [verificationCode, setVerificationCode] = useState("");
    const [step, setStep] = useState("phone"); // "phone", "code", "success"
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [countdown, setCountdown] = useState(0);
    const [recaptchaReady, setRecaptchaReady] = useState(false);
    const [updatedInfo, setUpdatedInfo] = useState(null);

    // Initialize recaptcha when component mounts
    useEffect(() => {
        // Cleanup any existing recaptcha on component unmount
        return () => {
            if (window.recaptchaVerifier) {
                try {
                    window.recaptchaVerifier.clear();
                    window.recaptchaVerifier = null;
                } catch (error) {
                    console.error("Error clearing recaptcha on unmount:", error);
                }
            }
        };
    }, []);

    // Initialize recaptcha only when in phone step
    useEffect(() => {
        if (step === "phone" && !recaptchaReady) {
            // Delay initialization slightly to ensure DOM is ready
            const timer = setTimeout(() => {
                initializeRecaptcha();
            }, 1000);

            return () => clearTimeout(timer);
        }
    }, [step, recaptchaReady]);

    // Countdown timer for code expiration
    useEffect(() => {
        let timer;
        if (countdown > 0 && step === "code") {
            timer = setInterval(() => {
                setCountdown(prev => prev - 1);
            }, 1000);
        } else if (countdown === 0 && step === "code") {
            setError("Verification code expired. Please request a new code.");
        }

        return () => {
            if (timer) clearInterval(timer);
        };
    }, [countdown, step]);

    const initializeRecaptcha = () => {
        // Only initialize if not already initialized
        if (window.recaptchaVerifier) {
            setRecaptchaReady(true);
            return;
        }

        try {
            const container = document.getElementById('recaptcha-container');
            if (!container) {
                console.error("Recaptcha container not found");
                return;
            }

            window.recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', {
                size: 'normal',
                callback: () => {
                    console.log("reCAPTCHA verified");
                    setRecaptchaReady(true);
                },
                'expired-callback': () => {
                    setError("reCAPTCHA expired. Please refresh and try again.");
                    setRecaptchaReady(false);
                }
            });

            // Render the recaptcha
            window.recaptchaVerifier.render()
                .then(widgetId => {
                    window.recaptchaWidgetId = widgetId;
                    setRecaptchaReady(true);
                })
                .catch(error => {
                    console.error("Error rendering recaptcha:", error);
                    setError("Failed to initialize reCAPTCHA. Please refresh the page.");
                });
        } catch (error) {
            console.error("Error initializing recaptcha:", error);
            setError("Failed to initialize reCAPTCHA. Please refresh the page.");
        }
    };

    const formatPhoneNumber = (input) => {
        // Remove all non-digit characters
        const digits = input.replace(/\D/g, '');

        // Handle Vietnamese numbers specifically
        if (digits.startsWith('84')) {
            return `+${digits}`;
        } else if (digits.startsWith('0')) {
            return `+84${digits.substring(1)}`;
        } else {
            return `+84${digits}`; // Assume it's a local number without country code
        }
    };

    const handleStartVerification = async () => {
        setLoading(true);
        setError("");

        try {
            const formattedPhone = formatPhoneNumber(phoneNumber);
            console.log("Attempting verification for:", formattedPhone);

            // Make sure reCAPTCHA is initialized and ready
            if (!recaptchaReady || !window.recaptchaVerifier) {
                setError("Please complete the reCAPTCHA verification first.");
                setLoading(false);
                return;
            }

            // First, make a backend request to register the phone update intent
            const token = localStorage.getItem("token");
            if (!token) {
                throw new Error("Not authenticated. Please log in again.");
            }

            // Call the PUT endpoint to initiate phone number update
            const response = await axios.put("http://localhost:8081/users/api/v1/me/phone-number", {
                phoneNumber: formattedPhone
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            });

            if (response.data && response.data.status === 1) {
                setVerificationId(response.data.data.verificationId);

                // Get the expiration time from the server response
                const expiresAt = new Date(response.data.data.expiresAt).getTime();
                const now = new Date().getTime();
                const timeLeftInSeconds = Math.floor((expiresAt - now) / 1000);

                // Set countdown timer (subtracting 10 seconds as buffer)
                setCountdown(Math.max(timeLeftInSeconds - 10, 0));

                // After backend confirms, send Firebase verification
                const appVerifier = window.recaptchaVerifier;

                // Use a try-catch block specifically for the Firebase call
                try {
                    const confirmationResult = await signInWithPhoneNumber(
                        auth,
                        formattedPhone,
                        appVerifier
                    );

                    window.confirmationResult = confirmationResult;
                    console.log("SMS sent successfully");

                    setStep("code");
                } catch (firebaseError) {
                    if (firebaseError.code === 'auth/too-many-requests') {
                        // Handle rate limiting specifically
                        setError("Too many verification attempts. Please wait a while (usually around 1 hour) before trying again, or try with a different phone number.");
                    } else {
                        throw firebaseError; // Let the outer catch handle other errors
                    }
                }
            } else {
                setError(response.data?.msg || "Failed to start verification");
            }
        } catch (err) {
            console.error("Verification error:", err);

            // Handle specific Firebase Auth errors
            if (err.code === 'auth/invalid-app-credential') {
                setError("Firebase authentication is misconfigured. Please contact support.");
            } else if (err.code === 'auth/captcha-check-failed') {
                setError("reCAPTCHA verification failed. Please try again.");
                // Reset reCAPTCHA on failure
                setRecaptchaReady(false);
                initializeRecaptcha();
            } else if (err.code === 'auth/invalid-phone-number') {
                setError("Invalid phone number format. Please use the international format (+84...).");
            } else if (err.code === 'auth/quota-exceeded') {
                setError("SMS quota exceeded. Please try again later.");
            } else if (err.code === 'auth/too-many-requests') {
                setError("Too many verification attempts. Please wait a while before trying again or use a different phone number.");
            } else {
                setError(`Error: ${err.message || "Unknown error"}`);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleVerifyCode = async () => {
        setLoading(true);
        setError("");

        try {
            if (!window.confirmationResult) {
                throw new Error("No verification in progress. Please restart the process.");
            }

            // Verify the code with Firebase
            const result = await window.confirmationResult.confirm(verificationCode);
            const user = result.user;

            // Get Firebase ID token
            const idToken = await user.getIdToken();

            // Send verification to backend
            const token = localStorage.getItem("token");

            // Call the POST endpoint to verify the phone number update
            const response = await axios.post("/users/api/v1/me/phone-number/verify", {
                verificationId: verificationId,
                code: verificationCode,
                firebaseIdToken: idToken // Include the Firebase ID token
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });

            if (response.data && response.data.status === 1) {
                setUpdatedInfo(response.data.data);
                setStep("success");

                // Clean up Firebase user created during verification
                try {
                    await user.delete();
                } catch (deleteError) {
                    console.error("Error cleaning up Firebase user:", deleteError);
                    // This error doesn't affect the user experience, so we don't show it
                }
            } else {
                setError(response.data?.msg || "Failed to verify code");
            }
        } catch (err) {
            console.error("Error verifying code:", err);

            if (err.code === 'auth/code-expired') {
                setError("Verification code has expired. Please request a new code.");
                setCountdown(0);
            } else if (err.code === 'auth/invalid-verification-code') {
                setError("Invalid verification code. Please check and try again.");
            } else {
                setError(`Verification failed: ${err.message || "Code is invalid. Please try again."}`);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleResendCode = () => {
        // Reset verification state and go back to phone step
        setStep("phone");
        setVerificationCode("");
        setError("");
        setRecaptchaReady(false);

        // Clear the existing reCAPTCHA
        if (window.recaptchaVerifier) {
            try {
                window.recaptchaVerifier.clear();
                window.recaptchaVerifier = null;
            } catch (error) {
                console.error("Error clearing recaptcha:", error);
            }
        }
    };

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    };

    return (
        <div className="container">
            <h2>Update Phone Number</h2>

            {error && <div className="error-text">{error}</div>}

            {step === "phone" && (
                <div>
                    <p>Enter your new phone number:</p>
                    <input
                        type="tel"
                        placeholder="Phone Number (e.g., 0365706735)"
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                    />

                    <div id="recaptcha-container" className="recaptcha-container"></div>

                    <button
                        className={loading ? "button loading" : "button"}
                        onClick={handleStartVerification}
                        disabled={loading || !phoneNumber || !recaptchaReady}
                    >
                        {loading ? "Sending..." : "Send Verification Code"}
                    </button>
                </div>
            )}

            {step === "code" && (
                <div>
                    <p>Enter the verification code sent to your new phone number:</p>
                    {countdown > 0 && (
                        <p className="countdown">Code expires in: {formatTime(countdown)}</p>
                    )}
                    <input
                        type="text"
                        placeholder="Verification Code"
                        value={verificationCode}
                        onChange={(e) => setVerificationCode(e.target.value)}
                    />
                    <div className="button-group">
                        <button
                            className={loading ? "button loading" : "button"}
                            onClick={handleVerifyCode}
                            disabled={loading || !verificationCode || countdown === 0}
                        >
                            {loading ? "Verifying..." : "Verify Code"}
                        </button>
                        {countdown === 0 && (
                            <button
                                className="button secondary"
                                onClick={handleResendCode}
                                disabled={loading}
                            >
                                Resend Code
                            </button>
                        )}
                    </div>
                </div>
            )}

            {step === "success" && (
                <div>
                    <h3>Phone Number Updated Successfully!</h3>
                    {updatedInfo && (
                        <div>
                            <p>Your phone number has been updated to: <strong>{updatedInfo.phoneNumber}</strong></p>
                            <p>Updated at: {new Date(updatedInfo.updatedAt).toLocaleString()}</p>
                        </div>
                    )}
                    <button
                        className="button"
                        onClick={() => navigate("/profile")}
                    >
                        Return to Profile
                    </button>
                </div>
            )}
        </div>
    );
};

export default UpdatePhoneNumber;
