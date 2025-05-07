import { Alert, Breadcrumb, notification } from "antd";
import axios from "axios";
import React, { useEffect, useState } from "react";
import ReactDOM from "react-dom/client";
import { InfoOutlined, WarningOutlined } from "@ant-design/icons";
import "./PaymentMethodsManagement.css";
import AddPaymentMethod from "./forms/AddPaymentMethod.jsx";
import VerifyPaymentMethod from "./forms/VerifyPaymentMethod.jsx";
import UpdatePaymentMethod from "./forms/UpdatePaymentMethod.jsx";

const PaymentMethodsManagement = () => {
  const [api, contextHolder] = notification.useNotification();

  const [bankAccounts, setBankAccounts] = useState([]);
  const [creditCards, setCreditCards] = useState([]);
  const [debitCards, setDebitCards] = useState([]);
  const [digitalWallets, setDigitalWallets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [openAddForm, setOpenAddForm] = useState(false);
  const [openVerifyForm, setOpenVerifyForm] = useState(false);
  const [openUpdateForm, setOpenUpdateForm] = useState(false);
  const [itemBeingVerified, setItemBeingVerified] = useState();
  const [itemBeingUpdated, setItemBeingUpdated] = useState();

  const onClickCategorySummary = (
    className,
    item,
    metadataClassNamePrefix,
    index
  ) => {
    const expandIcon = document
      .querySelector(`.${className}`)
      .querySelector(".expand-icon");
    expandIcon.classList.toggle("expand");

    const categoryWrapper = document.querySelector(`.${className}`);
    categoryWrapper.classList.toggle("details-show");
    if (!categoryWrapper.classList.contains("details-show")) {
      const itemDetails = categoryWrapper.querySelector(".item-details");
      itemDetails.remove();
    }
    else {
      const container = document.createElement("div");
      container.classList.add("item-details");
      const root = ReactDOM.createRoot(container);
      root.render(
        <ItemDetails
          item={item}
          metadataClassNamePrefix={metadataClassNamePrefix}
          index={index}
          onClickActivateBtn={onClickActivateBtn}
          onClickDeactivateBtn={onClickDeactivateBtn}
          onClickVerifyBtn={onClickVerifyBtn}
          onClickUpdateBtn={onClickUpdateBtn}
          onClickDeleteBtn={onClickDeleteBtn}
          onClickMetadata={onClickMetadata}
        />
      );
      categoryWrapper.appendChild(container);
      console.log(container.innerHTML);
    }
  };

  const onClickMetadata = (className, metadata) => {
    const expandIcon = document
      .querySelector(`.${className}`)
      .querySelector(".expand-icon");
    expandIcon.classList.toggle("metadata-expand");
    const metadataWrapper = document.querySelector(`.${className}`);
    metadataWrapper.classList.toggle("details-show");

    if (metadataWrapper.classList.contains("details-show")) {
      const container = document.createElement("div");
      container.classList.add("metadata-details");
      const root = ReactDOM.createRoot(container);
      root.render(<MetadataDetails metadata={metadata} />);
      metadataWrapper.appendChild(container);
    } else {
      const metadataDetails =
        metadataWrapper.querySelector(".metadata-details");
      metadataDetails.remove();
    }
  };

    const fetchPaymentMethods = async () => {
        const token = localStorage.getItem("token");
        try {
            const response = await axios.get("/accounts/payment-methods/api/v1/me/get", {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            });
            setLoading(false);
            if (response.data && response.data.status === 1) {
                const paymentMethods = response.data.data.items;
                let fetchedBankAccounts = [];
                let fetchedCreditCards = [];
                let fetchedDebitCards = [];
                let fetchedDigitalWallets = [];
                paymentMethods.forEach((paymentMethod) => {
                    switch (paymentMethod.type) {
                        case "BANK_ACCOUNT":
                            fetchedBankAccounts.push(Object.assign(paymentMethod, {imageSrc: "../../../src/assets/atm-card.png"}));
                            break;
                        case "CREDIT_CARD":
                            fetchedCreditCards.push(Object.assign(paymentMethod, {imageSrc: "../../../src/assets/credit-card.png"}));
                            break;
                        case "DEBIT_CARD":
                            fetchedDebitCards.push(Object.assign(paymentMethod, {imageSrc: "../../../src/assets/debit-card.png"}));
                            break;
                        case "DIGITAL_WALLET":
                            fetchedDigitalWallets.push(Object.assign(paymentMethod, {imageSrc: "../../../src/assets/digital-wallet.png"}));
                            break;
                    }
                });
                setBankAccounts(fetchedBankAccounts);
                setCreditCards(fetchedCreditCards);
                setDebitCards(fetchedDebitCards);
                setDigitalWallets(fetchedDigitalWallets);

            } else {
                setError(response.data);
            }
        } catch (e) {
            setLoading(false);
            setError(e);
        }
    }

    const onClickAddBtn = () => {
        setOpenAddForm(true);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.add("blurred");
    };

    const onSuccessfullyAdd = () => {
        setOpenAddForm(false);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.remove("blurred");
        setLoading(true);
        fetchPaymentMethods().then(() => {});
    };

    const onCancelAdd = () => {
        setOpenAddForm(false);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.remove("blurred");
    };

    const onClickActivateBtn = async (item) => {
        if (item.status === "VERIFICATION_PENDING") {
            openNotification("Warning", "You need to verify your payment method first!", <WarningOutlined/>);
            return;
        }
        setLoading(true);
        const token = localStorage.getItem("token");
        try {
            const response = await axios.put(`/accounts/payment-methods/api/v1/${item.id}/update`, {
                nickname: item.nickname,
                setAsDefault: item.setAsDefault,
                status: "ACTIVE",
                metadata: {
                    accountHolderName: item.accountHolderName,
                }
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            });
            if (response.data && response.data.status === 1) {
                setLoading(false);
                fetchPaymentMethods().then(() => {});
                openNotification("Success", "Your payment method activated successfully!", <InfoOutlined/>)
            }
            else {
                setError(response.data.msg);
            }
        }
        catch (e) {
            setLoading(false);
            setError(e.message);
        }
    };

    const onClickDeactivateBtn = async (item) => {
        if (item.status === "VERIFICATION_PENDING") {
            openNotification("Warning", "You need to verify your payment method first!", <WarningOutlined/>);
            return;
        }
        setLoading(true);
        const token = localStorage.getItem("token");
        try {
            const response = await axios.put(`/accounts/payment-methods/api/v1/${item.id}/update`, {
                nickname: item.nickname,
                setAsDefault: item.setAsDefault,
                status: "INACTIVE",
                metadata: {
                    accountHolderName: item.accountHolderName,
                }
            }, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            });
            if (response.data && response.data.status === 1) {
                setLoading(false);
                fetchPaymentMethods().then(() => {});
                openNotification("Success", "Your payment method deactivated successfully!", <InfoOutlined/>)
            }
            else {
                setError(response.data.msg);
            }
        }
        catch (e) {
            setLoading(false);
            setError(e.message);
        }
    };

    const onClickUpdateBtn = (item) => {
        setOpenUpdateForm(true);
        setItemBeingUpdated(item);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.add("blurred");
    };

    const onCancelUpdate = () => {
        setOpenUpdateForm(false);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.remove("blurred");
    }

    const onSuccessfullyUpdate = () => {
        setOpenUpdateForm(false);
        openNotification("Success", "Your payment method updated successfully!", <InfoOutlined />);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.remove("blurred");
        setLoading(true);
        fetchPaymentMethods().then(() => {});
    };

    const onClickVerifyBtn = (item) => {
        setOpenVerifyForm(true);
        setItemBeingVerified(item);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.add("blurred");
    };

    const onCancelVerify = () => {
        setOpenVerifyForm(false);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.remove("blurred");
    }

    const onSuccessfullyVerify = () => {
        setOpenVerifyForm(false);
        openNotification("Success", "Your payment method verified successfully!", <InfoOutlined />);
        const body = document.querySelector(".payment-method-management-container").querySelector(".body");
        body.classList.remove("blurred");
        setLoading(true);
        fetchPaymentMethods().then(() => {});
    };

    const onClickDeleteBtn = async (item) => {
        const token = localStorage.getItem("token");
        try {
            const response = await axios.delete(`/accounts/payment-methods/api/v1/${item.id}/delete`, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                }
            });
            setLoading(false);
            if (response.data && response.data.status === 1) {
                setLoading(true);
                fetchPaymentMethods().then(() => {});
                openNotification("Success", "Payment method has been removed!", <InfoOutlined/>)
            }
            else {
                setError(response.data.msg);
            }
        }
        catch (e) {
            setLoading(false);
            setError(e.message);
        }
    };

    const openNotification = (title, description, icon) => {
        api.open({
            message: title,
            description: description,
            icon: icon
        })
    };

    useEffect(() => {
        fetchPaymentMethods().then(() => {});
    }, []);

  return (
  <>
      {openVerifyForm &&
          <VerifyPaymentMethod
              onSuccess={onSuccessfullyVerify}
              onCancel={onCancelVerify}
              item={itemBeingVerified}
          />
      }
      {openUpdateForm &&
           <UpdatePaymentMethod
              onSuccess={onSuccessfullyUpdate}
              onCancel={onCancelUpdate}
              item={itemBeingUpdated}
           />
      }
    {contextHolder}
    <div className="container payment-method-management-container">
      <Breadcrumb
        className="breadcrumb"
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
                Payment methods
              </span>
            ),
            href: "/home/payment-methods",
          },
        ]}
      />
      <div className="title">
        <p className="name">Payment methods</p>
        <p className="description">
          Display your payment methods, in summary and details, quickly add new
          payment methods, as well as edit, verify, or remove them{" "}
        </p>
      </div>
      {error && (
        <Alert
          message="Error"
          description={error.message}
          type="error"
          showIcon
        />
      )}
      {openAddForm &&
          <AddPaymentMethod
              onSuccess={onSuccessfullyAdd}
              onCancel={onCancelAdd}
          />
      }

      <div className="body">
          <button className="add-payment-method-btn" onClick={onClickAddBtn}>Add</button>
        {loading && (
          <div className="loading-wrapper">
            <p>Fetching your payment methods </p>
            <div className="spinner" />
          </div>
        )}
        {!loading &&
          !error &&
          !bankAccounts.length &&
          !creditCards.length &&
          !debitCards.length &&
          !digitalWallets.length && <p>No payment methods found</p>}
        {!loading && !error && bankAccounts.length ? (
          <PaymentMethodCategory
            name="Bank accounts"
            items={bankAccounts}
            onClick={onClickCategorySummary}
          />
        ) : null}
        {(!loading && !error && creditCards.length) ? (
          <PaymentMethodCategory
            name="Credit cards"
            items={creditCards}
            onClick={onClickCategorySummary}
          />
        ) : null}
        {!loading && !error && debitCards.length ? (
          <PaymentMethodCategory
            name="Debit cards"
            items={debitCards}
            onClick={onClickCategorySummary}
          />
        ) : null}
        {!loading && !error && digitalWallets.length ? (
          <PaymentMethodCategory
            name="Digital wallets"
            items={digitalWallets}
            onClick={onClickCategorySummary}
          />
        ) : null}
      </div>
    </div>
  </>
  );
};

