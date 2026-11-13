import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import api, { errMsg } from "../api.js";
import { homeFor, useAuth } from "../auth.jsx";

export default function Auth() {
  const [mode, setMode] = useState("login");
  const [f, setF] = useState({ name: "", email: "", password: "", phone: "" });
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const { login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setErr(""); setBusy(true);
    try {
      const { data } = await api.post(`/api/auth/${mode}`, mode === "login" ? { email: f.email, password: f.password } : f);
      login(data);
      nav(data.role === "PATIENT" ? loc.state?.from || "/" : homeFor(data.role), { replace: true });
    } catch (e) { setErr(errMsg(e)); }
    setBusy(false);
  };

  return (
    <div className="auth-wrap">
      <form className="card auth" onSubmit={submit}>
        <h2>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
        <p className="muted">{mode === "login" ? "Log in to book your appointment." : "Register as a patient in under a minute."}</p>
        {mode === "register" && (
          <>
            <input placeholder="Full name" value={f.name} onChange={set("name")} required />
            <input placeholder="Phone (optional)" value={f.phone} onChange={set("phone")} />
          </>
        )}
        <input type="email" placeholder="Email" value={f.email} onChange={set("email")} required />
        <input type="password" placeholder="Password (min 6 chars)" value={f.password} onChange={set("password")} required />
        {err && <div className="alert">{err}</div>}
        <button className="btn big" disabled={busy}>{busy ? "Please wait…" : mode === "login" ? "Login" : "Register"}</button>
        <p className="muted center">
          {mode === "login" ? "New here? " : "Already registered? "}
          <a href="#" onClick={(e) => { e.preventDefault(); setMode(mode === "login" ? "register" : "login"); setErr(""); }}>
            {mode === "login" ? "Create an account" : "Login"}
          </a>
        </p>
      </form>
    </div>
  );
}
