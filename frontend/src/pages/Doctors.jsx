import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api, { errMsg } from "../api.js";

const DEFAULT_LOC = { lat: 26.8467, lng: 80.9462, label: "Lucknow centre (default)" };

export const Stars = ({ v }) => (
  <span className="stars" title={`${v} / 5`}>
    {"★".repeat(Math.round(v))}<span className="dim">{"★".repeat(5 - Math.round(v))}</span>
  </span>
);

export default function Doctors() {
  const [sp] = useSearchParams();
  const spec = sp.get("spec") || "";
  const [loc, setLoc] = useState(null);
  const [docs, setDocs] = useState([]);
  const [limit, setLimit] = useState(3);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const nav = useNavigate();

  useEffect(() => {
    if (!navigator.geolocation) return setLoc(DEFAULT_LOC);
    navigator.geolocation.getCurrentPosition(
      (p) => setLoc({ lat: p.coords.latitude, lng: p.coords.longitude, label: "your location" }),
      () => setLoc(DEFAULT_LOC),
      { timeout: 8000 }
    );
  }, []);

  useEffect(() => {
    if (!loc) return;
    setLoading(true);
    api.get("/api/doctors", { params: { specialization: spec, lat: loc.lat, lng: loc.lng, limit } })
      .then((r) => setDocs(r.data)).catch((e) => setErr(errMsg(e))).finally(() => setLoading(false));
  }, [loc, spec, limit]);

  return (
    <>
      <h2>{spec || "All doctors"} near you</h2>
      <p className="muted">Sorted by distance from {loc ? loc.label : "…locating you"}. Ratings come from verified patients.</p>
      {err && <div className="alert">{err}</div>}
      {loading && <p>Loading…</p>}
      {!loading && docs.length === 0 && <p>No doctors found.</p>}
      <div className="list">
        {docs.map((d) => (
          <div className="card doc" key={d.id}>
            <div className="avatar">{d.name.replace("Dr. ", "")[0]}</div>
            <div className="doc-info">
              <h3>{d.name}</h3>
              <div className="muted">{d.specialization} · {d.experienceYears} yrs experience</div>
              <div><b>{d.clinicName}</b></div>
              <div className="muted small">📍 {d.address}{d.distanceKm != null && ` · ${d.distanceKm} km away`}</div>
              <div>
                <Stars v={d.rating} /> <b>{d.rating.toFixed(1)}</b> <span className="muted small">({d.ratingCount} reviews)</span>
              </div>
            </div>
            <div className="doc-action">
              <div className="fee">₹{d.fee}</div>
              <button className="btn" onClick={() => nav(`/book/${d.id}`)}>Book</button>
              <a className="small" target="_blank" rel="noreferrer"
                 href={`https://www.google.com/maps/search/?api=1&query=${d.latitude},${d.longitude}`}>Open in Maps</a>
            </div>
          </div>
        ))}
      </div>
      {docs.length >= limit && limit < 20 && <button className="btn ghost" onClick={() => setLimit(limit + 3)}>Show more</button>}
    </>
  );
}
