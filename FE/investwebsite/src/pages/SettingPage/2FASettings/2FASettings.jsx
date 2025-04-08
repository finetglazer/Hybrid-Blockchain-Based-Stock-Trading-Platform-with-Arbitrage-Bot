import "./2FASettings.css";
import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {useAppContext} from "../../../AppContextProvider.jsx";

const TwoFactorAuthenticationSettings = () => {
    const navigate = useNavigate();
    const {setCallbackUrl} = useAppContext();

    const [twoFaEnabled, setTwoFaEnabled] = useState(undefined);
    const [email2FaEnabled, setEmail2FaEnabled] = useState(undefined);
    const [phone2FaEnabled, setPhone2FaEnabled] = useState(undefined);
    const [authenticatorApp2FaEnabled, setAuthenticatorApp2FaEnabled] = useState(undefined);
    const [enable2Fa, setEnable2Fa] = useState(undefined);

    const onClickBackBtn = () => {
        navigate("/setting");
    };

    const onClickTurnOn2FaBtn = () => {
        setEnable2Fa(true);
        setCallbackUrl(window.location.href);
        navigate("/two-factor-auth");
    };

    const onClickDisable2FaBtn = () => {
        setEnable2Fa(false);
        setCallbackUrl(window.location.href);
        navigate("/two-factor-auth");
    }

    useEffect(() => {
        if (enable2Fa) {        // This condition only happens when called back after 2FA and user wants to enable 2FA
            setTwoFaEnabled(true);
            setPhone2FaEnabled(true);
        }
        else if (enable2Fa === false) {  // This condition only happens when called back after 2FA and user wants to disable 2FA
            setTwoFaEnabled(false);
            setPhone2FaEnabled(false);
        }
        else {      // enable2Fa === undefined means user have just accessed this page from security setting page
            setTwoFaEnabled(false);     // Assume that 2FA is disabled by default (2FA enable or disable should be queried using API)
        }
    }, [enable2Fa]);

    return (
        <div className="container two-fa-settings-container">
            <div className="title" onClick={onClickBackBtn}>
                <button><img src="../../../../src/assets/left-arrow.png" alt="back icon"/></button>
                <p>2-Factor Authentication</p>
            </div>
            <div className="body">
                <div className="first-section">
                    <p className="title">Turn on 2-Factor Authentication</p>
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
                            <Option imageSrc="../../../../src/assets/email.png" name="Email" loading={email2FaEnabled === undefined} enabled={false}></Option>
                            <Option imageSrc="../../../../src/assets/call.png" name="Phone number" loading={phone2FaEnabled === undefined} enabled={true}></Option>
                            <Option imageSrc="../../../../src/assets/qr.png" name="Authenticator" loading={authenticatorApp2FaEnabled === undefined} enabled={false} />
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
