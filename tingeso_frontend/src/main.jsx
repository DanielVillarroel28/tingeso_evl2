// src/main.jsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "./services/keycloak";

const eventLogger = (event, error) => {
  console.log('🧩 [Keycloak event]', event, error);
};

const tokenLogger = (tokens) => {
  console.log('🔐 [Keycloak tokens]', tokens);
  localStorage.setItem("token", tokens.token);
};

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{ 
        onLoad: 'login-required',
        checkLoginIframe: false  
      }}
      onEvent={eventLogger}
      onTokens={tokenLogger}
    >
      <App />
    </ReactKeycloakProvider>
  </StrictMode>
);