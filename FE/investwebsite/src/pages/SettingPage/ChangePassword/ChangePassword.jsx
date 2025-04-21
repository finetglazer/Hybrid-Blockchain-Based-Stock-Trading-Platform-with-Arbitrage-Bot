import React, {useState} from "react";
import "./ChangePassword.css";
import {useNavigate} from "react-router-dom";
import axios from "axios";
import {Alert} from "antd";

const ChangePassword = () => {
    const navigate = useNavigate();

    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordError, setPasswordError] = useState("");
    const [passwordsMismatch, setPasswordsMismatch] = useState(false);
    const [changePasswordSucceeded, setChangePasswordSucceeded] = useState(undefined);
    const [changePasswordError, setChangePasswordError] = useState("");
    const [showOldPassword, setShowOldPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const validateNewPassword = (password) => {
        /*
        * Note:
        *    00 - Satisfied
        *    01 - Password length < 8
        *    02 - No lowercase letters found
        *    03 - No uppercase letters found
        *    04 - No numbers found
        *    05 - No special characters found
        * */
        if (password.length < 8) {
            return "01";
        }
        if (!password.match(/[a-z]/)) {
            return "02";
        }
        if (!password.match(/[A-Z]/)) {
            return "03";
        }
        if (!password.match(/[0-9]/)) {
            return "04";
        }
        if (!password.match(/[^a-zA-Z0-9]/)) {
            return "05";
        }
        return "00";
    };

    const onOldPasswordChange = (e) => {
        setOldPassword(e.target.value);
    };

    const onNewPasswordChange = (e) => {
        setNewPassword(e.target.value);
        const currentPassword = e.target.value;
        const validationRes = validateNewPassword(currentPassword);
        if (validationRes === "01") {
            setPasswordError("Password length should be greater or equal to 8");
        }
        else if (validationRes === "02") {
            setPasswordError("Password should have at least 1 lowercase letter");
        }
        else if (validationRes === "03") {
            setPasswordError("Password should have at least 1 uppercase letter");
        }
        else if (validationRes === "04") {
            setPasswordError("Password should have at least 1 digit");
        }
        else if (validationRes === "05") {
            setPasswordError("Password should have at least 1 special character");
        }
        else {
            setPasswordError("")
        }

        if (confirmPassword && confirmPassword !== currentPassword) {
            setPasswordsMismatch(true);
        }
        if (confirmPassword === currentPassword) {
            setPasswordsMismatch(false);
        }
    };

    const onConfirmPasswordChange = (e) => {
        setConfirmPassword(e.target.value);
        const password = e.target.value;
        if (password !== newPassword) {
            setPasswordsMismatch(true);
        }
        else {
            setPasswordsMismatch(false);
        }
    };

    const onClickBackBtn = () => {
        setChangePasswordSucceeded(undefined);
        navigate("/setting");
    };

    const onSubmit = async() => {
        try {
            const token = localStorage.getItem("token");
            const response = await axios.post("http://localhost:8081/users/api/v1/auth/change-password", {
                oldPassword: oldPassword,
                newPassword: newPassword
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            });

            if (response.data) {
                if (response.data.status === 1) {
                    setChangePasswordSucceeded(true);
                    setChangePasswordError(false);
                }
                else {
                    setChangePasswordSucceeded(false);
                    setChangePasswordError(response.data.msg);
                }
            }
        }
        catch (e) {
            setChangePasswordError(e);
        }
    };

    return (
        <div className="container change-password-container">
            {
                changePasswordSucceeded ? <Alert
                    message="Password changed successfully"
                    type="success"
                    showIcon
                />
                    : (changePasswordSucceeded === false ? <Alert
                            message="Change password failed"
                            description={changePasswordError}
                            type="error"
                            showIcon
                        /> : null)
            }
            <div className="title" onClick={onClickBackBtn}>
                <button><img src="../../../../src/assets/left-arrow.png" alt="back icon"/></button>
                <p>Password change</p>
            </div>
            <div className="body">
                <div className="label-input-pair old-password-wrapper">
                    <p>Old password</p>
                    <div>
                        <input
                            className="input"
                            type={showOldPassword ? "text" : "password"}
                            required={true}
                            onChange={onOldPasswordChange}
                        />
                    </div>
                    {showOldPassword
                        ? <button className="show-password-btn"><img src="../../../../src/assets/hide.png" alt="show password" onClick={() => setShowOldPassword(!showOldPassword)}/></button>
                        : <button className="hide-password-btn"><img src="../../../../src/assets/view.png" alt="hide password" onClick={() => setShowOldPassword(!showOldPassword)}/></button>
                    }
                </div>
                <div className="label-input-pair new-password-wrapper">
                    <p>New password</p>
                    <div>
                        <input
                            className="input"
                            type={showNewPassword ? "text" : "password"}
                            required={true}
                            onChange={onNewPasswordChange}
                        />
                    </div>
                    {showNewPassword
                        ? <button className="show-password-btn"><img src="../../../../src/assets/hide.png" alt="show password" onClick={() => setShowNewPassword(!showNewPassword)}/></button>
                        : <button className="hide-password-btn"><img src="../../../../src/assets/view.png" alt="hide password" onClick={() => setShowNewPassword(!showNewPassword)}/></button>
                    }
                    {passwordError &&
                        <div className="password-error-wrapper">
                            <img src="../../../../src/assets/warning.png" alt="error" />
                            <p className="description">{passwordError}</p>
                        </div>
                    }
                </div>
                <div className="label-input-pair confirm-password-wrapper">
                    <p>Confirm password</p>
                    <div>
                        <input
                            className="input"
                            type={showConfirmPassword ? "text" : "password"}
                            required={true}
                            onChange={onConfirmPasswordChange}
                        />
                    </div>
                    {showConfirmPassword
                        ? <button className="show-password-btn"><img src="../../../../src/assets/hide.png" alt="show password" onClick={() => setShowConfirmPassword(!showConfirmPassword)}/></button>
                        : <button className="hide-password-btn"><img src="../../../../src/assets/view.png" alt="hide password" onClick={() => setShowConfirmPassword(!showConfirmPassword)}/></button>
                    }
                    {passwordError &&
                        <div className="password-error-wrapper">
                            <img src="../../../../src/assets/warning.png" alt="error" />
                            <p className="description">{passwordError}</p>
                        </div>
                    }
                </div>
                <button
                    className="submit-new-password-btn"
                    disabled={!oldPassword || !newPassword || !confirmPassword
                              || passwordError || passwordsMismatch}
                    onClick={onSubmit}
                >Submit
                </button>
            </div>
        </div>
    );
};

export default ChangePassword;
