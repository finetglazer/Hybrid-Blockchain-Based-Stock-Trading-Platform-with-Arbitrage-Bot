import {useEffect, useRef, useState} from "react";
import axios from "axios";
import {useAppContext} from "../../AppContextProvider.jsx";
import {signInWithPhoneNumber} from "firebase/auth";
import {auth} from "../../firebase/firebaseConfig.js";
import {Alert} from "antd";
import {useNavigate} from "react-router-dom";

const TwoFaVerification = () => {
    const navigate = useNavigate();
    const {setTwoFaVerified} = useAppContext();

    const [timeLeft, setTimeLeft] = useState(600); // secs
    const inputsRef = useRef(null);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const {callbackUrl, setCallbackUrl} = useAppContext();

    const convertTimeLeft = () => {
        const m = Math.floor(timeLeft / 60);
        const s = timeLeft - m * 60;
        return (m < 10 ? "0" : "") + m.toString() + ":" + (s < 10 ? "0" : "") + s.toString();
    };

    const handleChange = (e, index) => {
        const value = e.target.value;

        if (value && index < 5) {
            inputsRef.current[index + 1].focus();
        }
    };

    const handleKeyDown = (e, index) => {
        if (e.key === "Backspace" && !e.target.value && index > 0) {
            inputsRef.current[index - 1].focus();
        }
    };

    const handleVerifyCode = async () => {
        if (timeLeft <= 0) {
            setError("Your verification code has expired");
            return;
        }
        setLoading(true);
        setError("");
        const code = inputsRef.current.map((input) => input.value).join("");
        console.log("Code entered:", code);

        try {
            if (!window.confirmationResult) {
                setError("No verification in progress. Please restart the process.");
                if (callbackUrl) {  //  to page called back after 2FA process
                    navigate(callbackUrl);
                    setTwoFaVerified(true);
                    setCallbackUrl("");
                }
            }

            // Verify the code with Firebase
            const result = await window.confirmationResult.confirm(code);
            const user = result.user;

            // Get Firebase ID token
            const idToken = await user.getIdToken();

            // Send verification to backend
            const token = localStorage.getItem("token");
            const create2FaRes = await axios.get("users/api/v1/auth/2fa/create", {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });

            let verificationId = "";
            if (create2FaRes.data && create2FaRes.data.status === 1) {
                verificationId = create2FaRes.data.data.verificationId;
            }
            else {
                setError(create2FaRes.data.msg);
                return;
            }

            const response = await axios.post("/users/api/v1/auth/2fa/verify", {
                verificationId: verificationId,
                code: code,
                firebaseIdToken: idToken // Include the Firebase ID token
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });

            if (response.data && response.data.status === 1) {
                try {
                    await user.delete();
                    if (callbackUrl) {  //  to page called back after 2FA process
                        navigate(callbackUrl);
                        setTwoFaVerified(true);
                        setCallbackUrl("");
                    }
                } catch (deleteError) {
                    console.error("Error cleaning up Firebase user:", deleteError);
                    // This error doesn't affect the user experience, so we don't show it
                }
            } else {
                setError(response.data?.msg || "Failed to verify code");
                if (callbackUrl) {  //  to page called back after 2FA process
                    navigate(callbackUrl);
                    setTwoFaVerified(true);
                    setCallbackUrl("");
                }
            }
        } catch (err) {
            setError(err.message);
            if (callbackUrl) {  //  to page called back after 2FA process
                navigate(callbackUrl);
                setTwoFaVerified(true);
                setCallbackUrl("");
            }

            if (err.code === 'auth/code-expired') {
                setError("Verification code has expired. Please request a new code.");
            } else if (err.code === 'auth/invalid-verification-code') {
                setError("Invalid verification code. Please check and try again.");
            } else {
                setError(`Verification failed: ${err.message || "Code is invalid. Please try again."}`);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleResend = () => {
        window.location.reload();   // =))))
    };

    useEffect(() => {
        const appVerifier = window.recaptchaVerifier;
        let phoneNumber = "";
        const getPhoneNumber = async () => {
            const token = localStorage.getItem("token");
            const phoneNumberRes = await axios.get("/users/api/v1/me/phone-number/get", {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });
            if (phoneNumberRes.data && phoneNumberRes.data.status === 1) {
                phoneNumber = phoneNumberRes.data.data;
            }
            else {
                setError(phoneNumberRes.data.msg);
            }
        }

        const get2FaCode = async () => {
            try {
                window.confirmationResult = await signInWithPhoneNumber(
                    auth,
                    phoneNumber,
                    appVerifier
                );
                console.log("SMS sent successfully");

            } catch (firebaseError) {
                if (firebaseError.code === 'auth/too-many-requests') {
                    // Handle rate limiting specifically
                    setError("Too many verification attempts. Please wait a while (usually around 1 hour) before trying again, or try with a different phone number.");
                } else {
                    throw firebaseError; // Let the outer catch handle other errors
                }
            }
        };

        getPhoneNumber().then(() => {
            if (!phoneNumber) return;
            get2FaCode().then(() => {});
        });

    }, []);

    useEffect(() => {
        const timer = setInterval(() => {
            setTimeLeft(timeLeft - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [timeLeft]);

    return (
        <>
            {error &&
                <Alert showIcon type={"error"} message={error} />
            }
            {!error &&
                <div className="container font-opensans w-[50vw] min-h-[50vh]">
                    <p className="">We've sent 6-digit verification code to your phone</p>
                    <p className="">Enter provided verification code:</p>
                    <div className="flex justify-center gap-2">
                        {Array.from({length: 6}).map((_, i) => (
                            <input
                                key={i}
                                type="text"
                                inputMode="numeric"
                                maxLength="1"
                                className="w-12 h-12 text-center border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                ref={(el) => {
                                    if (!inputsRef.current) inputsRef.current = [];
                                    inputsRef.current[i] = el;
                                }}
                                onChange={(e) => handleChange(e, i)}
                                onKeyDown={(e) => handleKeyDown(e, i)}
                            />
                        ))}
                    </div>
                    {!loading &&
                        <button className="mt-4" onClick={handleVerifyCode}>
                            Verify
                        </button>
                    }
                    {loading &&
                        <button className="mt-4 ml-20 bg-none bg-gray-400 flex items-center">
                            <div className="spinner w-[10px] h-[10px] mr-3" />
                            <p>Verifying...</p>
                        </button>
                    }
                    <div className="mt-5">
                        <p className="text-yellow-500">Code expires in {timeLeft >= 0 ? convertTimeLeft(timeLeft) : "00:00"} mins</p>
                        <div className="flex items-center pl-7">
                            Didn't receive the code?
                            <button className="ml-3 bg-orange-400 bg-none" onClick={handleResend}>Resend</button>
                        </div>
                    </div>
                </div>
            }
        </>
    );
};

export default TwoFaVerification;
