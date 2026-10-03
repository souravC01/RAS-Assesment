import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api.js';
export default function WorkerHistory() {
  const [items,setItems]=useState(null),[error,setError]=useState(''),[attempt,setAttempt]=useState(0);
  useEffect(()=>{
    const controller=new AbortController();setItems(null);setError('');
    api('/submissions',{signal:controller.signal}).then(setItems).catch(e=>{if(e.name!=='AbortError') setError(e.message);});
    return ()=>controller.abort();
  },[attempt]);
  return <section><div className="page-heading"><div><p className="eyebrow">Daily records</p><h1>My submissions</h1></div>
    <Link className="button" to="/submissions/new">New submission</Link></div>
    <p>View your submitted safety checks. Submitted forms cannot be edited.</p>
    {error?<div role="alert" className="error">{error} <button onClick={()=>setAttempt(a=>a+1)}>Try again</button></div>
      :items===null?<p role="status">Loading your submissions…</p>:items.length===0?<div className="panel empty"><h2>No submissions yet</h2><p>Your daily safety records will appear here.</p></div>
      :<ul className="history-list">{items.map(item=><li key={item.id}><Link className="history-card panel" to={`/submissions/${item.id}`}>
        <div><strong>{item.site.name}</strong><p><time dateTime={item.workDate}>{item.workDate}</time></p></div><span className="status">{item.status}</span><span aria-hidden="true">→</span>
      </Link></li>)}</ul>}
  </section>;
}
