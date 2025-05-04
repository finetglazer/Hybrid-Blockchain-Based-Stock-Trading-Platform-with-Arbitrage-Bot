import axios from 'axios';
import React, { useEffect, useState } from 'react';
import {useNavigate, useParams} from "react-router-dom";
import {Alert, Tag} from "antd";
import {useAppContext} from "../../AppContextProvider.jsx";

const Withdraw = () => {
  const accountId = useParams().accountId;
  const navigate = useNavigate();
  const {setCallbackUrl} = useAppContext();

  const [amount, setAmount] = useState('');
  const paymentMethodId = useParams().paymentMethodId;
  const {twoFaVerified, setTwoFaVerified} = useAppContext();
  const [account, setAccount] = useState("");
  const [paymentMethod, setPaymentMethod] = useState(null);
  const [twoFaEnabled, setTwoFaEnabled] = useState(false);
  const [balance, setBalance] = useState(null);
  const [reason, setReason] = useState('');
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [fetchAccountLoading, setFetchAccountLoading] = useState(true);
  const [fetchPaymentMethodLoading, setFetchPaymentMethodLoading] = useState(true);
  const [fetchAccountError, setFetchAccountError] = useState("");
  const [fetchPaymentMethodError, setFetchPaymentMethodError] = useState("");

  useEffect(() => {
    const fetchAccount = async() => {
      const token = localStorage.getItem("token");
      try {
        setFetchAccountLoading(true);
        const response = await axios.get(`/accounts/api/v1/${accountId}`, {
          headers: {
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json"
          }
        });
        setFetchAccountLoading(false);
        if (response.data && response.data.status === 1) {
          setFetchAccountError("");
          setAccount(response.data.data);
          setBalance(response.data.data.balance);
        } else {
          setFetchAccountError(response.data.msg);
        }
      } catch (e) {
        setFetchAccountLoading(false);
        setFetchAccountError(e.message);
      }
    };
    fetchAccount().then(() => {});
  }, [accountId]);

  useEffect(() => {
    const fetchPaymentMethod = async() => {
      const token = localStorage.getItem("token");
      try {
        setFetchPaymentMethodLoading(true);
        const response = await axios.get(`/accounts/payment-methods/api/v1/${paymentMethodId}`, {
          headers: {
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json"
          }
        });
        setFetchPaymentMethodLoading(false);
        if (response.data && response.data.status === 1) {
          setFetchPaymentMethodError("");
          setPaymentMethod(response.data.data);
        } else {
          setFetchPaymentMethodError(response.data.msg);
        }
      } catch (e) {
        setFetchPaymentMethodLoading(false);
        setFetchPaymentMethodError(e.message);
      }
    }

    fetchPaymentMethod().then(() => {});
  }, [paymentMethodId]);

  useEffect(() => {
    const get2FaStatus = async () => {
        const token = localStorage.getItem("token");
        const response = await axios.get("/users/api/v1/me/verification-status", {
            headers: {
              "Authorization": "Bearer " + token,
              "Content-type": "application/json"
            }
        });
        if (response.data && response.data.status === 1) {
            setTwoFaEnabled(response.data.data.phoneVerified);
        }
        else {
          setError(response.data.msg);
        }
    };

    get2FaStatus().then(() => {});
  }, [accountId]);


  const convert = (decimal) => {
    let str = decimal.toString();
    let res = "";
    let count = 0;
    const isDecimal = str.includes(".");
    for (let i = (isDecimal ? str.split(".")[0].length - 1 : str.length - 1); i >= 0 && str[i] !== "-"; --i) {
      ++count;
      res = str[i] + res;
      if (count % 3 === 0) res = "," + res;
    }
    if (res[0] === ",") res = res.substring(1, res.length);
    return (decimal < 0 || decimal[0] === "-" ? "-" : "") + res + (isDecimal ? "." + str.split(".")[1] : "");
  };

  const handleWithdraw = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    if (!amount || Number(amount) < 1000 || Number(amount) > balance?.available) {
      setError('Amount must be between 1,000$ and available balance');
      return;
    }

    if (!termsAccepted) {
      setError('You must agree with our terms');
      return;
    }

    if (!twoFaVerified) {
      setError("Please verify 2FA first");
      return;
    }

    try {
      const token = localStorage.getItem("token");
      let twoFaEnabled = false;
      const verificationStatusRes = await axios.get("/users/api/v1/me/verification-status", {
          headers: {
              "Authorization": "Bearer " + token,
              "Content-Type": "application/json"
          }
      });
      if (verificationStatusRes.data && verificationStatusRes.data.status === 1) {
         twoFaEnabled = verificationStatusRes.data.data.phoneVerified;
      }
      else {
          setError(verificationStatusRes.data.msg);
          return;
      }

      if (!twoFaEnabled) {
          setError("Please enable 2-Factor Authentication first");
          return;
      }
      const withdrawalRes = await axios.post("/sagas/api/v1/withdrawal/start", {
        userId: account.userId,
        accountId: accountId,
        paymentMethodId: paymentMethodId,
        amount: amount,
        currency: 'USD',
        description: reason
      }, {
        headers: {
          "Authorization": "Bearer " + token,
          "Content-Type": "application/json"
        }
      });
      if (withdrawalRes.data && withdrawalRes.data.sagaId) {
        setMessage(`Withdrawal request sent. Transaction ID: ${withdrawalRes.data.transactionId}`);
        setAmount('');
        setTwoFaVerified(false);
        setReason('');
        setTermsAccepted(false);
      }
      else {
        setError(withdrawalRes.data.msg);
      }
    } catch (e) {
      setError(e.message);
    }
  };

  return (
      <>
        {fetchAccountError &&
            <Alert showIcon type="error" description={fetchAccountError} />
        }
        {fetchPaymentMethodError &&
            <Alert showIcon type="error" description={fetchPaymentMethodError} />
        }
        {!fetchAccountLoading && !fetchPaymentMethodLoading && !fetchAccountError && !fetchPaymentMethodError &&
        <div className="container min-w-[60vw] bg-zinc-800 bg-opacity-80 backdrop-blur-md rounded-2xl shadow-lg p-8 space-y-6 border border-zinc-700">
          <div className="text-left flex items-center -ml-[20px]">
              <button className="bg-none hover:bg-none">
                <img src="../../../src/assets/left-arrow.png" alt="back icon" className="w-[20px] h-[20px]" />
              </button>
              <p className="text-3xl font-bold text-white mt-4">Withdraw</p>
          </div>
          <div className="bg-green-700/60 rounded-[15px] flex">
            <img
                src="../../../src/assets/dollar.png"
                alt="account icon"
                className="w-[60px] h-[60px] mt-2.5 ml-[5px]"
            />

            <div className="text-left ml-2.5">
              <div className="flex items-center">
                <Tag
                    bordered={false}
                    style={{ height: 22 }}
                    color={(() => {
                      switch (account.status) {
                        case "ACTIVE":
                          return "green";
                        case "INACTIVE":
                          return "red";
                        case "RESTRICTED":
                          return "warning";
                        default:
                          return "black";
                      }
                    })()}
                >
                  {account.status ?? "UNDEFINED"}
                </Tag>
                <p className="ml-1 mt-[12px]">{account.nickname}</p>
              </div>

              <p className="-mt-1.5">
                ${convert(account.balance.total.toFixed(2))}
              </p>
            </div>
          </div>

          {balance && (
            <p className="text-center text-sm text-zinc-300">
              Available balance: <span className="text-emerald-400 font-medium">${convert(balance.available.toFixed(2))}</span>
            </p>
          )}

          {message && (
            <div className="p-4 bg-green-100/10 text-green-300 border border-green-500 rounded-md text-sm font-semibold">
              {message}
            </div>
          )}
          {error && (
            <div className="p-4 bg-red-100/10 text-red-400 border border-red-500 rounded-md text-sm font-semibold">
              {error}
            </div>
          )}
          <form onSubmit={handleWithdraw}>
            <div className="font-opensans">
                {twoFaEnabled && !twoFaVerified &&
                  <button className="mb-10" onClick={() => {
                      setCallbackUrl(window.location.pathname);
                      navigate("/2fa-verify");
                  }}>
                      2-Factor Authentication here
                  </button>
                }
                {!twoFaEnabled &&
                  <button className="mb-10" onClick={() => {
                      navigate("/setting/2fa-settings");
                  }}>
                      Enable 2-Factor Authentication here
                  </button>
                }
                {twoFaVerified &&
                    <button className="mb-10 bg-none bg-gray-500 hover:bg-gray-500 hover:bg-none cursor-default" disabled={true}>
                        2FA verified
                    </button>
                }
              <label className="block text-sm font-medium mb-1 text-left ml-2">Amount (USD)</label>
              <input
                type="number"
                min="1"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="Withdrawal amount"
                className="w-full px-4 py-2 bg-zinc-900 text-white border border-zinc-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1 text-left ml-2">Withdrawal reason (optional)</label>
              <select
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="w-full px-4 py-2 bg-zinc-900 text-white border border-zinc-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">-- Reason --</option>
                <option value="investment">Investment</option>
                <option value="personal_use">Personal Expenses</option>
                <option value="other">Other</option>
              </select>
              {reason === "other" &&
                <input className="w-full h-auto mt-5 text-left align-top px-2 py-1" placeholder="Your reason"/>
              }
            </div>
            <div className="text-sm text-left mt-7">
              <p className="text-sm text-gray-500">Payment method name:</p>
              <p className="-mt-2.5">{paymentMethod.nickname}</p>
            </div>
            <div className="text-left mt-6">
              <p className="text-sm text-gray-500">Status:</p>
              <div className="text-left -mt-3">
                <Tag bordered={false}
                     color={(() => {
                       switch (paymentMethod.status) {
                         case "ACTIVE":
                           return "green";
                         case "INACTIVE":
                           return "red";
                         case "VERIFICATION_PENDING":
                           return "warning";
                         default:
                           return "black";
                       }
                     })()}>{paymentMethod.status}</Tag>
              </div>
            </div>
            <div className="text-left mt-6">
              <p className="text-sm text-gray-500">Commission:</p>
              <p className="text-sm -mt-2.5">0%</p>
            </div>
            <div className="text-left mt-6">
              <p className="text-sm text-gray-500">Estimated processing time:</p>
              <p className="text-sm -mt-2.5">15 minutes</p>
            </div>
            <div className="flex items-center text-left my-5">
              <input
                type="checkbox"
                checked={termsAccepted}
                onChange={() => setTermsAccepted(!termsAccepted)}
                className="form-checkbox h-4 w-4 m-0 ml-0.5 mr-3 text-blue-500 border-gray-500 rounded focus:ring-blue-500"
              />
              <label className="text-sm">
                I agree with {' '}
                <a href="#" className="text-blue-400 underline hover:text-blue-300">
                  withdrawal terms
                </a>
              </label>
            </div>
            <button
              type="submit"
              className="w-[120px] bg-blue-600 text-white font-semibold py-2 rounded-xl hover:bg-blue-700 transition"
            >
              Withdraw
            </button>
          </form>
        </div>
        }
      </>
  );
};

export default Withdraw;
