import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api.js';
import {checklist,answers} from '../checklist.js';
function SupportingPhoto({photo,index}) {
  const [url,setUrl]=useState(''),[error,setError]=useState(''),[pending,setPending]=useState(false);
  async function view() {setPending(true);setError('');try{setUrl((await api(`/photos/${photo.id}/url`)).url);}catch(e){setError(e.message);}finally{setPending(false);}}
  return <div className="photo-detail"><button className="secondary" disabled={pending} onClick={view}>{pending?'Loading photo…':url?`Refresh photo ${index+1}`:`View photo ${index+1}`}</button>
    {url&&<img src={url} alt={`Supporting photo ${index+1}`} onError={()=>{setUrl('');setError('This photo link expired or could not load. Request a new link.');}} />}
    {error&&<p className="error" role="alert">{error}</p>}
  </div>;
}
export default function SubmissionDetail({id,user}) {
  const [detail,setDetail]=useState(null),[error,setError]=useState('');
  useEffect(()=>{const controller=new AbortController();setDetail(null);setError('');api(`/submissions/${id}`,{signal:controller.signal})
    .then(setDetail).catch(e=>{if(e.name!=='AbortError')setError(e.message);});return()=>controller.abort();},[id]);
  return <section><Link className="back-link" to={user.role==='ADMIN'?'/admin':'/submissions'}>← {user.role==='ADMIN'?'All submissions':'My submissions'}</Link>
    <div className="page-intro"><p className="eyebrow">Read-only record</p><h1>Submission detail</h1></div>
    {error?<p className="error" role="alert">{error}</p>:!detail?<p role="status">Loading submission…</p>:<>
      <section className="panel"><div className="page-heading"><h2>{detail.site.name}</h2><span className="status">{detail.status}</span></div><dl className="metadata">
        <div><dt>Framer</dt><dd>{detail.worker.name}</dd></div><div><dt>Work date</dt><dd><time data-work-date dateTime={detail.workDate}>{detail.workDate}</time></dd></div>
        <div><dt>Recorded</dt><dd>{new Intl.DateTimeFormat('en-CA',{timeZone:'America/Vancouver',dateStyle:'medium',timeStyle:'short'}).format(new Date(detail.submittedAt))} (Vancouver)</dd></div>
      </dl><p className="muted">Submitted records do not certify that a site is safe.</p></section>
      <section className="panel"><h2>Safety checklist</h2><dl className="read-checklist">{checklist.map(([key,label])=><div key={key}><dt>{label}</dt><dd className={detail.checklist[key]==='ISSUE'?'issue-label':''}>{answers.find(([value])=>value===detail.checklist[key])?.[1]}</dd></div>)}</dl></section>
      <section className="panel"><h2>Notes</h2><p className="notes">{detail.notes||'No additional notes.'}</p></section>
      <section className="panel"><h2>Supporting photos</h2><p>Private links expire after five minutes. Request a fresh link if a photo does not load.</p>
        <div className="photo-grid">{detail.photos.map((photo,index)=><SupportingPhoto key={photo.id} photo={photo} index={index} />)}</div></section>
    </>}
  </section>;
}
