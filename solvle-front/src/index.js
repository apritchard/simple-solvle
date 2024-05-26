import React from 'react';
import '../node_modules/bootstrap/dist/css/bootstrap.min.css';
import { createRoot} from "react-dom/client";
import App from './App';
import reportWebVitals from './reportWebVitals';

const currentAppVersion = "0.0.1";
function manageLocalStorageVersion() {
    const storedVersion = localStorage.getItem('appVersion');

    if (storedVersion !== currentAppVersion) {
        localStorage.clear(); // Clear all local storage data
        localStorage.setItem('appVersion', currentAppVersion); // Set the new version in local storage
        console.log('Local storage was cleared due to app version change.');
    }
}

manageLocalStorageVersion();

const container = document.getElementById('root');
const root = createRoot(container);
root.render(<App />);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
