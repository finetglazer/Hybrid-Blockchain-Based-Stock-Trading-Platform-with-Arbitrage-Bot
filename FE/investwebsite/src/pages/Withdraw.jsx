// WithdrawPage.jsx
import React, { useState } from 'react';

const Withdraw = () => {
  const [amount, setAmount] = useState('');
  const [bank, setBank] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [message, setMessage] = useState('');

  const handleWithdraw = (e) => {
    e.preventDefault();

    if (!amount || !bank || !accountNumber) {
      setMessage('Vui lòng điền đầy đủ thông tin.');
      return;
    }

    // Simulate API call
    setTimeout(() => {
      setMessage(`Yêu cầu rút ${amount} VNĐ đến ${bank} - ${accountNumber} đã được gửi.`);
    }, 500);
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="bg-white p-6 rounded-2xl shadow-lg w-full max-w-md">
        <h2 className="text-2xl font-semibold text-center text-gray-800 mb-4">Rút tiền</h2>
        {message && (
          <div className="mb-4 p-3 bg-blue-100 text-blue-800 rounded-md text-sm">
            {message}
          </div>
        )}
        <form onSubmit={handleWithdraw} className="space-y-4">
          <div>
            <label className="block text-gray-600 text-sm mb-1">Số tiền (VNĐ)</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="Nhập số tiền"
              className="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>
          <div>
            <label className="block text-gray-600 text-sm mb-1">Ngân hàng</label>
            <input
              type="text"
              value={bank}
              onChange={(e) => setBank(e.target.value)}
              placeholder="Tên ngân hàng"
              className="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>
          <div>
            <label className="block text-gray-600 text-sm mb-1">Số tài khoản</label>
            <input
              type="text"
              value={accountNumber}
              onChange={(e) => setAccountNumber(e.target.value)}
              placeholder="Nhập số tài khoản"
              className="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>
          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 rounded-xl hover:bg-blue-700 transition duration-200"
          >
            Rút tiền
          </button>
        </form>
      </div>
    </div>
  );
};

export default Withdraw;
