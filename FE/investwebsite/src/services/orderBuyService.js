// services/orderBuyService.js
import axios from 'axios';

/**
 * Submit a buy order to the order saga service
 * @param {Object} orderData - The order data to submit
 * @returns {Promise<Object>} - The response from the saga service
 */
export const submitOrder = async (orderData) => {
    const token = localStorage.getItem("token");
    try {
        // Use relative path without leading slash
        const response = await axios.post("/sagas/api/v1/orders/buy", orderData, {
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json",
            }
        });
        return response.data;
    } catch (error) {
        console.error("Error submitting order:", error);
        throw error;
    }
};

/**
 * Get the status of an order saga
 * @param {string} sagaId - The ID of the saga to check
 * @returns {Promise<Object>} - The saga status data
 */
export const getOrderStatus = async (sagaId) => {
    const token = localStorage.getItem("token");
    try {
        const response = await axios.get(`/sagas/api/v1/orders/${sagaId}`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        return response.data;
    } catch (error) {
        console.error("Error fetching order status:", error);
        throw error;
    }
};

/**
 * Cancel an order by user request
 * @param {string} sagaId - The ID of the saga to cancel
 * @returns {Promise<Object>} - The response from the saga service
 */
export const cancelOrder = async (sagaId) => {
    const token = localStorage.getItem("token");
    try {
        const response = await axios.post(`/sagas/api/v1/orders/${sagaId}/cancel`, {}, {
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json",
            }
        });
        return response.data;
    } catch (error) {
        console.error("Error cancelling order:", error);
        throw error;
    }
};
