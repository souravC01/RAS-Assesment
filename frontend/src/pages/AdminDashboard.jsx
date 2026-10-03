import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api.js';
const emptyFilters={workerId:'',siteId:'',from:'',to:''};
export default function AdminDashboard() {
  const [sites,setSites]=useState([]),[workers,setWorkers]=useState([]),[lookupError,setLookupError]=useState('');
  const [draft,setDraft]=useState({...emptyFilters}),[applied,setApplied]=useState({...emptyFilters});
  const [result,setResult]=useState(null),[error,setError]=useState('');
  useEffect(()=>{const controller=new AbortController();Promise.all([api('/sites',{signal:controller.signal}),api('/admin/workers',{signal:controller.signal})])
    .then(([sites,workers])=>{if(!controller.signal.aborted){setSites(sites);setWorkers(workers);}})
    .catch(e=>{if(e.name!=='AbortError')setLookupError(e.message);});return()=>controller.abort();},[]);
  useEffect(()=>{const controller=new AbortController();setResult(null);setError('');
    const query=new URLSearchParams(Object.entries(applied).filter(([,value])=>value!==''));
    api(`/admin/submissions${query.size?'?'+query:''}`,{signal:controller.signal})
      .then(data=>{if(!controller.signal.aborted)setResult(data);})
      .catch(e=>{if(e.name!=='AbortError'&&!controller.signal.aborted)setError(e.message);});
    return()=>controller.abort();
  },[applied]);
  function field(key,value){setDraft(current=>({...current,[key]:value}));}
  return <section><p className="eyebrow">Admin overview</p><h1>All submissions</h1><p>Inspect daily safety records across your framers and job sites.</p>
    <section className="panel filters"><h2>Find submissions</h2>
      <form onSubmit={e=>{e.preventDefault();setResult(null);setApplied({...draft});}}>
        <div className="filter-grid"><div className="filter-field"><label htmlFor="admin-worker">Worker</label><select id="admin-worker" value={draft.workerId} onChange={e=>field('workerId',e.target.value)}><option value="">All framers</option>{workers.map(worker=><option key={worker.id} value={worker.id}>{worker.name}</option>)}</select></div>
          <div className="filter-field"><label htmlFor="admin-site">Job site</label><select id="admin-site" value={draft.siteId} onChange={e=>field('siteId',e.target.value)}><option value="">All job sites</option>{sites.map(site=><option key={site.id} value={site.id}>{site.name}</option>)}</select></div>
          <label>From date<input type="date" value={draft.from} max={draft.to||undefined} onChange={e=>field('from',e.target.value)} /></label>
          <label>To date<input type="date" value={draft.to} min={draft.from||undefined} onChange={e=>field('to',e.target.value)} /></label>
        </div><div className="filter-actions"><button>Apply filters</button><button type="button" className="secondary" onClick={()=>{setDraft({...emptyFilters});setResult(null);setApplied({...emptyFilters});}}>Reset filters</button></div>
      </form>{lookupError&&<p className="error" role="alert">{lookupError}</p>}
    </section>
    {error?<div className="error" role="alert">{error} <button onClick={()=>setApplied({...applied})}>Try again</button></div>:result===null?<p role="status">Loading matching submissions…</p>:<>
      <section className="panel"><div className="page-heading"><h2>Submissions by site</h2><p className="muted">{result.items.length} {result.items.length===1?'submission matches':'submissions match'}</p></div>
        {result.countsBySite.length===0?<p>No site totals for the current filters.</p>:<ul className="site-counts">{result.countsBySite.map(site=><li data-testid="site-count" key={site.siteId}><span>{site.siteName}</span><strong>{site.count}</strong></li>)}</ul>}
      </section>
      {result.items.length===0?<section className="panel empty"><h2>No submissions match these filters.</h2><p>Adjust the date range, worker, or job site.</p></section>:<section className="panel table-panel"><h2>Submission records</h2><div className="table-scroll"><table><thead><tr><th>Worker</th><th>Site</th><th>Date</th><th>Status</th><th aria-label="Details"></th></tr></thead>
        <tbody>{result.items.map(item=><tr key={item.id}><td>{item.worker.name}</td><td>{item.site.name}</td><td><time dateTime={item.workDate}>{item.workDate}</time></td><td><span className="status">{item.status}</span></td><td><Link aria-label={`View submission ${item.id}`} to={`/submissions/${item.id}`}>View</Link></td></tr>)}</tbody></table></div>
      </section>}
    </>}
  </section>;
}
