import "./PaymentMethodsManagement.css";
import {Breadcrumb} from "antd";

const PaymentMethodsManagement = () => {
    return (
        <div className="container payment-method-management-container">
            <Breadcrumb
                className="breadcrumb"
                separator=">"
                items={[
                    {
                        title: 'Account dashboard',
                        href: '/account-dashboard'
                    },
                    {
                        title: 'Payment methods',
                        href: '/account-dashboard/payment-methods'
                    }
                ]}
            />
            <div className="title">
                <p className="name">Payment methods</p>
                <p className="description">Display your payment methods, in summary and details, quickly add new payment methods, as well as edit, verify, or remove them </p>
            </div>
            <div className="body">
                <PaymentMethodCategory
                    name="Bank accounts"
                    items={[
                        {
                            id: "67efe208e5ef12698df2070d",
                            nickname: "Hung's Updated Bank Account",
                            imageSrc: "../../../src/assets/atm-card.png",
                            status: "Active",
                            maskedNumber: "*****4321",
                            isDefault: true,
                            addedAt: "2025-04-04T13:43:36.182Z",
                            lastUsedAt: "2025-04-04T13:43:36.182Z",
                            metadata: {
                                accountHolderName: "HungSenahihi",
                                accountNumber: "987654321",
                                bankName: "MB",
                                routingNumber: "123456789",
                                verificationMethod: "MICRO_DEPOSITS",
                                verificationRequired: true,
                                verifiedAt: null
                            }
                        }
                    ]}
                />
            </div>
        </div>
    );
};

const PaymentMethodCategory = (props) => {
    return (
        <div className="pm-category-container">
            <p className="category-name">{props.name}</p>
            {props.items.map((item) => {
                return (
                    <div className="category-wrapper">
                        <div className="summary">
                            <img className="expand-icon" src="../../../src/assets/right-arrow.png" alt="expand icon" />
                            <img className="item-icon" src={item.imageSrc} alt="item icon" />
                            <div className="item-info">
                                <p className="item-name">{item.nickname}</p>
                                <p className="item-id">{item.id}</p>
                            </div>
                            <div className="item-status">
                                <img src="../../../src/assets/tick.png" alt="status icon" />
                                <p className="status">{item.status}</p>
                            </div>
                        </div>

                        <div className="details hidden">
                            <div className="info">
                                <p>Details</p>
                                <div className="properties">
                                    <div className="property">
                                        <p className="label">Masked number</p>
                                        <p className="description">{item.maskedNumber}</p>
                                    </div>
                                    <div className="property">
                                        <p className="label">Use as default</p>
                                        <p className="description">{item.isDefault}</p>
                                    </div>
                                    <div className="property">
                                        <p className="label">Added at</p>
                                        <p className="description">{item.addedAt}</p>
                                    </div>
                                    <div className="property">
                                        <p className="label">Last used at</p>
                                        <p className="description">{item.lastUsedAt}</p>
                                    </div>
                                    <div className="property metadata">
                                        <p className="label">Metadata</p>
                                        <button className="expand-btn"><img src="../../../src/assets/right-arrow.png" alt="expand icon" /></button>
                                        {Object.entries(item.metadata).map((pair) => {
                                            return (
                                                <div className="metadata-property-wrapper hidden">
                                                    <p className="label">{pair[0]}</p>
                                                    <p className="description">{pair[1]}</p>
                                                </div>
                                            )
                                        })}
                                    </div>
                                </div>
                            </div>
                            <div className="crud-btns">
                                <button className="update-btn" >Update</button>
                                <button className="delete-btn">Delete</button>
                            </div>
                        </div>
                    </div>
                )
            })}
        </div>
    );
};

export default PaymentMethodsManagement;
