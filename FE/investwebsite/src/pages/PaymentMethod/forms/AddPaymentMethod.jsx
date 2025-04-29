import {useState} from "react";
import {Checkbox, FormControlLabel, InputLabel, MenuItem, Select, TextField} from "@mui/material";
import axios from "axios";
import {Alert} from "antd";

const AddPaymentMethod = ({onSuccess, onCancel}) => {
    const [formData, setFormData] = useState({
        type: "",
        nickname: "",
        setAsDefault: true,
        accountNumber: "",
        routingNumber: "",
        accountHolderName: "",
        bankName: "",
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const onChange = (changedValue) => {
        setFormData(Object.assign({}, formData, changedValue));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        const token = localStorage.getItem("token");
        try {
            const response = await axios.post("/accounts/payment-methods/api/v1/me/create", {
                type: formData.type,
                nickname: formData.nickname,
                setAsDefault: formData.setAsDefault,
                details: {
                    accountNumber: formData.accountNumber,
                    routingNumber: formData.routingNumber,
                    accountHolderName: formData.accountHolderName,
                    bankName: formData.bankName,
                }
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            });
            setLoading(false);
            if (response.data && response.data.status === 1) {
                setFormData(Object.assign({}, formData));
                setError("");
                onSuccess();
            }
            else {
                setError(response.data.message);
            }
        }
        catch (e) {
            setLoading(false);
            setError(e.message);
        }
    };

    const onClickCancelBtn = () => {
        setFormData(Object.assign({}, formData));
        onCancel();
    };

    return (
        <form
            className="container add-payment-method-form-container"
            style={{position: "absolute", left: "35%"}}
            onSubmit={handleSubmit}
        >
            {!loading && error ?
                <Alert type="error" showIcon message="Error" description={error} />
                : null
            }
            <InputLabel id="select-label" style={{textAlign: "left", color: "white"}}>Select type</InputLabel>
            <Select
                id="select-label"
                defaultValue="BANK_ACCOUNT"
                value={formData.type}
                fullWidth
                style={{color: "white"}}
                onChange={(e) => onChange({type: e.target.value})}
                variant="outlined"
            >
                <MenuItem value="BANK_ACCOUNT">
                    <div style={{display: "flex", alignItems: "center"}}>
                        <img src="../../../../src/assets/atm-card.png" style={{margin: "-5px 10px 0 0"}} alt="bank account" width={20} height={20} />
                        Bank account
                    </div>
                </MenuItem>
                <MenuItem value="CREDIT_CARD">
                    <div style={{display: "flex", alignItems: "center"}}>
                        <img src="../../../../src/assets/credit-card.png" style={{margin: "-5px 10px 0 0"}}  alt="credit card" width={20} height={20} />
                        Credit card
                    </div>
                </MenuItem>
                <MenuItem value="DEBIT_CARD">
                    <div style={{display: "flex", alignItems: "center"}}>
                        <img src="../../../../src/assets/debit-card.png" style={{margin: "-5px 10px 0 0"}}  alt="debit card" width={20} height={20} />
                        Debit card
                    </div>
                </MenuItem>
                <MenuItem value="DIGITAL_WALLET">
                    <div style={{display: "flex", alignItems: "center"}}>
                        <img src="../../../../src/assets/digital-wallet.png" style={{margin: "-5px 10px 0 0"}}  alt="Digital wallet" width={20} height={20} />
                        Digital wallet
                    </div>
                </MenuItem>
            </Select>
            <TextField
                label="Nickname"
                variant="outlined"
                fullWidth
                margin="normal"
                value={formData.nickname}
                onChange={(e) => onChange({nickname: e.target.value})}
                required
            />
            <TextField
                label="Account number"
                variant="outlined"
                fullWidth
                margin="normal"
                type="number"
                value={formData.accountNumber}
                onChange={(e) => onChange({accountNumber: e.target.value})}
                required
            />
            <TextField
                label="Routing number"
                variant="outlined"
                fullWidth
                margin="normal"
                value={formData.routingNumber}
                type="number"
                onChange={(e) => onChange({routingNumber: e.target.value})}
                required
            />
            <TextField
                label="Account holder name"
                variant="outlined"
                fullWidth
                margin="normal"
                value={formData.accountHolderName}
                onChange={(e) => onChange({accountHolderName: e.target.value})}
                required
            />
            <TextField
                label="Bank name"
                variant="outlined"
                fullWidth
                margin="normal"
                value={formData.bankName}
                onChange={(e) => onChange({bankName: e.target.value})}
                required
            />
            <FormControlLabel
                control={
                    <Checkbox
                        checked={formData.setAsDefault}
                        style={{marginLeft: "-60%"}}
                        onChange={(e) => onChange({setAsDefault: e.target.checked})} />
                }
                label="Set as default"
            />
            <div className="form-btns" style={{display: "flex", justifyContent: "flex-end"}}>
                {loading
                    ? <button className="submit-verify-payment-method-form" disabled style={{display:"flex" ,background: "gray", cursor: "default"}}>
                        <p className="spinner" style={{width: "10px", height: "10px", marginRight: "10px"}} />
                        <p style={{fontSize: "1rem"}}>Submit</p>
                      </button>
                    : <button className="submit-verify-payment-method-form">Submit</button>
                }
                <button className="cancel" style={{background: "orange"}} onClick={onClickCancelBtn}>Cancel</button>
            </div>
        </form>
    );
};

export default AddPaymentMethod;
