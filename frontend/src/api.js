import axios from "axios";

// Empty baseURL in dev -> Vite proxy forwards /api to localhost:8080. In prod set VITE_API_URL.
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || "" });

api.interceptors.request.use((cfg) => {
  const t = localStorage.getItem("medico_token");
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401 && localStorage.getItem("medico_token")) {
      localStorage.removeItem("medico_token");
      localStorage.removeItem("medico_user");
      window.location.href = "/login";
    }
    return Promise.reject(e);
  }
);

export const errMsg = (e) =>
  e.response?.data?.message || e.response?.data?.error || e.message || "Something went wrong";

export default api;
