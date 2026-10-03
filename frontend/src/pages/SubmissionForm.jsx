import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api.js';
import {checklist,answers,today} from '../checklist.js';
import Login from './Login.jsx';
function Preview({file}) {
  const [url,setUrl]=useState('');
  useEffect(()=>{const next=URL.createObjectURL(file);setUrl(next);return()=>URL.revokeObjectURL(next);},[file]);
  return <img src={url} alt={`Preview of ${file.name}`} />;
}
export default function SubmissionForm({user,onCreated,onSignedIn}) {
  const [sites,setSites]=useState([]),[siteError,setSiteError]=useState(''),[siteAttempt,setSiteAttempt]=useState(0),[sitesLoading,setSitesLoading]=useState(false);
  const [siteId,setSiteId]=useState(''),[workDate,setWorkDate]=useState(today),[checks,setChecks]=useState({});
  const [notes,setNotes]=useState(''),[photos,setPhotos]=useState([]);
  const [error,setError]=useState(''),[fields,setFields]=useState({}),[pending,setPending]=useState(false),[expired,setExpired]=useState(false),[uncertain,setUncertain]=useState(false);
  useEffect(()=>{const controller=new AbortController();setSiteError('');setSitesLoading(true);
    api('/sites',{signal:controller.signal}).then(result=>{if(!controller.signal.aborted)setSites(result);})
      .catch(e=>{if(!controller.signal.aborted){setSiteError(e.message);if(e.status===401)setExpired(true);}})
      .finally(()=>{if(!controller.signal.aborted)setSitesLoading(false);});
    return()=>controller.abort();},[siteAttempt]);
  const issue=Object.values(checks).includes('ISSUE');
  function addPhotos(event) {
    const added=Array.from(event.target.files);event.target.value='';
    if(added.some(file=>!['image/jpeg','image/png'].includes(file.type)||file.size>5_000_000)||photos.length+added.length>5) {
      setError('Choose up to five JPEG or PNG photos, each at most 5 MB.');return;
    }
    setPhotos(current=>[...current,...added]);setError('');
  }
  async function submit(event) {
    event.preventDefault();setError('');setFields({});setUncertain(false);
    if(photos.length===0) {setError('Add at least one supporting photo.');return;}
    const body=new FormData();body.append('form',new Blob([JSON.stringify({siteId:Number(siteId),workDate,checklist:checks,notes})],{type:'application/json'}));
    photos.forEach(file=>body.append('photos',file,file.name));setPending(true);
    try {const result=await api('/submissions',{method:'POST',body});onCreated(result.id);}
    catch(failure) {setError(failure.message);setFields(failure.fieldErrors||{});setExpired(failure.status===401);
      setUncertain(failure.status===0||failure.status>=500||failure.status===409);}
    finally {setPending(false);}
  }
  return <section className="form-page"><Link className="back-link" to="/submissions">← My submissions</Link>
    <p className="eyebrow">Before work begins</p><h1>Daily safety form</h1>
    <p>Report each check honestly. An issue can be submitted with explanatory notes.</p>
    {sitesLoading&&<p role="status">Loading job sites…</p>}
    {siteError&&<div className="error" role="alert"><p>{siteError}</p>{!expired&&<button type="button" disabled={sitesLoading} onClick={()=>setSiteAttempt(a=>a+1)}>Retry loading sites</button>}</div>}
    <form onSubmit={submit}>
      <fieldset disabled={pending||expired} className="form-fields">
        <section className="panel"><h2>Site and date</h2><div className="form-fields">
          <label>Framer<input value={user.name} readOnly /></label>
          <div className="filter-field"><label htmlFor="form-site">Job site</label><select id="form-site" required value={siteId} onChange={e=>setSiteId(e.target.value)}><option value="">Select a job site</option>{sites.map(site=><option value={site.id} key={site.id}>{site.name}</option>)}</select></div>
          <label>Work date<input type="date" required value={workDate} max={today()} onChange={e=>setWorkDate(e.target.value)} /></label>
          <small>Dates follow America/Vancouver.</small>
        </div></section>
        <section className="panel"><h2>Safety checklist</h2><p>Answer all eight checks. Use Not applicable only when the check does not apply.</p>
          <div className="checklist">{checklist.map(([key,label])=><fieldset className="answer-group" key={key}><legend>{label}</legend><div className="answer-options">
            {answers.map(([value,text])=><label key={value} className={checks[key]===value?'selected':''}><input type="radio" name={key} value={value} required checked={checks[key]===value} onChange={()=>setChecks({...checks,[key]:value})} />{text}</label>)}
          </div></fieldset>)}</div>
        </section>
        <section className="panel"><h2>Notes and photos</h2><label htmlFor="notes">Notes</label><textarea id="notes" rows="4" maxLength={4000} required={issue} value={notes} onChange={e=>setNotes(e.target.value)} />
          <small>{issue?'Explain the reported issue.':'Optional when no issues are reported.'} {notes.length}/4,000 characters</small>
          <label className="photo-input">Supporting photos<input type="file" accept="image/jpeg,image/png" multiple onChange={addPhotos} /></label>
          <small>Add 1–5 JPEG or PNG photos, up to 5 MB each. Convert HEIC photos before uploading.</small>
          <ul className="photo-previews">{photos.map((file,index)=><li key={index}><Preview file={file} /><span>{file.name}</span><button type="button" className="secondary" onClick={()=>setPhotos(current=>current.filter((_,i)=>i!==index))}>Remove photo {index+1}</button></li>)}</ul>
        </section>
      </fieldset>
      {error&&<div className="error" role="alert"><p>{error}</p>{Object.entries(fields).map(([key,text])=><p key={key}>{text}</p>)}
        {uncertain&&<><Link to="/submissions" target="_blank" rel="noopener noreferrer">Check your history before retrying</Link><p>History opens in a new tab. Your draft stays here.</p></>}</div>}
      <p className="muted">After submission, this record cannot be edited.</p>
      <button disabled={pending||expired||sitesLoading||sites.length===0}>{pending?'Submitting photos…':'Submit form'}</button>
      {pending&&<p role="status">Please keep this page open while your photos upload.</p>}
    </form>
    {expired&&<Login inline onSignedIn={actor=>{onSignedIn(actor);if(actor.id===user.id) {setExpired(false);setSiteAttempt(a=>a+1);setError('Signed in again. Review your form and submit when ready.');}}} />}
  </section>;
}
