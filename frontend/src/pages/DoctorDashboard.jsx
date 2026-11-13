import { useEffect, useMemo, useState } from "react";
import api, { errMsg } from "../api.js";

const today = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);

export default function DoctorDashboard() {
  const [tab, setTab] = useState("appointments");
  const [list, setList] = useState([]);
  const [filter, setFilter] = useState("upcoming");
  const [me, setMe] = useState(null);
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");

  const load = async () => {
    try {
      const [a, m] = await Promise.all([api.get("/api/doctor/appointments"), api.get("/api/doctor/me")]);
      setList(a.data); setMe(m.data);
    } catch (e) { setErr(errMsg(e)); }
  };
  useEffect(() => { load(); }, []);

  const shown = useMemo(() => {
    const t = today();
    const rows = list.filter((a) => filter === "all" ? true : filter === "today" ? a.date === t && a.status !== "CANCELLED"
      : a.status === "CONFIRMED" && a.date >= t);
    return [...rows].sort((x, y) => (filter === "all" ? -1 : 1) * (x.date + x.slot).localeCompare(y.date + y.slot));
  }, [list, filter]);

  const act = async (a, action) => {
    if (action === "cancel" && !window.confirm("Cancel this appointment? The patient will be refunded.")) return;
    setErr("");
    try { await api.post(`/api/doctor/appointments/${a.id}/${action}`); load(); } catch (e) { setErr(errMsg(e)); }
  };

  const saveProfile = async (e) => {
    e.preventDefault(); setErr(""); setMsg("");
    try {
      const b = { clinicName: me.clinicName, address: me.address, latitude: Number(me.latitude), longitude: Number(me.longitude), fee: Number(me.fee), experienceYears: Number(me.experienceYears) };
      setMe((await api.put("/api/doctor/me", b)).data); setMsg("Profile saved");
    } catch (e) { setErr(errMsg(e)); }
  };
  const set = (k) => (e) => setMe({ ...me, [k]: e.target.value });
  const useMyLocation = () => navigator.geolocation?.getCurrentPosition((p) =>
    setMe((m) => ({ ...m, latitude: p.coords.latitude.toFixed(6), longitude: p.coords.longitude.toFixed(6) })));

  return (
    <>
      <div className="tabs">
        <button className={tab === "appointments" ? "on" : ""} onClick={() => setTab("appointments")}>Appointments</button>
        <button className={tab === "profile" ? "on" : ""} onClick={() => setTab("profile")}>My profile</button>
      </div>
      {err && <div className="alert">{err}</div>}

      {tab === "appointments" && (
        <>
          <div className="row between">
            <h2>{me ? me.name : "Appointments"}</h2>
            <select className="sel" value={filter} onChange={(e) => setFilter(e.target.value)}>
              <option value="upcoming">Upcoming</option><option value="today">Today</option><option value="all">All history</option>
            </select>
          </div>
          {shown.length === 0 && <p className="muted">No appointments here.</p>}
          <div className="list">
            {shown.map((a) => (
              <div className="card appt" key={a.id}>
                <div>
                  <b>{a.date} · {a.slot}</b> <span className={`badge ${a.status}`}>{a.status}</span>
                  <div>{a.patientName}{a.patientPhone && <span className="muted"> · 📞 {a.patientPhone}</span>}</div>
                  {a.problem && <div className="muted small">“{a.problem}”</div>}
                  <div className="muted small mono">{a.ticketCode}</div>
                </div>
                {a.status === "CONFIRMED" && (
                  <div className="row gap">
                    <button className="btn sm" onClick={() => act(a, "complete")}>Mark completed</button>
                    <button className="btn danger sm" onClick={() => act(a, "cancel")}>Cancel</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </>
      )}

      {tab === "profile" && me && (
        <form className="card" onSubmit={saveProfile}>
          <h2>{me.name} <span className="muted">· {me.specialization}</span></h2>
          <p className="muted small">Name and specialization are managed by the admin. Rating: {me.rating.toFixed(1)} ({me.ratingCount} reviews)</p>
          <div className="form-grid">
            <label>Clinic / hospital<input required value={me.clinicName} onChange={set("clinicName")} /></label>
            <label>Address<input required value={me.address} onChange={set("address")} /></label>
            <label>Latitude<input required type="number" step="any" value={me.latitude} onChange={set("latitude")} /></label>
            <label>Longitude<input required type="number" step="any" value={me.longitude} onChange={set("longitude")} /></label>
            <label>Fee (₹)<input required type="number" min="0" value={me.fee} onChange={set("fee")} /></label>
            <label>Experience (years)<input required type="number" min="0" value={me.experienceYears} onChange={set("experienceYears")} /></label>
          </div>
          <div className="row gap">
            <button type="button" className="btn ghost" onClick={useMyLocation}>📍 Use my current location</button>
            <button className="btn">Save profile</button>
            {msg && <span className="muted">{msg}</span>}
          </div>
        </form>
      )}
    </>
  );
}
