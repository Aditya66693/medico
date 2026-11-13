import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { errMsg } from "../api.js";

export default function Ticket() {
  const { id } = useParams();
  const [a, setA] = useState(null);
  const [err, setErr] = useState("");
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");

  const load = () => api.get(`/api/appointments/${id}`).then((r) => setA(r.data)).catch((e) => setErr(errMsg(e)));
  useEffect(() => { load(); }, [id]);

  const act = async (fn) => { setErr(""); try { setA((await fn()).data); } catch (e) { setErr(errMsg(e)); } };
  const cancel = () => window.confirm("Cancel this appointment?") && act(() => api.post(`/api/appointments/${id}/cancel`));
  const review = () => act(() => api.post(`/api/appointments/${id}/review`, { rating, comment }));

  if (!a) return <p>{err || "Loading…"}</p>;
  const ok = a.status === "CONFIRMED" || a.status === "COMPLETED";
  return (
    <div className="ticket-wrap">
      <div className={`ticket ${ok ? "" : "void"}`}>
        <div className="ticket-top">
          <div className="check">{ok ? "✓" : "✕"}</div>
          <h2>{a.status === "COMPLETED" ? "Visit completed" : ok ? "Appointment confirmed" : a.status === "CANCELLED" ? "Appointment cancelled" : "Awaiting payment"}</h2>
          <div className="code">{a.ticketCode}</div>
        </div>
        <div className="ticket-body">
          <Row k="Patient" v={a.patientName} />
          <Row k="Doctor" v={`${a.doctorName} (${a.specialization})`} />
          <Row k="Clinic" v={a.clinicName} />
          <Row k="Address" v={a.address} />
          <Row k="Date" v={a.date} />
          <Row k="Time" v={a.slot} />
          <Row k="Amount" v={`₹${a.amount} · ${PAY[a.paymentStatus] || a.paymentStatus}`} />
          <p className="muted small">Show this ticket code at the clinic reception. Please arrive 10 minutes early.</p>
        </div>
      </div>

      {err && <div className="alert">{err}</div>}
      <div className="row center gap noprint">
        <button className="btn ghost" onClick={() => window.print()}>🖨 Print / Save PDF</button>
        {a.status === "CONFIRMED" && <button className="btn danger" onClick={cancel}>Cancel appointment</button>}
        <Link className="btn ghost" to="/appointments">My appointments</Link>
      </div>

      {ok && !a.reviewed && (
        <div className="card noprint">
          <h3>Rate your visit</h3>
          <div className="row gap">
            {[1, 2, 3, 4, 5].map((n) => (
              <button key={n} className={`star ${n <= rating ? "on" : ""}`} onClick={() => setRating(n)}>★</button>
            ))}
          </div>
          <textarea rows={2} placeholder="Write a short review (optional)" value={comment} onChange={(e) => setComment(e.target.value)} />
          <button className="btn" onClick={review}>Submit review</button>
          <p className="muted small">Reviews are accepted after your appointment time has passed.</p>
        </div>
      )}
      {a.reviewed && <p className="center muted">Thanks for your review ⭐</p>}
    </div>
  );
}

const PAY = { PAID: "Paid", DEMO_PAID: "DEMO — no real payment made", UNPAID: "Unpaid", REFUNDED: "Refunded", REFUND_FAILED: "Refund pending — contact support" };
const Row = ({ k, v }) => <div className="row between trow"><span className="muted">{k}</span><b>{v}</b></div>;
