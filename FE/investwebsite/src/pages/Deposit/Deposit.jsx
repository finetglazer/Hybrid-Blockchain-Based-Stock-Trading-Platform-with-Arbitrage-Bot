import React, {useEffect, useState} from 'react';
import {useParams} from "react-router-dom";
import axios from "axios";
import {Alert, Tag} from "antd";

const Deposit = () => {
  const accountId = useParams().accountId;
  const paymentMethodId = useParams().paymentMethodId;

  const [amount, setAmount] = useState('');
  const [message, setMessage] = useState('');
  const [fetchAccountLoading, setFetchAccountLoading] = useState(true);
  const [fetchPaymentMethodLoading, setFetchPaymentMethodLoading] = useState(true);
  const [fetchAccountError, setFetchAccountError] = useState("");
  const [fetchPaymentMethodError, setFetchPaymentMethodError] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [account, setAccount] = useState();
  const [paymentMethod, setPaymentMethod] = useState();

  const validate = () => {
    const num = parseFloat(amount);
    if (!amount) {
      setError('Please fill in all fields.');
      return false;
    }
    if (isNaN(num) || num < 1000 || num > 3000000) {
      setError('Amount must be between 1000 and $3,000,000.');
      return false;
    }
    setError('');
    return true;
  };

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

  const handleDeposit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    setShowConfirm(true);
  };

  const confirmDeposit = async () => {
    setShowConfirm(false);
    setLoading(true);
    const token = localStorage.getItem("token");
    try {
      const response = await fetch('/sagas/api/v1/deposit/start', {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + token,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId: account.userId,
          accountId: accountId,
          paymentMethodId: paymentMethodId,
          amount: parseFloat(amount),
          currency: "USD",
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(response.message || 'An error occurred while processing the deposit.');
      }

      setMessage(`Your deposit request of $${amount} via "${paymentMethod.nickname}" has been submitted.\nTransaction ID: ${data.transactionId || `tx${Date.now()}`}`);
      setAmount('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

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
    };

    fetchPaymentMethod().then(() => {});
  }, [paymentMethodId]);

  return (
    <div className="min-h-[50vh] min-w-[60vw] flex items-center justify-center bg-gradient-to-br from-blue-100 to-blue-200 p-4">
      <div className="container min-w-[60vw] shadow-xl rounded-2xl p-6 relative">
        {loading && <div className="text-blue-600 mb-4 text-center font-medium">Processing transaction...</div>}

        {message && (
          <div className="mb-4 p-4 rounded-md bg-green-100 text-green-700 text-sm whitespace-pre-line">
            {message}
          </div>
        )}

        {error && (
          <div className="mb-4 p-4 rounded-md bg-red-100 text-red-700 text-sm">
            {error}
          </div>
        )}

        {fetchAccountError &&
            <Alert showIcon type="error" description={fetchAccountError} />
        }
        {fetchPaymentMethodError &&
            <Alert showIcon type="error" description={fetchPaymentMethodError} />
        }

        {!fetchAccountLoading && !fetchPaymentMethodLoading && !fetchAccountError && !fetchPaymentMethodError && (
          <div className="w-full m-0 p-0">
            <div className="flex">
              <button className="bg-none -mt-4 -ml-7 hover:bg-none" onClick={() => {window.location.assign(`/${accountId}/deposit/choose-payment-method`)}}>
                <img src="../../../src/assets/left-arrow.png" alt="back icon"
                     width={25}
                     height={15}
                />
              </button>
              <h2 className="text-3xl font-bold text-center mb-6 text-gray-800">Deposit</h2>
            </div>
            <div>
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
                    <p className="ml-2 mt-[12px]">{account.nickname}</p>
                  </div>

                  <p className="-mt-1.5">
                    ${convert(account.balance.total.toFixed(2))}
                  </p>
                </div>
              </div>
            </div>
            <form onSubmit={handleDeposit} className="mt-5">
              <div>
                <label className="block text-gray-700 text-left font-medium mb-1">Amount (USD)</label>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="Enter deposit amount"
                  className="w-full h-12 -ml-1 px-4 py-2 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <div className="flex flex-wrap gap-2 mt-2 text-sm">
                  {[1000, 50000, 100000].map(val => (
                    <button
                      key={val}
                      type="button"
                      className="px-3 py-1 border rounded-xl text-blue-600 hover:bg-blue-50 transition"
                      onClick={() => setAmount(val.toString())}
                    >
                      ${val}
                    </button>
                  ))}
                </div>
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
              <div className="text-left mt-6">
                <button
                  type="submit"
                  className=" w-[150px] bg-orange-600 text-white py-2 ml-0 hover:bg-blue-700 transition text-[1.1rem]"
                >
                  Deposit
                </button>
              </div>
            </form>
          </div>
        )}

        {showConfirm && (
          <div className="absolute inset-0 bg-black bg-opacity-40 flex items-center justify-center z-20">
            <div className="bg-white p-6 rounded-2xl shadow-2xl max-w-sm w-full text-center space-y-4">
              <h3 className="text-xl font-semibold text-gray-800">Confirm Transaction</h3>
              <p className="text-sm">Are you sure you want to deposit <strong>${amount}</strong> using:</p>
              <p className="font-medium text-blue-700">{paymentMethod.nickname}</p>
              <div className="flex justify-center gap-4 mt-4">
                <button
                  onClick={confirmDeposit}
                  className="bg-green-600 text-white px-4 py-2 rounded-xl hover:bg-green-700 transition"
                >
                  Confirm
                </button>
                <button
                  onClick={() => setShowConfirm(false)}
                  className="bg-gray-300 text-gray-800 px-4 py-2 rounded-xl hover:bg-gray-400 transition"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Deposit;
