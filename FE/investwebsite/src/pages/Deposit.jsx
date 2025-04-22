import React, { useState } from 'react';

const Deposit = () => {
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [processingTime, setProcessingTime] = useState('Dự kiến xử lý trong 5 phút');
  const [paymentMethods, setPaymentMethods] = useState([
    { value: 'bank_transfer', label: 'Chuyển khoản ngân hàng' },
    { value: 'credit_card', label: 'Thẻ tín dụng / ghi nợ' },
    { value: 'momo', label: 'Ví MoMo' },
  ]);

  const handleAddNewMethod = () => {
    const newMethod = {
      value: `new_method_${Date.now()}`,
      label: `Phương thức mới (${new Date().toLocaleTimeString()})`
    };
    setPaymentMethods([...paymentMethods, newMethod]);
    setMethod(newMethod.value);
  };

  const validate = () => {
    const num = parseFloat(amount);
    if (!amount || !method) {
      setError('Vui lòng nhập đầy đủ thông tin.');
      return false;
    }
    if (isNaN(num) || num < 10 || num > 50000) {
      setError('Số tiền phải nằm trong khoảng từ 10 đến 50,000 VNĐ.');
      return false;
    }
    setError('');
    return true;
  };

  const handleDeposit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    setShowConfirm(true);
  };

  const confirmDeposit = () => {
    setShowConfirm(false);
    setLoading(true);

    setTimeout(() => {
      const label = paymentMethods.find(m => m.value === method)?.label;
      setMessage(`Yêu cầu nạp ${amount} VNĐ bằng phương thức "${label}" đã được gửi.\nMã giao dịch: tx${Date.now()}`);
      setAmount('');
      setMethod('');
      setLoading(false);
    }, 1000);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
      <div className="bg-white shadow-lg rounded-2xl w-full max-w-md p-6 relative">
        <h2 className="text-2xl font-semibold text-center mb-4 text-gray-800">Nạp tiền</h2>

        {loading && <div className="text-blue-600 mb-4 text-center">Đang xử lý giao dịch...</div>}

        {message && (
          <div className="mb-4 p-3 rounded-md bg-green-100 text-green-700 text-sm whitespace-pre-line">
            {message}
          </div>
        )}

        {error && (
          <div className="mb-4 p-3 rounded-md bg-red-100 text-red-700 text-sm">
            {error}
          </div>
        )}

        {!loading && (
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
              <div className="flex space-x-2 mt-2 text-sm">
                {[100, 500, 1000].map(val => (
                  <button
                    key={val}
                    type="button"
                    className="px-3 py-1 border rounded-xl text-blue-600 hover:bg-blue-50"
                    onClick={() => setAmount(val)}
                  >
                    {val.toLocaleString()} VNĐ
                  </button>
                ))}
              </div>
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
              <button
                type="button"
                onClick={handleAddNewMethod}
                className="text-sm text-blue-600 mt-2 hover:underline"
              >
                + Thêm phương thức mới
              </button>
            </div>

            <div className="text-sm text-gray-500">{processingTime}</div>

            <button
              type="submit"
              className="w-full bg-blue-600 text-white py-2 rounded-xl hover:bg-blue-700 transition duration-200"
            >
              Nạp tiền
            </button>
          </form>
        )}

        {showConfirm && (
          <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center z-10">
            <div className="bg-white p-6 rounded-xl shadow-xl max-w-sm w-full text-center space-y-4">
              <h3 className="text-lg font-semibold">Xác nhận giao dịch</h3>
              <p>Bạn có chắc chắn muốn nạp {amount} VNĐ bằng phương thức:</p>
              <p className="font-medium">{paymentMethods.find(m => m.value === method)?.label}</p>
              <div className="flex justify-center gap-4 mt-4">
                <button
                  onClick={confirmDeposit}
                  className="bg-green-600 text-white px-4 py-2 rounded-xl hover:bg-green-700"
                >
                  Xác nhận
                </button>
                <button
                  onClick={() => setShowConfirm(false)}
                  className="bg-gray-300 text-gray-800 px-4 py-2 rounded-xl hover:bg-gray-400"
                >
                  Hủy
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
