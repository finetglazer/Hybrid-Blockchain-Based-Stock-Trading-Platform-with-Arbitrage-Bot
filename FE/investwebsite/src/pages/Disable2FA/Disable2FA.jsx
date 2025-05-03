// FE/investwebsite/src/pages/Disable2FA/Disable2FA.jsx
import React, { useState, useEffect } from "react";
import { RecaptchaVerifier, signInWithPhoneNumber } from "firebase/auth";
import { auth } from "../../firebase/firebaseConfig";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./Disable2FA.css"; // Create a matching CSS file

const Disable2FA = () => {
    const navigate = useNavigate();
    const [phoneNumber, setPhoneNumber] = useState(""); // Full phone number for Firebase
    const [maskedPhoneNumber, setMaskedPhoneNumber] = useState(""); // Masked phone number for display
    const [verificationId, setVerificationId] = useState("");
    const [verificationCode, setVerificationCode] = useState("");
    const [step, setStep] = useState("initialize"); // "initialize", "code", "success"
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [countdown, setCountdown] = useState(0);
    const [recaptchaReady, setRecaptchaReady] = useState(false);
    const [usingRecoveryKey, setUsingRecoveryKey] = useState(false);
    const [recoveryKey, setRecoveryKey] = useState("");

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

    // Initialize recaptcha only when in initialize step and not using recovery key
    useEffect(() => {
        if (step === "initialize" && !usingRecoveryKey && !recaptchaReady) {
            // Delay initialization slightly to ensure DOM is ready
            const timer = setTimeout(() => {
                initializeRecaptcha();
            }, 1000);

            return () => clearTimeout(timer);
        }
    }, [step, recaptchaReady, usingRecoveryKey]);

    // Countdown timer for code expiration
    useEffect(() => {
        let timer;
        if (countdown > 0 && step === "code") {
            timer = setInterval(() => {
                setCountdown(prev => prev - 1);
            }, 1000);
        } else if (countdown === 0 && step === "code") {
            setError("Verification code expired. Please restart the process.");
        }

        return () => {
            if (timer) clearInterval(timer);
        };
    }, [countdown, step]);

    // Initial load - fetch user's 2FA info
    useEffect(() => {
        if (step === "initialize") {
            fetchTwoFactorInfo();
        }
    }, []);

    // Phone number formatter for Firebase (taken from Enable2FA.jsx)
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

    const fetchTwoFactorInfo = async () => {
        setLoading(true);
        setError("");

        try {
            const token = localStorage.getItem("token");
            if (!token) {
                throw new Error("Not authenticated. Please log in again.");
            }

            // Call the controller endpoint to get 2FA info
            const response = await axios.get("/users/api/v1/auth/2fa/info", {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });

            if (response.data && response.data.status === 1) {
                // Store both phone number versions
                setPhoneNumber(response.data.data.phoneNumber); // Full phone for Firebase
                setMaskedPhoneNumber(response.data.data.maskedPhoneNumber || response.data.data.phoneNumber); // Masked for display
                setVerificationId(response.data.data.verificationId);

                // Get the expiration time from the server response
                const expiresAt = new Date(response.data.data.expiresAt).getTime();
                const now = new Date().getTime();
                const timeLeftInSeconds = Math.floor((expiresAt - now) / 1000);

                // Set countdown timer (subtracting 10 seconds as buffer)
                setCountdown(Math.max(timeLeftInSeconds - 10, 0));
            } else {
                setError(response.data?.msg || "Failed to fetch 2FA information");
            }
        } catch (err) {
            console.error("Error fetching 2FA info:", err);
            setError(`Error: ${err.message || "Failed to load 2FA information"}`);
        } finally {
            setLoading(false);
        }
    };

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

    const handleStartVerification = async () => {
        setLoading(true);
        setError("");

        try {
            // Make sure reCAPTCHA is initialized and ready
            if (!recaptchaReady || !window.recaptchaVerifier) {
                setError("Please complete the reCAPTCHA verification first.");
                setLoading(false);
                return;
            }

            // Format the phone number properly for Firebase
            const formattedPhone = formatPhoneNumber(phoneNumber);
            console.log("Attempting verification for:", formattedPhone);

            // Use Firebase to send verification code
            const appVerifier = window.recaptchaVerifier;

            try {
                const confirmationResult = await signInWithPhoneNumber(
                    auth,
                    formattedPhone, // Use properly formatted phone number
                    appVerifier
                );

                window.confirmationResult = confirmationResult;
                console.log("SMS sent successfully");

                setStep("code");
            } catch (firebaseError) {
                if (firebaseError.code === 'auth/too-many-requests') {
                    setError("Too many verification attempts. Please wait a while before trying again.");
                } else {
                    throw firebaseError;
                }
            }
        } catch (err) {
            console.error("Verification error:", err);

            // Handle specific Firebase Auth errors
            if (err.code === 'auth/invalid-app-credential') {
                setError("Firebase authentication is misconfigured. Please contact support.");
            } else if (err.code === 'auth/captcha-check-failed') {
                setError("reCAPTCHA verification failed. Please try again.");
                setRecaptchaReady(false);
                initializeRecaptcha();
            } else if (err.code === 'auth/invalid-phone-number') {
                setError("Invalid phone number format. Please contact support.");
            } else if (err.code === 'auth/quota-exceeded') {
                setError("SMS quota exceeded. Please try again later.");
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

            // Send verification to backend to disable 2FA
            const token = localStorage.getItem("token");

            // Since there's no specific disable endpoint in the controller, we'll add one
            // You may need to add this endpoint to your controller
            const response = await axios.post("/users/api/v1/auth/2fa/disable", {
                verificationId: verificationId,
                firebaseIdToken: idToken
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });

            if (response.data && response.data.status === 1) {
                setStep("success");

                // Clean up Firebase user created during verification
                try {
                    await user.delete();
                } catch (deleteError) {
                    console.error("Error cleaning up Firebase user:", deleteError);
                    // This error doesn't affect the user experience, so we don't show it
                }
            } else {
                setError(response.data?.msg || "Failed to disable 2FA");
            }
        } catch (err) {
            console.error("Error verifying code:", err);

            if (err.code === 'auth/code-expired') {
                setError("Verification code has expired. Please restart the process.");
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

    const handleVerifyWithRecoveryKey = async () => {
        setLoading(true);
        setError("");

        try {
            const token = localStorage.getItem("token");
            if (!token) {
                throw new Error("Not authenticated. Please log in again.");
            }

            // Call the recovery key verification endpoint from the controller
            const response = await axios.post("/users/api/v1/auth/2fa/recovery-keys/verify", {
                recoveryKey: recoveryKey
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });

            if (response.data && response.data.status === 1) {
                // After verification with recovery key, add a custom endpoint for disabling 2FA
                // Since there's no specific disable endpoint in the controller, we'll add one
                // You may need to add this endpoint to your controller
                const disableResponse = await axios.post("/users/api/v1/auth/2fa/disable", {
                    verificationMethod: "RECOVERY_KEY",
                    recoveryKeyVerificationId: response.data.data.verificationId
                }, {
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                });

                if (disableResponse.data && disableResponse.data.status === 1) {
                    setStep("success");
                } else {
                    setError(disableResponse.data?.msg || "Failed to disable 2FA");
                }
            } else {
                setError(response.data?.msg || "Invalid recovery key");
            }
        } catch (err) {
            console.error("Error verifying recovery key:", err);
            setError(`Error: ${err.message || "Failed to verify recovery key"}`);
        } finally {
            setLoading(false);
        }
    };

    const toggleVerificationMethod = () => {
        setUsingRecoveryKey(!usingRecoveryKey);
        setError("");

        // Clear recaptcha if switching to recovery key
        if (!usingRecoveryKey) {
            if (window.recaptchaVerifier) {
                try {
                    window.recaptchaVerifier.clear();
                    window.recaptchaVerifier = null;
                } catch (error) {
                    console.error("Error clearing recaptcha:", error);
                }
            }
            setRecaptchaReady(false);
        } else {
            // Initialize recaptcha if switching back to SMS
            initializeRecaptcha();
        }
    };

    const formatTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    };

    return (
        <div className="container">
            <h2>Disable Two-Factor Authentication</h2>

            {error && <div className="error-text">{error}</div>}

            {step === "initialize" && (
                <div>
                    <p>To disable two-factor authentication, we need to verify your identity.</p>

                    {usingRecoveryKey ? (
                        <div>
                            <p>Enter one of your recovery keys:</p>
                            <input
                                type="text"
                                placeholder="Recovery Key (e.g., XXXX-XXXX-XXXX-XXXX)"
                                value={recoveryKey}
                                onChange={(e) => setRecoveryKey(e.target.value)}
                            />
                            <button
                                className={loading ? "button loading" : "button"}
                                onClick={handleVerifyWithRecoveryKey}
                                disabled={loading || !recoveryKey}
                            >
                                {loading ? "Verifying..." : "Verify Recovery Key"}
                            </button>
                        </div>
                    ) : (
                        <div>
                            <p>We will send a verification code to your registered phone number:</p>
                            <p className="masked-phone">{maskedPhoneNumber}</p>

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

                    <div className="toggle-method">
                        <button
                            className="button secondary"
                            onClick={toggleVerificationMethod}
                            disabled={loading}
                        >
                            {usingRecoveryKey ? "Use SMS Verification Instead" : "Use Recovery Key Instead"}
                        </button>
                    </div>
                </div>
            )}

            {step === "code" && (
                <div>
                    <p>Enter the verification code sent to your phone:</p>
                    {countdown > 0 && (
                        <p className="countdown">Code expires in: {formatTime(countdown)}</p>
                    )}
                    <input
                        type="text"
                        placeholder="Verification Code"
                        value={verificationCode}
                        onChange={(e) => setVerificationCode(e.target.value)}
                    />
                    <button
                        className={loading ? "button loading" : "button"}
                        onClick={handleVerifyCode}
                        disabled={loading || !verificationCode || countdown === 0}
                    >
                        {loading ? "Verifying..." : "Verify and Disable 2FA"}
                    </button>

                    {countdown === 0 && (
                        <button
                            className="button secondary"
                            onClick={() => setStep("initialize")}
                            disabled={loading}
                        >
                            Restart Process
                        </button>
                    )}
                </div>
            )}

            {step === "success" && (
                <div>
                    <h3>Two-Factor Authentication Disabled</h3>
                    <p>Your account no longer requires two-factor authentication during login.</p>
                    <p>If you want to enable 2FA again in the future, you can do so from your account settings.</p>
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

export default Disable2FA;
