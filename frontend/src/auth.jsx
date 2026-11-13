import { createContext, useContext, useState } from "react";

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);
export const homeFor = (role) => ({ ADMIN: "/admin", DOCTOR: "/doctor" }[role] || "/");

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("medico_user") || "null"));

  const login = (res) => {
    localStorage.setItem("medico_token", res.token);
    const u = { id: res.id, name: res.name, email: res.email, role: res.role };
    localStorage.setItem("medico_user", JSON.stringify(u));
    setUser(u);
  };
  const logout = () => {
    localStorage.removeItem("medico_token");
    localStorage.removeItem("medico_user");
    setUser(null);
  };
  const updateUser = (patch) => {
    const u = { ...user, ...patch };
    localStorage.setItem("medico_user", JSON.stringify(u));
    setUser(u);
  };
  return <Ctx.Provider value={{ user, login, logout, updateUser }}>{children}</Ctx.Provider>;
}
