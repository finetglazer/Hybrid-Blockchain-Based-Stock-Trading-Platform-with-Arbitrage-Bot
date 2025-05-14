import React, {useEffect, useState} from "react";
import './Setting.css'
import {useNavigate} from "react-router-dom";
import axios from "axios";

const Setting = () => {
    const navigate = useNavigate();

    const [twoFaEnabled, setTwoFaEnabled] = useState(undefined);

    const onClick2Fa = () => {
        navigate("/setting/2fa-settings");
    };

    const onClickPassword = () => {
        navigate("/setting/change-password");
    };

    const onClickGenerateRecoveryKeys = () => {
        navigate("/setting/generate-recovery-keys");
    };

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
                }
                else {
                    console.log(response.data.data);
                    if (typeof(response.data.data) === "string" && response.data.data === "NOT_ENABLED") {
                        setTwoFaEnabled(false);
                    }
                    else {
                        console.log(response.data.msg);
                    }
                }
            }
            catch (e) {
                console.log(e.message);
            }
        }

        get2FaInfo().then(() => {});
    }, []);

    return (
      <div className="container setting-container">
          <div className="title">
              <p className="name">Security</p>
              <p className="description">Settings to help you keep your account secure</p>
          </div>
          <div className="setting-body">
              <p className="title">How you sign in our website</p>
              <SecurityCategory imageSrc="./src/assets/2FA.png" name="2-Factor Authentication" description={"2-Factor Authentication is " + (twoFaEnabled ? "enabled" : (twoFaEnabled === false ? "disabled" : "undefined"))} onClick={onClick2Fa} />
              <SecurityCategory imageSrc="./src/assets/key.png" name="Password" description="●●●●●●●●●" onClick={onClickPassword} />
              <SecurityCategory imageSrc="./src/assets/session.png" name="View active sessions" description="2 active sessions" onClick={() => {}}  />
              <SecurityCategory imageSrc="./src/assets/account-recovery.png" name="Generate recovery keys" description="" onClick={onClickGenerateRecoveryKeys}  />
          </div>
      </div>
  );
};

const SecurityCategory = (props) => {
  return (
      <div className="security-category-container cursor-pointer" onClick={props.onClick}>
          <div className="icon"><img src={props.imageSrc} alt="Security category icon" /></div>
          <span className="name">{props.name}</span>
          <span className="description">{props.description}</span>
          <img src="./src/assets/right-arrow.png" alt="Go to icon" />
      </div>
  )
}

export default Setting;
