import {createContext, useContext, useState} from "react";

const AppContext = createContext({
    // Callback values for urls required 2FA
    callbackUrl: "",
    setCallbackUrl: () => {},

    // 2FA Settings values
    twoFaEnabled: undefined,
    email2FaEnabled: undefined,
    phone2FaEnabled: undefined,
    authenticatorApp2FaEnabled: undefined,
    setTwoFaEnabled: () => {},
    setEmail2FaEnabled: () => {},
    setPhone2FaEnabled: () => {},
    setAuthenticatorApp2FaEnabled: () => {},
});

export const AppContextProvider = ({ children }) => {
    const [twoFaEnabled, setTwoFaEnabled] = useState(undefined); // [undefined, true, false]
    const [email2FaEnabled, setEmail2FaEnabled] = useState(undefined);
    const [phone2FaEnabled, setPhone2FaEnabled] = useState(undefined);
    const [authenticatorApp2FaEnabled, setAuthenticatorApp2FaEnabled] = useState(undefined);
    const [callbackUrl, setCallbackUrl] = useState("");

    const value = {
        twoFaEnabled,
        email2FaEnabled,
        phone2FaEnabled,
        authenticatorApp2FaEnabled,
        callbackUrl,
        setTwoFaEnabled,
        setEmail2FaEnabled,
        setPhone2FaEnabled,
        setAuthenticatorApp2FaEnabled,
        setCallbackUrl,
    }

    return (
        <AppContext.Provider value={value}>
            {children}
        </AppContext.Provider>
    )
}

export const useAppContext = () => {
    return useContext(AppContext);
}


