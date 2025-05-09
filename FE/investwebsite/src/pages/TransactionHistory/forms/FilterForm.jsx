import {InputLabel, MenuItem, Select, TextField} from "@mui/material";
import {Alert, DatePicker, Tag, TimePicker} from "antd";
import dayjs from 'dayjs';
import React, {useState} from "react";
import axios from "axios";

const FilterForm = ({onSuccess, onCancel}) => {
    const [formData, setFormData] = useState({
        createdStartDate: "1970-01-01",
        createdEndDate: dayjs().format("YYYY-MM-DD"),
        createdStartTime: "00:00:00",
        createdEndTime: "23:59:59",

        amountFrom: 0,
        amountTo: 9999999999.99999,
        types: ["ALL"],
        statuses: ["ALL"],
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [createdTimeInputError, setCreatedTimeInputError] = useState("");
    const [amountInputError, setAmountInputError] = useState("");

    const onSubmit = async (e) => {
        e.preventDefault();

        setLoading(true);
        const token = localStorage.getItem("token");
        try {
            console.log(formData);
            formData.types.splice(formData.types.findIndex(type => type === "ALL"), 1);
            formData.statuses.splice(formData.statuses.findIndex(status => status === "ALL"), 1);
            const response = await axios.post("/accounts/transactions/api/v1/get", {
                startDate: formData.createdStartDate,
                endDate: formData.createdEndDate,
                startTime: formData.createdStartTime,
                endTime: formData.createdEndTime,
                types: formData.types,
                statuses: formData.statuses,
                page: 0,
                size: 9999,
            }, {
                headers: {
                    "Authorization": "Bearer " + token,
                    "Content-Type": "application/json"
                }
            });
            setLoading(false);
            if (response.data && response.data.status === 1) {
                onSuccess(response.data.data.items);
            }
            else {
                setError(e.data.msg);
            }
        }
        catch (e) {
            setLoading(false);
            setError(e.message);
        }
    }

    const onClickCancelBtn = () => {
        setFormData({
            createdStartDate: "1970-01-01",
            createdEndDate: dayjs().format("YYYY-MM-DD"),
            createdStartTime: "00:00:00",
            createdEndTime: "23:59:59",

            amountFrom: 0,
            amountTo: 99999999999.999,
            types: ["ALL"],
            statuses: ["ALL"],
            externalReferenceId: "",
            description: "",
        });
        setError("");

        onCancel();
    };

    const onChange = (changedField) => {
        const tempFormData = Object.assign({}, formData, changedField);
        const createdStartDate = dayjs(tempFormData.createdStartDate, "YYYY-MM-DD");
        const createdEndDate = dayjs(tempFormData.createdEndDate, "YYYY-MM-DD");
        const createdStartTime = dayjs(tempFormData.createdStartTime, "HH:mm:ss");
        const createdEndTime = dayjs(tempFormData.createdEndTime, "HH:mm:ss");

        const amountFrom = tempFormData.amountFrom;
        const amountTo = tempFormData.amountTo;

        if (createdStartDate.isAfter(createdEndDate)) {
            setCreatedTimeInputError("Start date should be before End date");
        }
        else if (createdStartDate.isSame(createdEndDate) && createdStartTime.isAfter(createdEndTime)) {
            setCreatedTimeInputError("Start time should be before End time");
        }
        else {
            setCreatedTimeInputError("");
        }

        if (amountFrom < 0) {
            setAmountInputError("Amount from should be >= 0");
            return;
        }
        else if (amountTo < 0) {
            setAmountInputError("Amount to should be >= 0");
        }
        else if (amountFrom > amountTo) {
            setAmountInputError("Amount from should not exceed Amount to");
            return;
        }
        else if (amountFrom > 99999999999.999) {
            setAmountInputError("Amount from should not exceed 9999999999.99");
            return;
        }
        else if (amountTo > 99999999999.999) {
            setAmountInputError("Amount to should not exceed 9999999999.99");
            return;
        }
        else {
            setAmountInputError("");
        }

        setFormData(Object.assign({}, formData, changedField));
    };

    const onCloseTypeTag = (tag) => {
        const typesArr = formData.types;
        let index = typesArr.findIndex((item) => item === tag);
        const updatedTypesArr = index === -1 ? typesArr : [
            ...typesArr.slice(0, index),
            ...typesArr.slice(index + 1)
        ];
        onChange({types: [...updatedTypesArr]});
    };

    const onCloseStatusTag = (tag) => {
        const statusesArr = formData.statuses;
        let index = statusesArr.findIndex((item) => item === tag);
        const updatedStatusesArr = index === -1 ? statusesArr : [
            ...statusesArr.slice(0, index),
            ...statusesArr.slice(index + 1)
        ];
        onChange({statuses: [...updatedStatusesArr]});
    };

    return (
        <form
            className="container filter-form-container"
            style={{width: '40%', position: 'absolute', zIndex: 9, top: 40, left: '30%'}}
        >
            {!loading && error &&
                <Alert type="error" showIcon description={error}/>
            }
            <InputLabel
                id="time-range created-time-range-input"
                style={{color: '#ffb65e', textAlign: 'left', fontSize: '1.3rem', fontWeight: 'bold', marginBottom: '15px'}}
            >Created time</InputLabel>
            <div className="created-time-from-input" style={{display: "flex"}}>
                <p>From date</p>
                <DatePicker
                    picker="date"
                    defaultValue={dayjs('1970-01-01', 'YYYY-MM-DD')}
                    style={{zIndex: 99999, width: '150px', height: '40px', margin: '5px 0 0 10px', color: 'white', background: 'rgba(59,173,92,0.77)', border: 'none'}}
                    onChange={(date, dateString) => onChange({createdStartDate: dateString})}
                />
                <p style={{marginLeft: '70px'}}>Start at</p>
                <TimePicker
                    defaultValue={dayjs('00:00:00', 'HH:mm:ss')}   // Now
                    style={{width: '150px', height: '40px', margin: '5px 0 0 10px', color: 'white', background: 'rgba(59,173,92,0.77)', border: 'none'}}
                    onChange={(time, timeString) => onChange({createdStartTime: timeString})}
                />
            </div>
            <div className="created-time-to-input" style={{display: "flex", marginTop: '20px'}}>
                <p style={{marginRight: '20px'}}>To date</p>
                <DatePicker
                    picker="date"
                    defaultValue={dayjs()}  // Today
                    style={{width: '150px', height: '40px', margin: '5px 0 0 10px', color: 'white', background: 'rgba(59,173,92,0.77)', border: 'none'}}
                    onChange={(date, dateString) => onChange({createdEndDate: dateString})}
                />
                <p style={{margin: '15px 5px 0 70px'}}>End at</p>
                <TimePicker
                    defaultValue={dayjs()}   // Now
                    style={{width: '150px', height: '40px', margin: '5px 0 0 10px', color: 'white', background: 'rgba(59,173,92,0.77)', border: 'none'}}
                    onChange={(time, timeString) => onChange({createdEndTime: timeString})}
                />
            </div>
            {createdTimeInputError && (
                <div style={{display: "flex", alignItems: 'center'}}>
                   <img width={20} height={20} style={{marginRight: '7px'}} src="../../../../src/assets/warning.png" alt="warning icon"/>
                    <p style={{color: 'orange'}}>{createdTimeInputError}</p>
                </div>
            )}

            <InputLabel
                id="amount-range-input"
                style={{color: '#ffb65e', textAlign: 'left', fontSize: '1.3rem', fontWeight: 'bold', marginBottom: '15px', marginTop: '40px'}}
            >Amount range</InputLabel>
            <div className="amount-range-input" style={{display: "flex"}}>
                <p style={{marginRight: '30px'}}>From</p>
                <TextField
                    type="number"
                    variant="outlined"
                    value={formData.amountFrom}
                    sx={{
                        input: {
                            color: 'white',
                            boxShadow: 'none',
                        },
                        width: '150px',
                        height: '50px',
                        border: 'none',
                        background: 'rgba(59,173,92,0.77)',
                        borderRadius: 2,
                        '& .MuiOutlinedInput-root': {
                            '& fieldset': {
                                borderColor: 'transparent',
                            },
                            '&:hover fieldset': {
                                borderColor: 'transparent',
                            },
                            '&.Mui-focused fieldset': {
                                borderColor: 'transparent',
                            },
                        },
                    }}
                    onChange={(e) => onChange({amountFrom: e.target.value})}
                />
                <p style={{marginRight: '30px', marginLeft: '50px'}}>To</p>
                <TextField
                    type="number"
                    variant="outlined"
                    value={formData.amountTo}
                    sx={{
                        input: {
                            color: 'white',
                            boxShadow: 'none',
                        },
                        width: '150px',
                        height: '50px',
                        border: 'none',
                        background: 'rgba(59,173,92,0.77)',
                        borderRadius: 2,
                        '& .MuiOutlinedInput-root': {
                            '& fieldset': {
                                borderColor: 'transparent',
                                boxShadow: 'none',
                            },
                            '&:hover fieldset': {
                                borderColor: 'transparent',
                            },
                            '&.Mui-focused fieldset': {
                                borderColor: 'transparent',
                                boxShadow: 'none',
                            },
                        },
                    }}
                    onChange={(e) => onChange({amountTo: e.target.value})}
                />
            </div>
            {amountInputError && (
                <div style={{display: "flex", alignItems: 'center'}}>
                   <img width={20} height={20} style={{marginRight: '7px'}} src="../../../../src/assets/warning.png" alt="warning icon"/>
                    <p style={{color: 'orange'}}>{amountInputError}</p>
                </div>
            )}
            <div className="type-input" style={{display: "flex", alignItems: 'center', marginTop: '15px'}}>
                <p style={{color: '#ffb65e', textAlign: 'left', fontSize: '1.3rem', fontWeight: 'bold', marginBottom: '15px', marginTop: '40px'}}>Type</p>
                <Select
                    label="Type"
                    style={{width: "180px", height: "40px", margin: "25px 30px 0 50px", color: "white", background: 'rgba(59,173,92,0.77)', border: 'none'}}
                    onChange={(e) => {
                        const typesArr = formData.types;
                        const type = e.target.value;
                        if (!typesArr.includes(type)) {
                            typesArr.push(type);
                        }
                        onChange({types: [...typesArr]})
                    }}
                    defaultValue="ALL"
                    variant="outlined"
                >
                    <MenuItem value="ALL">ALL</MenuItem>
                    <MenuItem value="DEPOSIT">DEPOSIT</MenuItem>
                    <MenuItem value="WITHDRAWAL">WITHDRAWAL</MenuItem>
                    <MenuItem value="INTEREST">INTEREST</MenuItem>
                    <MenuItem value="FEE">FEE</MenuItem>
                    <MenuItem value="TRANSFER">TRANSFER</MenuItem>
                    <MenuItem value="ORDER_PAYMENT">ORDER_PAYMENT</MenuItem>
                    <MenuItem value="REFUND">REFUND</MenuItem>
                </Select>
                <div style={{marginTop: "30px", textAlign: "left"}}>
                    {
                        formData.types.map(value => {
                            switch (value) {
                                case "ALL":
                                    return (
                                        <Tag bordered={false} color="default" closable onClose={onCloseTypeTag}>ALL</Tag>
                                    );
                                case "DEPOSIT":
                                    return (
                                        <Tag bordered={false} color="magenta" closable onClose={onCloseTypeTag}>DEPOSIT</Tag>
                                    );
                                case "WITHDRAWAL":
                                    return (
                                        <Tag bordered={false} color="purple" closable onClose={onCloseTypeTag}>WITHDRAWAL</Tag>
                                    );
                                case "INTEREST":
                                    return (
                                        <Tag bordered={false} color="cyan" closable onClose={onCloseTypeTag}>INTEREST</Tag>
                                    );
                                case "FEE":
                                    return (
                                        <Tag bordered={false} color="red" closable onClose={onCloseTypeTag}>FEE</Tag>
                                    );
                                case "TRANSFER":
                                    return (
                                        <Tag bordered={false} color="gold" closable onClose={onCloseTypeTag}>TRANSFER</Tag>
                                    );
                                case "ORDER_PAYMENT":
                                    return (
                                        <Tag bordered={false} color="lime" closable onClose={onCloseTypeTag}>ORDER PAYMENT</Tag>
                                    );
                                case "REFUND":
                                    return (
                                        <Tag bordered={false} color="geekblue" closable onClose={onCloseTypeTag}>REFUND</Tag>
                                    );
                            }
                        })
                    }
                </div>
            </div>

            <div className="status-input" style={{display: "flex", alignItems: 'center', marginTop: '15px'}}>
                <p style={{color: '#ffb65e', textAlign: 'left', fontSize: '1.3rem', fontWeight: 'bold', marginBottom: '15px', marginTop: '40px'}}>Status</p>
                <Select
                    label="Status"
                    style={{width: "180px", height: "40px", margin: "25px 30px 0 35px", color: "white", background: 'rgba(59,173,92,0.77)', border: 'none'}}
                    onChange={(e) => {
                        const statusesArr = formData.statuses;
                        const status = e.target.value;
                        if (!statusesArr.includes(status)) {
                            statusesArr.push(status);
                        }
                        onChange({statuses: [...statusesArr]})
                    }}
                    defaultValue="ALL"
                    variant="outlined"
                >
                    <MenuItem value="ALL">ALL</MenuItem>
                    <MenuItem value="PENDING">PENDING</MenuItem>
                    <MenuItem value="COMPLETED">COMPLETED</MenuItem>
                    <MenuItem value="FAILED">FAILED</MenuItem>
                    <MenuItem value="CANCELLED">CANCELLED</MenuItem>
                </Select>
                <div style={{marginTop: "30px", textAlign: "left"}}>
                    {
                        formData.statuses.map(value => {
                            switch (value) {
                                case "ALL":
                                    return (
                                        <Tag bordered={false} color="default" closable onClose={onCloseStatusTag}>ALL</Tag>
                                    );
                                case "PENDING":
                                    return (
                                        <Tag bordered={false} color="cyan-inverse" closable onClose={onCloseStatusTag}>PENDING</Tag>
                                    );
                                case "COMPLETED":
                                    return (
                                        <Tag bordered={false} color="green-inverse" closable onClose={onCloseStatusTag}>COMPLETED</Tag>
                                    );
                                case "FAILED":
                                    return (
                                        <Tag bordered={false} color="red-inverse" closable onClose={onCloseStatusTag}>FAILED</Tag>
                                    );
                                case "CANCELLED":
                                    return (
                                        <Tag bordered={false} color="purple-inverse" closable onClose={onCloseStatusTag}>CANCELLED</Tag>
                                    );
                            }
                        })
                    }
                </div>
            </div>

            {/*<div className="external-reference-id-input" style={{textAlign: "left"}}>*/}
            {/*    <InputLabel*/}
            {/*        id="amount-range-input"*/}
            {/*        style={{color: '#ffb65e', textAlign: 'left', fontSize: '1.3rem', fontWeight: 'bold', marginBottom: '15px', marginTop: '40px'}}*/}
            {/*    >External Reference Id</InputLabel>*/}
            {/*    <TextField*/}
            {/*        placeholder="ext-14m15n153468u821@"*/}
            {/*        variant="outlined"*/}
            {/*        value={formData.externalReferenceId}*/}
            {/*        sx={{*/}
            {/*            input: {*/}
            {/*                color: 'white',*/}
            {/*                boxShadow: 'none',*/}
            {/*            },*/}
            {/*            width: '400px',*/}
            {/*            height: '50px',*/}
            {/*            border: 'none',*/}
            {/*            background: 'rgba(59,173,92,0.77)',*/}
            {/*            borderRadius: 2,*/}
            {/*            '& .MuiOutlinedInput-root': {*/}
            {/*                '& fieldset': {*/}
            {/*                    borderColor: 'transparent',*/}
            {/*                    boxShadow: 'none',*/}
            {/*                },*/}
            {/*                '&:hover fieldset': {*/}
            {/*                    borderColor: 'transparent',*/}
            {/*                },*/}
            {/*                '&.Mui-focused fieldset': {*/}
            {/*                    borderColor: 'transparent',*/}
            {/*                },*/}
            {/*            },*/}
            {/*        }}*/}
            {/*        onChange={(e) => onChange({externalReferenceId: e.target.value})}*/}
            {/*    />*/}
            {/*</div>*/}

            {/*<div className="description-input" style={{textAlign: "left"}}>*/}
            {/*    <InputLabel*/}
            {/*        id="amount-range-input"*/}
            {/*        style={{color: '#ffb65e', textAlign: 'left', fontSize: '1.3rem', fontWeight: 'bold', marginBottom: '15px', marginTop: '40px'}}*/}
            {/*    >Description</InputLabel>*/}
            {/*    <TextField*/}
            {/*        placeholder="Transfer aid to my son"*/}
            {/*        variant="outlined"*/}
            {/*        value={formData.description}*/}
            {/*        sx={{*/}
            {/*            input: {*/}
            {/*                color: 'white',*/}
            {/*                boxShadow: 'none',*/}
            {/*            },*/}
            {/*            width: '600px',*/}
            {/*            height: '150px',*/}
            {/*            border: 'none',*/}
            {/*            background: 'rgba(59,173,92,0.77)',*/}
            {/*            borderRadius: 2,*/}
            {/*            '& .MuiOutlinedInput-root': {*/}
            {/*                '& fieldset': {*/}
            {/*                    borderColor: 'transparent',*/}
            {/*                    boxShadow: 'none',*/}
            {/*                },*/}
            {/*                '&:hover fieldset': {*/}
            {/*                    borderColor: 'transparent',*/}
            {/*                },*/}
            {/*                '&.Mui-focused fieldset': {*/}
            {/*                    borderColor: 'transparent',*/}
            {/*                },*/}
            {/*            },*/}
            {/*        }}*/}
            {/*        onChange={(e) => onChange({description: e.target.value})}*/}
            {/*    />*/}
            {/*</div>*/}
            <div className="form-btns" style={{display: "flex", justifyContent: "flex-end"}}>
                {loading
                    ? <button className="submit-verify-payment-method-form" disabled style={{display:"flex", padding: '0 20px', alignItems: 'center', background: "gray", cursor: "default"}}>
                        <p className="spinner" style={{width: "20px", height: "20px", marginRight: "10px", marginTop: "20px"}} />
                        <p style={{fontSize: "1rem", marginTop: "10px"}}>Filter</p>
                    </button>
                    : <button className="submit-verify-payment-method-form" onClick={(e) => onSubmit(e)}>Filter</button>
                }
                <button className="cancel" style={{background: "orange"}} onClick={onClickCancelBtn}>Cancel</button>
            </div>
        </form>
    );
};

export default FilterForm;

/*
* time range (complete, create, update),
* amount range,
* types,
* statuses,
* external reference id,
* description,
*
*
* reports and exports
*/
