import {useState} from "react";
import {Checkbox, FormControlLabel, InputLabel, MenuItem, Select, TextField} from "@mui/material";
import axios from "axios";
import {Alert} from "antd";

const VerifyPaymentMethod = ({onSuccess, onCancel, item}) => {
    const [formData, setFormData] = useState({
        amount1: 0.0,
        amount2: 0.0,
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
            const response = await axios.post(`/accounts/payment-methods/api/v1/${item.id}/verify`, {
                verificationType: "MICRO_DEPOSITS",
                verificationDataRequest: {
                    amount1: formData.amount1,
                    amount2: formData.amount2,
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
            className="container verify-payment-method-form-container"
            style={{position: "fixed", left: "35%", zIndex: 999}}
            onSubmit={handleSubmit}
        >
            {!loading && error ?
                <Alert type="error" showIcon message="Error" description={error} />
                : null
            }
            <TextField
                label="Amount 1"
                variant="outlined"
                fullWidth
                margin="normal"
                value={formData.amount1}
                onChange={(e) => onChange({amount1: e.target.value})}
                required
            />
            <TextField
                label="Amount 2"
                variant="outlined"
                fullWidth
                margin="normal"
                type="number"
                value={formData.amount2}
                onChange={(e) => onChange({amount2: e.target.value})}
                required
            />

            <div className="form-btns" style={{display: "flex", justifyContent: "flex-end"}}>
                {loading
                    ? <button className="submit-add-payment-method-form" disabled style={{display:"flex" ,background: "gray", cursor: "default"}}>
                        <p className="spinner" style={{width: "10px", height: "10px", marginRight: "10px"}} />
                        <p style={{fontSize: "1rem"}}>Submit</p>
                    </button>
                    : <button className="submit-add-payment-method-form">Submit</button>
                }
                <button className="cancel-add-payment-method-form" style={{background: "orange"}} onClick={onClickCancelBtn}>Cancel</button>
            </div>
        </form>

    );
};

export default VerifyPaymentMethod;
