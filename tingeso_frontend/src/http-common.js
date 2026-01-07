import axios from "axios";
import keycloak from "./services/keycloak";

const apiServer = import.meta.env.VITE_API_SERVER
const apiPort = import.meta.env.VITE_API_PORT;

console.log(apiServer)
console.log(apiPort)

const api = axios.create({
  baseURL: `http://${apiServer}:${apiPort}`,
  headers: {
    "Content-Type": "application/json"
  },
  withCredentials: false,
});

api.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    await keycloak.updateToken(30);
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default api;