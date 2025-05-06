import React from "react";
import "./TransactionHistoryHeader.css"; // nếu bạn có style riêng

const TransactionHistory = ({ transactionHistory }) => {
  return (
    <div className="transaction-history">
      <h3>Transaction History</h3>
      <ul>
        {transactionHistory.map((transaction) => (
          <li key={transaction.id}>
            <p>
              <strong>Date:</strong> {transaction.date}
            </p>
            <p>
              <strong>Description:</strong> {transaction.description}
            </p>
            <p>
              <strong>Amount:</strong> {transaction.amount}
            </p>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default TransactionHistory;
