import { useEffect, useState } from "react";
import api, { errMsg } from "../api.js";

const EMPTY = { name: "", specialization: "General Physician", clinicName: "", address: "", latitude: "", longitude: "", fee: 500, experienceYears: 5, email: "", password: "" };

export default function Admin() {
  const [tab, setTab] = useState("overview");
  const [stats, setStats] = useState(null);
  const [docs, setDocs] = useState([]);
  const [appts, setAppts] = useState([]);
  const [specs, setSpecs] = useState([]);
  const [form, setForm] = useState(null);
  const [err, setErr] = useState("");

  const load = async () => {
    try {
      const [s, d, a, sp] = await Promise.all([
        api.get("/api/admin/stats"), api.get("/api/admin/doctors"), api.get("/api/admin/appointments"), api.get("/api/specializations")]);
      setStats(s.data); setDocs(d.data); setAppts(a.data); setSpecs(sp.data);
    } catch (e) { setErr(errMsg(e)); }
  };
  useEffect(() => { load(); }, []);

  const save = async (e) => {
    e.preventDefault(); setErr("");
    const body = { ...form, latitude: Number(form.latitude), longitude: Number(form.longitude), fee: Number(form.fee), experienceYears: Number(form.experienceYears) };
    try {
      if (form.id) await api.put(`/api/admin/doctors/${form.id}`, body); else await api.post("/api/admin/doctors", body);
      setForm(null); load();
    } catch (e) { setErr(errMsg(e)); }
  };
  const remove = async (d) => {
    if (!window.confirm(`Delete ${d.name}?`)) return;
    setErr("");
    try { await api.delete(`/api/admin/doctors/${d.id}`); load(); } catch (e) { setErr(errMsg(e)); }
  };
  const useMyLocation = () => navigator.geolocation?.getCurrentPosition((p) =>
    setForm((f) => ({ ...f, latitude: p.coords.latitude.toFixed(6), longitude: p.coords.longitude.toFixed(6) })));
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <>
      <div className="tabs">
        {["overview", "doctors", "appointments"].map((t) => (
          <button key={t} className={tab === t ? "on" : ""} onClick={() => setTab(t)}>{t[0].toUpperCase() + t.slice(1)}</button>
        ))}
      </div>
      {err && <div className="alert">{err}</div>}

      {tab === "overview" && stats && (
        <div className="stats">
          <Stat k="Doctors" v={stats.doctors} /><Stat k="Patients" v={stats.patients} />
          <Stat k="Appointments" v={stats.appointments} /><Stat k="Upcoming (confirmed)" v={stats.confirmed} />
          <Stat k="Completed" v={stats.completed} /><Stat k="Cancelled" v={stats.cancelled} />
          <Stat k="Collected (₹)" v={stats.revenue} />
        </div>
      )}

      {tab === "doctors" && (
        <>
          <div className="row between"><h2>Doctors</h2><button className="btn" onClick={() => setForm({ ...EMPTY })}>+ Add doctor</button></div>
          {form && (
            <form className="card" onSubmit={save}>
              <h3>{form.id ? "Edit doctor" : "New doctor"}</h3>
              <div className="form-grid">
                <label>Name<input required value={form.name} onChange={set("name")} placeholder="Dr. Full Name" /></label>
                <label>Specialization
                  <select value={form.specialization} onChange={set("specialization")}>{specs.map((s) => <option key={s}>{s}</option>)}</select></label>
                <label>Clinic / hospital<input required value={form.clinicName} onChange={set("clinicName")} /></label>
                <label>Address<input required value={form.address} onChange={set("address")} /></label>
                <label>Latitude<input required type="number" step="any" value={form.latitude} onChange={set("latitude")} /></label>
                <label>Longitude<input required type="number" step="any" value={form.longitude} onChange={set("longitude")} /></label>
                <label>Fee (₹)<input required type="number" min="0" value={form.fee} onChange={set("fee")} /></label>
                <label>Experience (years)<input required type="number" min="0" value={form.experienceYears} onChange={set("experienceYears")} /></label>
                <label>Login email{form.id && " (leave as is to keep)"}<input type="email" required={!form.id} value={form.email || ""} onChange={set("email")} /></label>
                <label>{form.id ? "New password (optional)" : "Password"}<input type="password" required={!form.id} minLength={form.id ? 0 : 6} value={form.password} onChange={set("password")} /></label>
              </div>
              <p className="muted small">Tip: stand at the clinic and press the button, or right-click the place in Google Maps to copy its coordinates.</p>
              <div className="row gap">
                <button type="button" className="btn ghost" onClick={useMyLocation}>📍 Use my current location</button>
                <button className="btn">Save</button>
                <button type="button" className="btn ghost" onClick={() => setForm(null)}>Cancel</button>
              </div>
            </form>
          )}
          <div className="table-wrap"><table className="table">
            <thead><tr><th>Name</th><th>Specialty</th><th>Clinic</th><th>Fee</th><th>Rating</th><th>Login</th><th></th></tr></thead>
            <tbody>{docs.map((d) => (
              <tr key={d.id}>
                <td><b>{d.name}</b></td><td>{d.specialization}</td><td>{d.clinicName}<div className="muted small">{d.address}</div></td>
                <td>₹{d.fee}</td><td>{d.rating.toFixed(1)} ({d.ratingCount})</td>
                <td>{d.email || <span className="muted">none</span>}</td>
                <td className="nowrap">
                  <button className="btn ghost sm" onClick={() => setForm({ ...d, password: "" })}>Edit</button>{" "}
                  <button className="btn danger sm" onClick={() => remove(d)}>Delete</button>
                </td>
              </tr>))}</tbody>
          </table></div>
        </>
      )}

      {tab === "appointments" && (
        <>
          <h2>Latest appointments</h2>
          <div className="table-wrap"><table className="table">
            <thead><tr><th>Ticket</th><th>Patient</th><th>Doctor</th><th>When</th><th>Amount</th><th>Status</th><th>Payment</th></tr></thead>
            <tbody>{appts.map((a) => (
              <tr key={a.id}>
                <td className="mono">{a.ticketCode}</td><td>{a.patientName}</td><td>{a.doctorName}</td>
                <td>{a.date} {a.slot}</td><td>₹{a.amount}</td>
                <td><span className={`badge ${a.status}`}>{a.status.replace("_", " ")}</span></td><td>{a.paymentStatus}</td>
              </tr>))}</tbody>
          </table></div>
        </>
      )}
    </>
  );
}

const Stat = ({ k, v }) => <div className="card stat"><div className="muted small">{k}</div><div className="stat-v">{v}</div></div>;
