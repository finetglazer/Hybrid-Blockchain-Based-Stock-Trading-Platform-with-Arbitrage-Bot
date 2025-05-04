import {Breadcrumb} from "antd";
import React from "react";

const OrderViewHistory = () => {
    return (
    <>
      <div className="container font-opensans min-w-[100vw] min-h-[100vh]">
          <Breadcrumb
             className=""
             separator=" "
             items={[
                 {
                     title: (
                         <span style={{ color: "rgba(255, 255, 255, 0.6)" }}>
                            Home
                         </span>
                     ),
                     href: "/home",
                 },
                 {
                     title: (
                         <span style={{ color: "rgba(255, 255, 255, 0.6)" }}> {">"} </span>
                     ),
                 },
                 {
                     title: (
                         <span style={{ color: "rgba(255, 255, 255, 1)" }}>
                            Order history
                         </span>
                     ),
                     href: "/home/order-history",
                 },
             ]}
          />
          <div className="mt-[50px]">
              <p className="text-4xl">Order History</p>
              <p>A centralized location where users can track, review, and analyze all their past trading activity</p>
          </div>

      </div>
    </>
    );
};

export default OrderViewHistory;
