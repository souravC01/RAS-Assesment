import { useEffect, useRef, useState } from 'react';
import { api, logout, SESSION_REFRESH_EVENT } from './api.js';
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
  const actorRef=useRef(null),syncSequence=useRef(0),accountChannel=useRef(null);
  function applyActor(actor,navigateOnChange=false) {
    const previous=actorRef.current;actorRef.current=actor;setUser(actor);setError('');setLoading(false);
    if(navigateOnChange&&previous?.id!==actor?.id)navigate(actor?actor.role==='ADMIN'?'/admin':'/submissions':'/login');
  }
  useEffect(() => {
    let active = true;
    async function synchronize() {
      const sequence=++syncSequence.current;
      try {const actor=await api('/auth/me');if(active&&sequence===syncSequence.current)applyActor(actor,!!actorRef.current);}
      catch(failure){if(active&&sequence===syncSequence.current&&failure.status!==401)setError(failure.message);}
      // Background expiry retains a same-account draft; explicit logout clears it below.
      finally{if(active&&sequence===syncSequence.current)setLoading(false);}
    }
    function changed(message) {
      if(message.data?.type==='logout'){++syncSequence.current;applyActor(null,true);}
      else if(message.data?.type==='login')synchronize();
    }
    function visible(){if(document.visibilityState==='visible')synchronize();}
    let channel;
    if(typeof BroadcastChannel!=='undefined'){channel=new BroadcastChannel('ras-account');channel.onmessage=changed;accountChannel.current=channel;}
    window.addEventListener('focus',synchronize);window.addEventListener(SESSION_REFRESH_EVENT,synchronize);
    document.addEventListener('visibilitychange',visible);synchronize();
    return () => {active=false;++syncSequence.current;channel?.close();accountChannel.current=null;
      window.removeEventListener('focus',synchronize);window.removeEventListener(SESSION_REFRESH_EVENT,synchronize);document.removeEventListener('visibilitychange',visible);};
  }, []);
  async function signOut() {
    try { await logout(); ++syncSequence.current;applyActor(null,true);accountChannel.current?.postMessage({type:'logout'}); }
    catch (failure) { if (failure.status === 401) {++syncSequence.current;applyActor(null,true);accountChannel.current?.postMessage({type:'logout'});} else setError(failure.message); }
  }
  const home=user?.role==='ADMIN'?'/admin':'/submissions';
  function signedIn(actor) {++syncSequence.current;applyActor(actor,true);accountChannel.current?.postMessage({type:'login'});}
  return <><a className="skip-link" href="#main-content">Skip to content</a><header className="app-header"><div className="header-inner"><Link className="wordmark" to={user?home:'/login'}><img src="/ras-logo.png" alt="Ron Anderson & Sons Ltd." /><span>Site safety</span></Link>
    {user&&<div className="account"><span>{user.name} · {user.role==='ADMIN'?'Admin':'Framer'}</span><button className="secondary" onClick={signOut}>Sign out</button></div>}</div></header><main id="main-content" tabIndex={-1}>
    {error && <p className="error" role="alert">{error}</p>}
    {loading ? <p role="status">Connecting to site safety…</p> : !user
      ? <Routes><Route path="/login" element={<Login onSignedIn={signedIn} />} /><Route path="*" element={<Navigate to="/login" replace />} /></Routes>
      : <Routes key={user.id}><Route path="/login" element={<Navigate to={home} replace />} />
        <Route path="/submissions" element={user.role==='FRAMER'?<WorkerHistory />:<Navigate to="/admin" replace />} />
        <Route path="/submissions/new" element={user.role==='FRAMER'?<SubmissionForm user={user} onSignedIn={signedIn} onCreated={id=>navigate(`/submissions/${id}`)} />:<Navigate to="/admin" replace />} />
        <Route path="/submissions/:id" element={<DetailRoute user={user} />} />
        <Route path="/admin" element={user.role==='ADMIN'?<AdminDashboard />:<Navigate to="/submissions" replace />} />
        <Route path="*" element={<Navigate to={home} replace />} /></Routes>}
  </main><footer><div className="footer-inner"><div><strong>Ron Anderson & Sons Ltd.</strong><span>Daily site safety records</span></div><a href="https://www.rasltd.ca/">Visit RAS website ↗</a></div></footer></>;
}
export default function App() {return <BrowserRouter><Application /></BrowserRouter>;}
