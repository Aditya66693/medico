import { Link, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { homeFor, useAuth } from "./auth.jsx";
import Auth from "./pages/Auth.jsx";
import Home from "./pages/Home.jsx";
import Doctors from "./pages/Doctors.jsx";
import Book from "./pages/Book.jsx";
import Ticket from "./pages/Ticket.jsx";
import MyAppointments from "./pages/MyAppointments.jsx";
import Profile from "./pages/Profile.jsx";
import Admin from "./pages/Admin.jsx";
import DoctorDashboard from "./pages/DoctorDashboard.jsx";

function Protected({ roles = ["PATIENT"], children }) {
  const { user } = useAuth();
  const loc = useLocation();
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname + loc.search }} replace />;
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />;
  return children;
}

export default function App() {
  const { user, logout } = useAuth();
  return (
    <>
      <nav className="nav">
        <Link to={user ? homeFor(user.role) : "/"} className="brand">🩺 Medico{user && user.role !== "PATIENT" && <small> · {user.role === "ADMIN" ? "Admin" : "Doctor"}</small>}</Link>
        <div className="nav-links">
          {user ? (
            <>
              {user.role === "PATIENT" && <Link to="/appointments">My appointments</Link>}
              <Link to="/profile">👤 {user.name.split(" ")[0]}</Link>
              <button className="btn ghost" onClick={logout}>Logout</button>
            </>
          ) : (
            <Link to="/login" className="btn">Login / Register</Link>
          )}
        </div>
      </nav>
      <main className="container">
        <Routes>
          <Route path="/login" element={<Auth />} />
          <Route path="/" element={<Protected><Home /></Protected>} />
          <Route path="/doctors" element={<Protected><Doctors /></Protected>} />
          <Route path="/book/:doctorId" element={<Protected><Book /></Protected>} />
          <Route path="/ticket/:id" element={<Protected><Ticket /></Protected>} />
          <Route path="/appointments" element={<Protected><MyAppointments /></Protected>} />
          <Route path="/profile" element={<Protected roles={["PATIENT", "DOCTOR", "ADMIN"]}><Profile /></Protected>} />
          <Route path="/admin" element={<Protected roles={["ADMIN"]}><Admin /></Protected>} />
          <Route path="/doctor" element={<Protected roles={["DOCTOR"]}><DoctorDashboard /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="footer">Medico is a booking tool, not a medical service. In an emergency call 112.</footer>
    </>
  );
}
