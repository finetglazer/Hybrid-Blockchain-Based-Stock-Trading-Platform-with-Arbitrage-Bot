import {useState} from "react";
import {Checkbox, FormControlLabel, InputLabel, MenuItem, Select, TextField} from "@mui/material";
import axios from "axios";
import {Alert} from "antd";

const UpdatePaymentMethod = ({onSuccess, onCancel, item}) => {
    const [formData, setFormData] = useState({
        nickname: item.nickname,
        setAsDefault: item.setAsDefault,
        status: item.status,
        accountHolderName: item.accountHolderName,
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
            const response = await axios.put(`/accounts/payment-methods/api/v1/${item.id}/update`, {
                nickname: formData.nickname,
                setAsDefault: formData.setAsDefault,
                status: formData.status,
                metadata: {
                    accountHolderName: formData.accountHolderName,
                },
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
                setError(response.data.msg);
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
            className="container update-payment-method-form-container"
            style={{position: "fixed", left: "35%", zIndex: 999}}
            onSubmit={handleSubmit}
        >
            {!loading && error ?
                <Alert type="error" showIcon message="Error" description={error} />
                : null
            }
            <InputLabel id="select-status-label" style={{textAlign: "left", color: "white"}}>Select status</InputLabel>

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
                label="Account holder name"
                variant="outlined"
                fullWidth
                margin="normal"
                value={formData.accountHolderName}
                onChange={(e) => onChange({accountHolderName: e.target.value})}
                required
            />
            <FormControlLabel
                control={
                    <Checkbox
                        checked={formData.setAsDefault}
                        style={{marginLeft: "-50%"}}
                        onChange={(e) => onChange({setAsDefault: e.target.checked})} />
                }
                label="Set as default"
            />
            <div className="form-btns" style={{display: "flex", justifyContent: "flex-end"}}>
                {loading
                    ? <button className="submit-update-payment-method-form" disabled style={{display:"flex" ,background: "gray", cursor: "default"}}>
                        <p className="spinner" style={{width: "10px", height: "10px", marginRight: "10px"}} />
                        <p style={{fontSize: "1rem"}}>Submit</p>
                    </button>
                    : <button className="submit-update-payment-method-form">Submit</button>
                }
                <button className="cancel-update-payment-method-form" style={{background: "orange"}} onClick={onClickCancelBtn}>Cancel</button>
            </div>
        </form>

    );
};

export default UpdatePaymentMethod;
