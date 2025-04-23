import axios from 'axios';
import React, { useEffect, useState } from 'react';

const Withdraw = () => {
  const [amount, setAmount] = useState('');
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [selectedMethod, setSelectedMethod] = useState('');
  const [balance, setBalance] = useState(null);
  const [reason, setReason] = useState('');
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    axios.get('/accounts/api/v1/accounts/acc123456')
      .then(res => setBalance(res.data.data.balance))
      .catch(() => setError('Không thể lấy số dư tài khoản.'));

    axios.get('/accounts/api/v1/payment-methods?status=ACTIVE&type=BANK_ACCOUNT,DEBIT_CARD')
      .then(res => setPaymentMethods(res.data.data))
      .catch(() => setError('Không thể tải phương thức thanh toán.'));
  }, []);

  const handleWithdraw = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    if (!amount || Number(amount) < 1 || Number(amount) > balance?.available) {
      setError('Số tiền phải từ 1.00 đến số dư khả dụng.');
      return;
    }

    if (!selectedMethod) {
      setError('Vui lòng chọn phương thức thanh toán.');
      return;
    }

    if (!termsAccepted) {
      setError('Bạn phải đồng ý với điều khoản rút tiền.');
      return;
    }

    try {
      const verifyRes = await axios.post('/users/api/v1/users/me/2fa/generate');
      const verificationId = verifyRes.data.data.verificationId;
      const verificationCode = prompt(`Nhập mã xác minh được gửi đến ${verifyRes.data.data.maskedDestination}`);

      await axios.post('/users/api/v1/users/me/2fa/verify', {
        verificationId,
        verificationCode,
      });

      const withdrawalRes = await axios.post(`/accounts/api/v1/accounts/acc123456/transactions/withdrawal`, {
        amount: parseFloat(amount),
        currency: 'USD',
        paymentMethodId: selectedMethod,
        description: reason,
        verificationId,
        verificationCode,
      });

      setMessage(`Yêu cầu rút tiền đã được gửi. Mã giao dịch: ${withdrawalRes.data.data.transactionId}`);
      setAmount('');
      setSelectedMethod('');
      setReason('');
      setTermsAccepted(false);
    } catch (err) {
      setError('Có lỗi xảy ra khi xử lý yêu cầu rút tiền.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-black to-zinc-900 text-white flex items-center justify-center px-4 py-12">
      <div className="bg-zinc-800 bg-opacity-80 backdrop-blur-md rounded-2xl shadow-lg p-8 w-full max-w-lg space-y-6 border border-zinc-700">
        <h2 className="text-3xl font-bold text-center text-white">Rút tiền</h2>

        {balance && (
          <p className="text-center text-sm text-zinc-300">
            Số dư khả dụng: <span className="text-emerald-400 font-medium">${balance.available}</span>
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

        <form onSubmit={handleWithdraw} className="space-y-5">
          <div>
            <label className="block text-sm font-medium mb-1">Số tiền (USD)</label>
            <input
              type="number"
              min="1"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="Nhập số tiền cần rút"
              className="w-full px-4 py-2 bg-zinc-900 text-white border border-zinc-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Phương thức thanh toán</label>
            <select
              value={selectedMethod}
              onChange={(e) => setSelectedMethod(e.target.value)}
              className="w-full px-4 py-2 bg-zinc-900 text-white border border-zinc-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Chọn phương thức --</option>
              {paymentMethods.map(method => (
                <option key={method.id} value={method.id}>
                  {method.type} - ****{method.last4}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="mt-2 text-sm text-blue-400 hover:underline hover:text-blue-300"
            >
              + Thêm phương thức mới
            </button>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Lý do rút tiền (tuỳ chọn)</label>
            <select
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full px-4 py-2 bg-zinc-900 text-white border border-zinc-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Chọn lý do --</option>
              <option value="investment">Đầu tư</option>
              <option value="personal_use">Chi tiêu cá nhân</option>
              <option value="other">Khác</option>
            </select>
          </div>

          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={termsAccepted}
              onChange={() => setTermsAccepted(!termsAccepted)}
              className="form-checkbox h-4 w-4 text-blue-500 border-gray-500 rounded focus:ring-blue-500"
            />
            <label className="text-sm">
              Tôi đồng ý với{' '}
              <a href="#" className="text-blue-400 underline hover:text-blue-300">
                điều khoản rút tiền
              </a>
            </label>
          </div>

          <button
            type="submit"
            className="w-full bg-blue-600 text-white font-semibold py-2 rounded-xl hover:bg-blue-700 transition"
          >
            Rút tiền
          </button>
        </form>
      </div>
    </div>
  );
};

export default Withdraw;
