// In a file like utils/auth.js
import { jwtDecode } from 'jwt-decode'; // Change this line

export const getUserIdFromToken = () => {
    const token = localStorage.getItem("token");
    if (!token) return null;

    try {
        // The rest of your code remains the same
        const decoded = jwtDecode(token);
        return decoded.sub || decoded.id || decoded.userId;
    } catch (error) {
        console.error("Failed to decode token:", error);
        return null;
    }
};