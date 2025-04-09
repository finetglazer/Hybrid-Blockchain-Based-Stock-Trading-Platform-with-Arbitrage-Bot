import React, { useState, useEffect } from "react";
import {
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Checkbox,
} from "@mui/material";
import axios from "axios";

const CreateAccount = ({ numOfAccount, onSuccess }) => {
  const [nickname, setNickname] = useState("");
  const [currency, setCurrency] = useState("");
  const [agree, setAgree] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    console.log("Current number of accounts:", numOfAccount);
  }, [numOfAccount]); // Mỗi lần numOfAccount thay đổi, useEffect sẽ chạy

  const validateNickname = (name) => {
    return name.length >= 3 && name.length <= 30;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateNickname(nickname)) {
      setError("Nickname must be between 3 and 30 characters.");
      return;
    }
    setError("");

    try {
      const response = await axios.post("/api/accounts/api/v1/create", {
        currency,
        nickname,
      });

      if (response.status === 201) {
        onSuccess(); // Cập nhật số lượng tài khoản
      }
    } catch (error) {
      console.error(
        "Error creating account:",
        error.response?.data || error.message
      );
    }
  };

  return (
    <>
      <form onSubmit={handleSubmit}>
        <TextField
          label="Account Nickname"
          variant="outlined"
          fullWidth
          margin="normal"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          error={!!error}
          helperText={error}
        />
        <FormControl fullWidth margin="normal">
          <InputLabel>Currency</InputLabel>
          <Select
            value={currency}
            onChange={(e) => setCurrency(e.target.value)}
          >
            <MenuItem value="USD">
              <div style={{ display: "flex", alignItems: "center" }}>
                <img
                  src="/UsaFlag.svg"
                  alt="USA"
                  width={24}
                  height={16}
                  style={{ marginRight: 8, borderRadius: "50%" }}
                />
                USD
              </div>
            </MenuItem>
            <MenuItem value="EUR">
              <div style={{ display: "flex", alignItems: "center" }}>
                <img
                  src="/EuroFlag.svg"
                  alt="Euro"
                  width={24}
                  height={16}
                  style={{ marginRight: 8 }}
                />
                EUR
              </div>
            </MenuItem>
            <MenuItem value="VND">
              <div style={{ display: "flex", alignItems: "center" }}>
                <img
                  src="/VietNamFlag.svg"
                  alt="Vietnam"
                  width={24}
                  height={16}
                  style={{ marginRight: 8 }}
                />
                VND
              </div>
            </MenuItem>
          </Select>
        </FormControl>

        <FormControlLabel
          control={
            <Checkbox
              checked={agree}
              onChange={(e) => setAgree(e.target.checked)}
            />
          }
          label="I agree to the Terms and Conditions"
        />
        <Button
          type="submit"
          variant="contained"
          color="primary"
          fullWidth
          disabled={!agree}
        >
          Create Account
        </Button>
      </form>
    </>
  );
};

export default CreateAccount;
