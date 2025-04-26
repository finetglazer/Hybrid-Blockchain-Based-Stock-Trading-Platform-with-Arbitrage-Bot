import React, { useState } from "react";
import {
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Checkbox,
  Box,
} from "@mui/material";
import axios from "axios";

const CreateAccount = ({ onSuccess }) => {
  const [nickname, setNickname] = useState("");
  const [currency, setCurrency] = useState("");
  const [agree, setAgree] = useState(false);
  const [error, setError] = useState({ nickname: "", currency: "" });

  const validateNickname = (name) => {
    return name.length >= 3 && name.length <= 30;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validate nickname
    if (!validateNickname(nickname)) {
      setError({
        nickname: "Nickname must be between 3 and 30 characters.",
        currency: "",
      });
      return;
    }

    // Validate currency
    if (!currency) {
      setError({
        nickname: "",
        currency: "Please select a currency.",
      });
      return;
    }

    // Clear previous errors
    setError({ nickname: "", currency: "" });

    // Get token from localStorage
    const token = localStorage.getItem("token");
    console.log(token);

    try {
      const response = await axios.post(
        "/accounts/api/v1/create",
        {
          currency,
          nickname,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status === 201 || response.status === 200) {
        onSuccess(); // Gọi callback khi tạo tài khoản thành công
        console.log("response: ", response.status, response.data);
      }
    } catch (error) {
      console.error(
        "Error creating account:",
        error.response?.data || error.message
      );
      alert("Failed to create account. Please check token or server.");
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <TextField
        label="Account Nickname"
        variant="outlined"
        fullWidth
        margin="normal"
        value={nickname}
        onChange={(e) => setNickname(e.target.value)}
        error={!!error.nickname}
        helperText={error.nickname}
      />

      <FormControl fullWidth margin="normal" error={!!error.currency}>
        <InputLabel>Currency</InputLabel>
        <Select
          value={currency}
          onChange={(e) => setCurrency(e.target.value)}
          label="Currency"
        >
          <MenuItem value="USD">
            <Box sx={{ display: "flex", alignItems: "center" }}>
              <img
                src="/UsaFlag.svg"
                alt="USA"
                width={24}
                height={16}
                style={{ marginRight: 8, borderRadius: "50%" }}
              />
              USD
            </Box>
          </MenuItem>
          <MenuItem value="EUR">
            <Box sx={{ display: "flex", alignItems: "center" }}>
              <img
                src="/EuroFlag.svg"
                alt="Euro"
                width={24}
                height={16}
                style={{ marginRight: 8 }}
              />
              EUR
            </Box>
          </MenuItem>
          <MenuItem value="VND">
            <Box sx={{ display: "flex", alignItems: "center" }}>
              <img
                src="/VietNamFlag.svg"
                alt="Vietnam"
                width={24}
                height={16}
                style={{ marginRight: 8 }}
              />
              VND
            </Box>
          </MenuItem>
        </Select>
        {error.currency && (
          <div style={{ color: "red", fontSize: "0.8rem", marginTop: 4 }}>
            {error.currency}
          </div>
        )}
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
  );
};

export default CreateAccount;
