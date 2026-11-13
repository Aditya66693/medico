import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api, { errMsg } from "../api.js";
import { useAuth } from "../auth.jsx";
import { Stars } from "./Doctors.jsx";

const iso = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);

function loadRazorpay() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    s.onload = resolve; s.onerror = () => reject(new Error("Could not load Razorpay"));
    document.body.appendChild(s);
  });
}

export default function Book() {
  const { doctorId } = useParams();
  const { user } = useAuth();
  const nav = useNavigate();
  const [doc, setDoc] = useState(null);
  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => { const d = new Date(); d.setDate(d.getDate() + i); return d; }), []);
  const [date, setDate] = useState(iso(days[0]));
  const [slots, setSlots] = useState([]);
  const [slot, setSlot] = useState("");
  const [problem, setProblem] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const [cfg, setCfg] = useState(null);

  useEffect(() => { api.get("/api/config").then((r) => setCfg(r.data)); }, []);
  useEffect(() => { api.get(`/api/doctors/${doctorId}`).then((r) => setDoc(r.data)).catch((e) => setErr(errMsg(e))); }, [doctorId]);
  useEffect(() => {
    setSlot("");
    api.get(`/api/doctors/${doctorId}/slots`, { params: { date } }).then((r) => setSlots(r.data)).catch((e) => setErr(errMsg(e)));
  }, [doctorId, date]);

  const pay = async () => {
    setErr(""); setBusy(true);
    let appt;
    try {
      const { data } = await api.post("/api/appointments", { doctorId: Number(doctorId), date, slot, problem });
      appt = data.appointment;
      if (data.demo) {
        await api.post(`/api/appointments/${appt.id}/pay/demo`);
        return nav(`/ticket/${appt.id}`);
      }
      await loadRazorpay();
      const rzp = new window.Razorpay({
        key: data.keyId, amount: data.amountPaise, currency: "INR", order_id: data.orderId,
        name: "Medico", description: `${doc.name} · ${date} ${slot}`,
        prefill: { name: user.name, email: user.email },
        theme: { color: "#0d9488" },
        handler: async (r) => {
          try {
            await api.post(`/api/appointments/${appt.id}/pay/verify`, {
              orderId: r.razorpay_order_id, paymentId: r.razorpay_payment_id, signature: r.razorpay_signature });
            nav(`/ticket/${appt.id}`);
          } catch (e) { setErr(errMsg(e)); setBusy(false); }
        },
        modal: { ondismiss: () => setBusy(false) },
      });
      rzp.open();
    } catch (e) { setErr(errMsg(e)); setBusy(false); }
  };

  if (!doc) return <p>{err || "Loading…"}</p>;
  return (
    <div className="book">
      <div className="card">
        <h2>Book appointment</h2>
        <h3>{doc.name} <span className="muted">· {doc.specialization}</span></h3>
        <div>{doc.clinicName} — <span className="muted">{doc.address}</span></div>
        <div><Stars v={doc.rating} /> {doc.rating.toFixed(1)} ({doc.ratingCount})</div>

        <h4>1. Pick a date</h4>
        <div className="row wrap">
          {days.map((d) => (
            <button key={iso(d)} className={`pill ${date === iso(d) ? "on" : ""}`} onClick={() => setDate(iso(d))}>
              <small>{d.toLocaleDateString("en-IN", { weekday: "short" })}</small>
              <b>{d.getDate()}</b>
              <small>{d.toLocaleDateString("en-IN", { month: "short" })}</small>
            </button>
          ))}
        </div>

        <h4>2. Pick a time</h4>
        <div className="row wrap">
          {slots.map((s) => (
            <button key={s.time} disabled={!s.available} className={`slot ${slot === s.time ? "on" : ""}`} onClick={() => setSlot(s.time)}>
              {s.time}
            </button>
          ))}
        </div>

        <h4>3. What's it about? (optional)</h4>
        <textarea rows={3} maxLength={1000} value={problem} onChange={(e) => setProblem(e.target.value)} placeholder="Briefly describe your problem for the doctor" />
      </div>

      <aside className="card summary">
        <h3>Summary</h3>
        <div className="row between"><span>Consultation fee</span><b>₹{doc.fee}</b></div>
        <div className="row between"><span>Date</span><b>{date}</b></div>
        <div className="row between"><span>Time</span><b>{slot || "—"}</b></div>
        <hr />
        <div className="row between total"><span>Total</span><b>₹{doc.fee}</b></div>
        {cfg?.demoPayments && <div className="notice">🧪 Demo mode: no real payment is taken. The appointment will be marked <b>DEMO</b>.</div>}
        {cfg && !cfg.paymentsLive && !cfg.demoPayments && <div className="alert">Online payments are not configured on this server.</div>}
        {cfg?.paymentsLive && <div className="notice">🔒 Secure checkout by Razorpay.</div>}
        {err && <div className="alert">{err}</div>}
        <button className="btn big" disabled={!slot || busy || !cfg || (!cfg.paymentsLive && !cfg.demoPayments)} onClick={pay}>{busy ? "Processing…" : cfg?.demoPayments ? `Confirm (demo, ₹${doc.fee})` : `Pay ₹${doc.fee} & confirm`}</button>
        <p className="muted small">Your slot is held for 10 minutes while you pay.</p>
      </aside>
    </div>
  );
}
