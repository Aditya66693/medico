import { useEffect, useState } from "react";
import api, { errMsg } from "../api.js";
import { useAuth } from "../auth.jsx";

const GENDERS = ["Male", "Female", "Other", "Prefer not to say"];
const BLOOD = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export default function Profile() {
  const { updateUser } = useAuth();
  const [p, setP] = useState(null);
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");
  const [pw, setPw] = useState({ currentPassword: "", newPassword: "" });
  const [pwMsg, setPwMsg] = useState("");
  const [pwErr, setPwErr] = useState("");

  useEffect(() => { api.get("/api/profile").then((r) => setP(r.data)).catch((e) => setErr(errMsg(e))); }, []);
  const set = (k) => (e) => setP({ ...p, [k]: e.target.value });

  const save = async (e) => {
    e.preventDefault(); setErr(""); setMsg("");
    try {
      const { data } = await api.put("/api/profile", { ...p, dateOfBirth: p.dateOfBirth || null });
      setP(data); updateUser({ name: data.name }); setMsg("Profile saved ✓");
    } catch (e) { setErr(errMsg(e)); }
  };

  const changePw = async (e) => {
    e.preventDefault(); setPwErr(""); setPwMsg("");
    try { await api.post("/api/profile/password", pw); setPw({ currentPassword: "", newPassword: "" }); setPwMsg("Password changed ✓"); }
    catch (e) { setPwErr(errMsg(e)); }
  };

  if (!p) return <p>{err || "Loading…"}</p>;
  return (
    <div className="profile">
      <form className="card" onSubmit={save}>
        <div className="row gap">
          <div className="avatar">{p.name[0]?.toUpperCase()}</div>
          <div><h2 style={{ margin: 0 }}>{p.name}</h2><div className="muted">{p.email} · {p.role.toLowerCase()}</div></div>
        </div>
        <div className="form-grid">
          <label>Full name<input required value={p.name} onChange={set("name")} /></label>
          <label>Email (login id, can't be changed)<input value={p.email} readOnly disabled /></label>
          <label>Phone<input value={p.phone || ""} onChange={set("phone")} placeholder="e.g. 9876543210" /></label>
          <label>Gender
            <select value={p.gender || ""} onChange={set("gender")}><option value="">Select…</option>{GENDERS.map((g) => <option key={g}>{g}</option>)}</select></label>
          <label>Date of birth<input type="date" value={p.dateOfBirth || ""} onChange={set("dateOfBirth")} /></label>
          <label>Blood group
            <select value={p.bloodGroup || ""} onChange={set("bloodGroup")}><option value="">Select…</option>{BLOOD.map((b) => <option key={b}>{b}</option>)}</select></label>
          <label>City<input value={p.city || ""} onChange={set("city")} /></label>
        </div>
        {err && <div className="alert">{err}</div>}
        <div className="row gap"><button className="btn">Save changes</button>{msg && <span className="muted">{msg}</span>}</div>
      </form>

      <form className="card" onSubmit={changePw}>
        <h3>Change password</h3>
        <div className="form-grid">
          <label>Current password<input type="password" required value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} /></label>
          <label>New password (min 6)<input type="password" required minLength={6} value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} /></label>
        </div>
        {pwErr && <div className="alert">{pwErr}</div>}
        <div className="row gap"><button className="btn ghost">Update password</button>{pwMsg && <span className="muted">{pwMsg}</span>}</div>
      </form>
    </div>
  );
}