const PaymentMethodCategory = (props) => {
  return (
    <>
      <div className="pm-category-container">
        <p className="category-name">{props.name}</p>
        {props.items.map((item, index) => {
          const t = props.name.toLowerCase().split(/\s+/);
          let prefix = "";
          t.map((str) => (prefix += str.charAt(0)));
          return (
            <div className={"category-wrapper " + `${prefix}-${index}`}>
              <div
                className="summary"
                onClick={() =>
                  props.onClick(`${prefix}-${index}`, item, prefix, index)
                }
              >
                <img
                  className="expand-icon"
                  src="../../../src/assets/right-arrow.png"
                  alt="expand icon"
                />
                <img
                  className="item-icon"
                  src={item.imageSrc}
                  alt="item icon"
                />
                <div className="item-info">
                  <p className="item-name">{item.nickname}</p>
                  <p className="item-id">{item.id}</p>
                </div>
                <div className="item-status">
                  {item.status === "ACTIVE" && (
                    <img src="../../../src/assets/tick.png" alt="status icon" />
                  )}
                  {item.status === "INACTIVE" && (
                    <img src="../../../src/assets/x.png" alt="status icon" />
                  )}
                  {item.status === "VERIFICATION_PENDING" && (
                    <img
                      src="../../../src/assets/pending.png"
                      alt="status icon"
                    />
                  )}
                  <p
                    className="status"
                    style={{
                      color:
                        item.status === "ACTIVE"
                          ? "springgreen"
                          : item.status === "INACTIVE"
                          ? "red"
                          : "gray",
                    }}
                  >
                    {item.status}
                  </p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
};

const ItemDetails = ({
  item,
  metadataClassNamePrefix,
  index,
  onClickActivateBtn,
  onClickDeactivateBtn,
  onClickVerifyBtn,
  onClickUpdateBtn,
  onClickDeleteBtn,
  onClickMetadata,
}) => {
  return (
    <>
      <div className="details-info">
        <p>DETAILS</p>
        <div className="properties">
          <div className="property">
            <p className="label">Masked number</p>
            <p className="description">{item.maskedNumber}</p>
          </div>
          <div className="property">
            <p className="label">Use as default</p>
            <p className="description">{item.default.toString()}</p>
          </div>
          <div className="property">
            <p className="label">Added at</p>
            <p className="description">{item.addedAt}</p>
          </div>
          <div className="property">
            <p className="label">Last used at</p>
            <p className="description">{item.lastUsedAt}</p>
          </div>
          <div
            className={`property metadata ${metadataClassNamePrefix}-metadata-wrapper-${index}`}
          >
            <div
              className="title"
              onClick={() =>
                onClickMetadata(
                  `${metadataClassNamePrefix}-metadata-wrapper-${index}`,
                  item.metadata
                )
              }
            >
            <div className="flex">
                <span className="label mt-5">Metadata</span>
                <img className="expand-icon"
                    src="../../../src/assets/right-arrow.png"
                    alt="expand icon"
                />
            </div>
            </div>
            {/* This is the position of metadata details and would be inserted using JS */}
          </div>
        </div>
      </div>
      <div className="crud-btns">
        <div className="btn btn-wrapper">
          <p className="description">Activate this payment method</p>
          <button
            className="activate-btn"
            onClick={() => onClickActivateBtn(item)}
          >
            Activate
          </button>
        </div>
        <div className="btn-wrapper">
          <p className="description">Deactivate this payment method</p>
          <button
            className="deactivate-btn"
            onClick={() => onClickDeactivateBtn(item)}
          >
            Deactivate
          </button>
        </div>
        <div className="btn-wrapper">
          <p className="description">Verify this payment method</p>
          <button className="verify-btn" onClick={() => onClickVerifyBtn(item)}>
            Verify
          </button>
        </div>
        <div className="btn-wrapper">
          <p className="description">
            Update information about this payment method
          </p>
          <button className="update-btn" onClick={() => onClickUpdateBtn(item)}>
            Update
          </button>
        </div>
        <div className="btn-wrapper">
          <p className="description warning-text">Remove this payment method</p>
          <button className="delete-btn" onClick={() => onClickDeleteBtn(item)}>
            Delete
          </button>
        </div>
      </div>
    </>
  );
};

const MetadataDetails = ({ metadata }) => {
  return (
    <div className="metadata-details">
      {Object.entries(metadata).map((pair) => {
        return (
          <div className="property metadata-property-wrapper">
            <p className="label">{pair[0]}</p>
            <p className="description">{pair[1]}</p>
          </div>
        );
      })}
    </div>
  );
};

export default PaymentMethodsManagement;
