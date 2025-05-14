import "./2FASettings.css";
import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {useAppContext} from "../../../AppContextProvider.jsx";
import axios from "axios";
import {Alert} from "antd";

const TwoFactorAuthenticationSettings = () => {
    const navigate = useNavigate();
    const {setCallbackUrl} = useAppContext();

    const [twoFaEnabled, setTwoFaEnabled] = useState(undefined);
    const [phone2FaEnabled, setPhone2FaEnabled] = useState(undefined);
    const [wantToEnable2Fa, setWantToEnable2Fa] = useState(undefined);
    const [tradingPermissions, setTradingPermissions] = useState([]);
    const [error, setError] = useState("");

    const onClickBackBtn = () => {
        navigate("/setting");
    };

    const onClickTurnOn2FaBtn = () => {
        setWantToEnable2Fa(true);
        setCallbackUrl("http://localhost:5173/setting");
        navigate("/two-factor-auth");
    };

    const onClickDisable2FaBtn = () => {
        setWantToEnable2Fa(false);
        setCallbackUrl("http://localhost:5173/setting");
        navigate("/two-factor-auth");
    }

    useEffect(() => {
        const token = localStorage.getItem("token");
        const get2FaInfo = async () => {
            try {
                const response = await axios.get("/users/api/v1/auth/2fa/info", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                });
                if (response.data && response.data.status === 1) {
                    setTwoFaEnabled(true);
                    setPhone2FaEnabled(true);
                    setError("");
                }
                else {
                    console.log(response.data.data);
                    if (typeof(response.data.data) === "string" && response.data.data === "NOT_ENABLED") {
                        setTwoFaEnabled(false);
                        setPhone2FaEnabled(false);
                        setError("");
                    }
                    else {
                        setError(response.data.msg);
                    }
                }
            }
            catch (e) {
                setError(e.message);
            }
        }

        const getTradingPermissions = async () => {
            try {
                const response = await axios.get("/users/api/v1/me/trading-permissions", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                });
                if (response.data && response.data.status === 1) {
                    setTradingPermissions(response.data.data.permissions);
                    setError("");
                }
                else {
                    setError(response.data.msg);
                }
            }
            catch (e) {
                setError(e.message);
            }
        }

        get2FaInfo().then(() => {});
        getTradingPermissions().then(() => {});
    }, []);

    useEffect(() => {
        if (wantToEnable2Fa) {        // This condition only happens when called back after 2FA and user wants to enable 2FA
            setTwoFaEnabled(true);
            setPhone2FaEnabled(true);
        }
        else if (wantToEnable2Fa === false) {  // This condition only happens when called back after 2FA and user wants to disable 2FA
            setTwoFaEnabled(false);
            setPhone2FaEnabled(false);
        }
    }, [wantToEnable2Fa]);

    return (
        <div className="container two-fa-settings-container">
            {twoFaEnabled === undefined && error &&
                <Alert type="error" showIcon message={error} />
            }
            <div className="title" onClick={onClickBackBtn}>
                <button><img src="../../../../src/assets/left-arrow.png" alt="back icon"/></button>
                <p>2-Factor Authentication</p>
            </div>
            <div className="body">
                <div className="first-section">
                    <p className="content">Prevent hackers from accessing your account with an additional layer of security.</p>
                    {twoFaEnabled === undefined && <div className="spinner" />}
                    {twoFaEnabled === true && <button onClick={onClickDisable2FaBtn}>Disable 2-Factor Authentication</button>}
                    {twoFaEnabled === false && <button onClick={onClickTurnOn2FaBtn}>Turn on 2-Factor Authentication</button>}
                </div>
                <div className="second-section">
                    <div className="title">
                        <p>Second steps</p>
                        <p>Choose your appropriate second-step authentication option, or you can opt for multiple authentication options</p>
                        <div className="options-wrapper">
                            <Option imageSrc="../../../../src/assets/email.png" name="Email" loading={false} enabled={true}></Option>
                            <Option imageSrc="../../../../src/assets/call.png" name="Phone number" loading={phone2FaEnabled === undefined} enabled={phone2FaEnabled}></Option>
                            <Option imageSrc="../../../../src/assets/qr.png" name="Authenticator" loading={false} enabled={false} />
                            <div className="two-fa-option">
                                <div className="option-icon"><img src="../../../../src/assets/permission.png" alt="Security category icon" /></div>
                                <div className="name"><span>Trading permissions</span></div>
                                <div className="description-wrapper">
                                    {tradingPermissions.length === 0 && <div className="spinner two-fa-option-spinner" />}
                                    {tradingPermissions.length !== 0 && (
                                        <p className="permissions">
                                            {tradingPermissions.map((permission) => permission).join(', ')}
                                        </p>
                                    )}
                                </div>
                            </div>
                            <p style={{fontSize: "0.8rem", fontWeight: 400, fontStyle: "italic"}}>(*) Account with 2-Factor Authentication can have permission to trade</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

const Option = (props) => {
    return (
        <div className="two-fa-option">
            <div className="option-icon"><img src={props.imageSrc} alt="Security category icon" /></div>
            <div className="name"><span>{props.name}</span></div>
            <div className="description-wrapper">
                {props.loading && <div className="spinner two-fa-option-spinner" />}
                {!props.loading && props.enabled && <img src="./../../../../src/assets/tick.png" alt="Tick icon" />}
                {!props.loading && !props.enabled && <img src="./../../../../src/assets/warning.png" alt="Warning icon" />}
            </div>
        </div>
    );
}

export default TwoFactorAuthenticationSettings;
