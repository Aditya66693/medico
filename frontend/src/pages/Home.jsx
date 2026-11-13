import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { errMsg } from "../api.js";
import { useAuth } from "../auth.jsx";

const ICONS = {
  "General Physician": "🧑‍⚕️", Neurologist: "🧠", Dentist: "🦷", Cardiologist: "❤️", Dermatologist: "🧴",
  Orthopedic: "🦴", Pediatrician: "🧸", "ENT Specialist": "👂", Gynecologist: "🌸", Ophthalmologist: "👁️", Psychiatrist: "🫂",
};

export default function Home() {
  const [specs, setSpecs] = useState([]);
  const [problem, setProblem] = useState("");
  const [res, setRes] = useState(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const nav = useNavigate();
  const { user } = useAuth();

  useEffect(() => { api.get("/api/specializations").then((r) => setSpecs(r.data)); }, []);
  const go = (s) => nav(`/doctors?spec=${encodeURIComponent(s)}`);

  const analyze = async () => {
    setErr(""); setRes(null); setBusy(true);
    try { setRes((await api.post("/api/ai/triage", { problem })).data); }
    catch (e) { setErr(errMsg(e)); }
    setBusy(false);
  };

  return (
    <>
      <section className="hero">
        <h1>Hello {user.name.split(" ")[0]}, who do you want to see?</h1>
        <p>Pick a specialist, or describe your problem and let AI suggest one.</p>
      </section>

      <h3>Choose a specialist</h3>
      <div className="grid chips">
        {specs.map((s) => (
          <button key={s} className="chip" onClick={() => go(s)}>
            <span className="emoji">{ICONS[s] || "🩺"}</span>{s}
          </button>
        ))}
      </div>

      <section className="card ai">
        <h3>✨ Not sure which doctor? Describe your problem</h3>
        <textarea rows={4} maxLength={1000} value={problem} onChange={(e) => setProblem(e.target.value)}
          placeholder="e.g. I've had a throbbing headache on one side for 3 days with sensitivity to light…" />
        <button className="btn" disabled={busy || problem.trim().length < 5} onClick={analyze}>
          {busy ? "Analyzing…" : "Suggest a doctor"}
        </button>
        {err && <div className="alert">{err}</div>}
        {res && (
          <div className={`result ${res.urgency === "emergency" ? "danger" : ""}`}>
            {res.urgency === "emergency" && <div className="alert">🚨 This may be an emergency. Call 112 or go to the nearest hospital now.</div>}
            <div className="muted">Suggested specialist</div>
            <h2>{ICONS[res.specialization]} {res.specialization}</h2>
            <p>{res.reason}</p>
            <p className="muted small">{res.disclaimer} (source: {res.source})</p>
            <button className="btn" onClick={() => go(res.specialization)}>Find {res.specialization}s near me →</button>
          </div>
        )}
      </section>
    </>
  );
}
