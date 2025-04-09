import React from "react";
import './Setting.css'
import {useNavigate} from "react-router-dom";

const Setting = () => {
    const navigate = useNavigate();

    const onClick2Fa = () => {
        navigate("/setting/2fa-settings");
    };

    const onClickPassword = () => {
        navigate("/setting/change-password");
    };

    return (
      <div className="container setting-container">
          <div className="title">
              <p className="name">Security</p>
              <p className="description">Settings to help you keep your account secure</p>
          </div>
          <div className="setting-body">
              <p className="title">How you sign in our website</p>
              <SecurityCategory imageSrc="./src/assets/2FA.png" name="2-Factor Authentication" description="2-Factor Authentication is disabled" onClick={onClick2Fa} />
              <SecurityCategory imageSrc="./src/assets/key.png" name="Password" description="●●●●●●●●●" onClick={onClickPassword} />
              <SecurityCategory imageSrc="./src/assets/session.png" name="View active sessions" description="2 active sessions" onClick={() => {}}  />
              <SecurityCategory imageSrc="./src/assets/account-recovery.png" name="Recovery options" description="Phone number" onClick={() => {}}  />
          </div>
      </div>
  );
};

const SecurityCategory = (props) => {
  return (
      <div className="security-category-container" onClick={props.onClick}>
          <div className="icon"><img src={props.imageSrc} alt="Security category icon" /></div>
          <span className="name">{props.name}</span>
          <span className="description">{props.description}</span>
          <img src="./src/assets/right-arrow.png" alt="Go to icon" />
      </div>
  )
}

export default Setting;
