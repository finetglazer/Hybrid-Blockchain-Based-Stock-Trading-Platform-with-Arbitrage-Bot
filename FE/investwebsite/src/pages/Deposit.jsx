import React, { useState } from 'react';

const Deposit = () => {
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [processingTime] = useState('Estimated processing time: 5 minutes');
  const [paymentMethods, setPaymentMethods] = useState([
    { value: 'bank_transfer', label: 'Bank Transfer' },
    { value: 'credit_card', label: 'Credit/Debit Card' },
    { value: 'momo', label: 'MoMo Wallet' },
  ]);

  const handleAddNewMethod = () => {
    const newMethod = {
      value: `new_method_${Date.now()}`,
      label: `New Method (${new Date().toLocaleTimeString()})`
    };
    setPaymentMethods([...paymentMethods, newMethod]);
    setMethod(newMethod.value);
  };

  const validate = () => {
    const num = parseFloat(amount);
    if (!amount || !method) {
      setError('Please fill in all fields.');
      return false;
    }
    if (isNaN(num) || num < 10 || num > 50000) {
      setError('Amount must be between $10 and $50,000.');
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

  const confirmDeposit = async () => {
    setShowConfirm(false);
    setLoading(true);
    try {
      const response = await fetch('/sagas/api/v1/deposit/start', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          amount: parseFloat(amount),
          method,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'An error occurred while processing the deposit.');
      }

      const label = paymentMethods.find(m => m.value === method)?.label;
      setMessage(`Your deposit request of $${amount} via "${label}" has been submitted.\nTransaction ID: ${data.transactionId || `tx${Date.now()}`}`);
      setAmount('');
      setMethod('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-100 to-blue-200 p-4">
      <div className="bg-white shadow-xl rounded-2xl w-full max-w-md p-6 relative">
        <h2 className="text-3xl font-bold text-center mb-6 text-gray-800">Deposit</h2>

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

        {!loading && (
          <form onSubmit={handleDeposit} className="space-y-5">
            <div>
              <label className="block text-gray-700 font-medium mb-1">Amount (USD)</label>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="Enter deposit amount"
                className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <div className="flex flex-wrap gap-2 mt-2 text-sm">
                {[10, 50, 100].map(val => (
                  <button
                    key={val}
                    type="button"
                    className="px-3 py-1 border rounded-xl text-blue-600 hover:bg-blue-50 transition"
                    onClick={() => setAmount(val)}
                  >
                    ${val}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-gray-700 font-medium mb-1">Payment Method</label>
              <select
                value={method}
                onChange={(e) => setMethod(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">-- Select Method --</option>
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
                + Add new method
              </button>
            </div>

            <div className="text-sm text-gray-500 italic text-center">{processingTime}</div>

            <button
              type="submit"
              className="w-full bg-blue-600 text-white py-2 rounded-xl hover:bg-blue-700 transition font-medium"
            >
              Deposit
            </button>
          </form>
        )}

        {showConfirm && (
          <div className="absolute inset-0 bg-black bg-opacity-40 flex items-center justify-center z-20">
            <div className="bg-white p-6 rounded-2xl shadow-2xl max-w-sm w-full text-center space-y-4">
              <h3 className="text-xl font-semibold text-gray-800">Confirm Transaction</h3>
              <p className="text-sm">Are you sure you want to deposit <strong>${amount}</strong> using:</p>
              <p className="font-medium text-blue-700">{paymentMethods.find(m => m.value === method)?.label}</p>
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
