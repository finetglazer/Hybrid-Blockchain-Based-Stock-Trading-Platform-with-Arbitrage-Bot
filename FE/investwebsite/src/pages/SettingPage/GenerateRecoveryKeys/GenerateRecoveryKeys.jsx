import "./GenerateRecoveryKeys.css";
import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import axios from "axios";
import {Alert} from "antd";

const GenerateRecoveryKeys = () => {
    const navigate = useNavigate();

    const [twoFaEnabled, setTwoFaEnabled] = useState(undefined);
    const [error, setError] = useState("");
    const [recoveryKeys, setRecoveryKeys] = useState([]);
    const [generating, setGenerating] = useState(false);

    const onClickBackBtn = () => {
        navigate("/setting");
    };
    const onClickEnable2FaBtn = () => {
        navigate("/setting/2fa-settings")
    };
    const onClickGenerateRecoveryKeysBtn = async () => {
        const token = localStorage.getItem("token");
        try {
            setGenerating(true);
            const response = await axios.post("/users/api/v1/auth/2fa/recovery-keys/generate", {},{
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                }
            });
            if (response.data && response.data.status === 1) {
                setRecoveryKeys(response.data.data.recoveryKeys);
                setError("");
                setGenerating(false);
            }
            else {
                setGenerating(false);
                setError(response.data.msg);
            }
        }
        catch (e) {
            setGenerating(false);
            setError(e.message);
        }
    };

    useEffect(() => {
        const token = localStorage.getItem("token");
        const get2FaInfo = async () => {
            try {
                const response = await axios.get("/users/api/v1/auth/2fa/info", {
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json"
                    }
                });
                if (response.data && response.data.status === 1) {
                    setTwoFaEnabled(true);
                    setError("");
                }
                else {
                    console.log(response.data.data);
                    if (typeof(response.data.data) === "string" && response.data.data === "NOT_ENABLED") {
                        setTwoFaEnabled(false);
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

        get2FaInfo().then(() => {});
    }, []);

    return (
        <div className="container generate-recovery-keys-container">
            <div className="title" onClick={onClickBackBtn}>
                <button><img src="../../../../src/assets/left-arrow.png" alt="back icon"/></button>
                <p>Generate recovery keys</p>
            </div>
            <div className="body">
                <p className="description">
                    Backup code used to regain access to your account if you're unable to sign in due to forgotten credentials, a lost device, or other issues
                </p>
                {twoFaEnabled && !error && !generating &&
                    <button className="btn generate-btn" onClick={onClickGenerateRecoveryKeysBtn}>
                        Generate recovery keys
                    </button>
                }
                {twoFaEnabled === false && !error &&
                    <>
                        <p>You have to enable 2-Factor Authentication first</p>
                        <button className="btn enable-2fa-btn" onClick={onClickEnable2FaBtn}>
                            Enable 2-Factor Authentication
                        </button>
                    </>
                }
                {error &&
                    <Alert type="error" showIcon message={error} />
                }
                {generating &&
                    <button className="btn generate-btn disabled-btn" style={{display: "flex", alignItems: 'center'}} onClick={onClickGenerateRecoveryKeysBtn}>
                        <div className="spinner" style={{width: 20, height: 20, marginRight: 10}} />Generate recovery keys
                    </button>
                }
                {recoveryKeys.length > 0 && (
                    <div className="recovery-keys">
                        <h4>Recovery Keys</h4>
                        <p>Store these recovery keys in a safe place. You'll need them if you lose access to your phone.</p>
                        <div>
                            {recoveryKeys.map((key, index) => (
                                <div key={index} className="recovery-key">
                                    {key}
                                </div>
                            ))}
                        </div>
                        <button
                            className="button"
                            onClick={() => navigate("/setting")}
                        >
                            Return to Setting
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default GenerateRecoveryKeys;
