import React, { useState } from 'react';

const Deposit = () => {
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState('');
  const [message, setMessage] = useState('');

  const paymentMethods = [
    { value: 'bank_transfer', label: 'Chuyển khoản ngân hàng' },
    { value: 'credit_card', label: 'Thẻ tín dụng / ghi nợ' },
    { value: 'momo', label: 'Ví MoMo' },
  ];

  const handleDeposit = (e) => {
    e.preventDefault();
    if (!amount || !method) {
      setMessage('Vui lòng nhập đầy đủ thông tin.');
      return;
    }

    // Giả lập xử lý nạp tiền
    setTimeout(() => {
      setMessage(`Yêu cầu nạp ${amount} VNĐ bằng phương thức "${paymentMethods.find(m => m.value === method)?.label}" đã được gửi.`);
      setAmount('');
      setMethod('');
    }, 500);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
      <div className="bg-white shadow-lg rounded-2xl w-full max-w-md p-6">
        <h2 className="text-2xl font-semibold text-center mb-4 text-gray-800">Nạp tiền</h2>

        {message && (
          <div className="mb-4 p-3 rounded-md bg-green-100 text-green-700 text-sm">
            {message}
          </div>
        )}

        <form onSubmit={handleDeposit} className="space-y-4">
          <div>
            <label className="block text-gray-600 text-sm mb-1">Số tiền (VNĐ)</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="Nhập số tiền cần nạp"
              className="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>

          <div>
            <label className="block text-gray-600 text-sm mb-1">Phương thức nạp</label>
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400"
            >
              <option value="">-- Chọn phương thức --</option>
              {paymentMethods.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 rounded-xl hover:bg-blue-700 transition duration-200"
          >
            Nạp tiền
          </button>
        </form>
      </div>
    </div>
  );
};

export default Deposit;
