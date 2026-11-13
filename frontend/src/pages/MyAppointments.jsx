import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../api.js";

export default function MyAppointments() {
  const [list, setList] = useState(null);
  const [err, setErr] = useState("");
  useEffect(() => { api.get("/api/appointments/mine").then((r) => setList(r.data)).catch((e) => setErr(errMsg(e))); }, []);

  if (!list) return <p>{err || "Loading…"}</p>;
  return (
    <>
      <h2>My appointments</h2>
      {list.length === 0 && <p>No appointments yet. <Link to="/">Book one</Link></p>}
      <div className="list">
        {list.map((a) => (
          <Link to={`/ticket/${a.id}`} className="card appt" key={a.id}>
            <div>
              <b>{a.doctorName}</b> <span className="muted">· {a.specialization}</span>
              <div className="muted small">{a.clinicName}</div>
              <div>{a.date} at {a.slot}</div>
            </div>
            <div className="right">
              <span className={`badge ${a.status}`}>{a.status.replace("_", " ")}</span>
              <div className="muted small">{a.ticketCode}</div>
            </div>
          </Link>
        ))}
      </div>
    </>
  );
}
