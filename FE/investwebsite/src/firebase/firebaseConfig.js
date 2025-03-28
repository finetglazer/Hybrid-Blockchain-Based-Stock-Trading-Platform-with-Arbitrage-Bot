// src/firebase/firebaseConfig.js
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getAuth } from "firebase/auth"; // Add this import

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
    apiKey: "AIzaSyD2TSO3-0HgW6e91ceKLw5UGKIxuNt7AkY",
    authDomain: "trading-simulator-30378.firebaseapp.com",
    projectId: "trading-simulator-30378",
    storageBucket: "trading-simulator-30378.firebasestorage.app",
    messagingSenderId: "713467160948",
    appId: "1:713467160948:web:bf3d5df0dbe72889f2a421",
    measurementId: "G-1BESNPCQ8G"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
const auth = getAuth(app); // Initialize auth

export { auth }; // Export the auth object