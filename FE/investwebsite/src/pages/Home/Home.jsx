import React, { useState } from "react";
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import CreateAccount from "../CreateAccount";
import "./Home.css";
const Home = () => {
  const [open, setOpen] = useState(false);
  const [numOfAccount, setNumOfAccount] = useState(0);

  const handleAccountCreated = () => {
    setNumOfAccount((prev) => prev + 1);
    setOpen(false); // Đóng dialog sau khi tạo thành công
  };

  return (
    <>
      <div className="create-trading-account">
        <p className="account-question">
          {numOfAccount === 0 ? "Do you have any account?" : ""}
        </p>
        <Button
          variant="contained"
          color="primary"
          onClick={() => setOpen(true)}
          className="btn-add-account"
        >
          +
        </Button>

        <Dialog
          open={open}
          onClose={() => setOpen(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>Create a Trading Account</DialogTitle>
          <DialogContent>
            <CreateAccount
              numOfAccount={numOfAccount}
              onSuccess={handleAccountCreated}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)} color="secondary">
              Cancel
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    </>
  );
};

export default Home;
