import { useEffect, useState } from 'react';
import { api, logout } from './api.js';
import Login from './pages/Login.jsx';
import {BrowserRouter,Routes,Route,Navigate,Link,useNavigate,useParams} from 'react-router-dom';
import WorkerHistory from './pages/WorkerHistory.jsx';
import SubmissionForm from './pages/SubmissionForm.jsx';
import SubmissionDetail from './pages/SubmissionDetail.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';

function DetailRoute({user}) {const {id}=useParams();return <SubmissionDetail id={id} user={user} />;}
function Application() {
  const navigate=useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let active = true;
    api('/auth/me').then(actor => { if (active) setUser(actor); })
      .catch(failure => { if (active && failure.status !== 401) setError(failure.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  async function signOut() {
    try { await logout(); setUser(null); setError(''); navigate('/login'); }
    catch (failure) { if (failure.status === 401) {setUser(null);navigate('/login');} else setError(failure.message); }
  }
  const home=user?.role==='ADMIN'?'/admin':'/submissions';
  function signedIn(actor) {const same=actor.id===user?.id;setUser(actor);setError('');if(!same)navigate(actor.role==='ADMIN'?'/admin':'/submissions');}
  return <><header className="app-header"><div className="header-inner"><Link className="wordmark" to={user?home:'/login'}><img src="/ras-logo.png" alt="Ron Anderson & Sons Ltd." /><span>Site safety</span></Link>
    {user&&<div className="account"><span>{user.name} · {user.role==='ADMIN'?'Admin':'Framer'}</span><button className="secondary" onClick={signOut}>Sign out</button></div>}</div></header><main>
    {error && <p className="error" role="alert">{error}</p>}
    {loading ? <p role="status">Connecting to site safety…</p> : !user
      ? <Routes><Route path="/login" element={<Login onSignedIn={signedIn} />} /><Route path="*" element={<Navigate to="/login" replace />} /></Routes>
      : <Routes key={user.id}><Route path="/login" element={<Navigate to={home} replace />} />
        <Route path="/submissions" element={user.role==='FRAMER'?<WorkerHistory />:<Navigate to="/admin" replace />} />
        <Route path="/submissions/new" element={user.role==='FRAMER'?<SubmissionForm user={user} onSignedIn={signedIn} onCreated={id=>navigate(`/submissions/${id}`)} />:<Navigate to="/admin" replace />} />
        <Route path="/submissions/:id" element={<DetailRoute user={user} />} />
        <Route path="/admin" element={user.role==='ADMIN'?<AdminDashboard />:<Navigate to="/submissions" replace />} />
        <Route path="*" element={<Navigate to={home} replace />} /></Routes>}
  </main><footer>RAS · Daily site safety records</footer></>;
}
export default function App() {return <BrowserRouter><Application /></BrowserRouter>;}
