import React, { useState } from 'react'

export default function Admin(){
  const [output, setOutput] = useState('')
  const [teamId, setTeamId] = useState('')
  const API = 'http://localhost:8080/api'

  async function call(path, opts){
    try{
      let url
      if (path.startsWith('http')) url = path
      else if (path.startsWith('/api')) url = API + path.slice(4)
      else url = API + path
      const res = await fetch(url, opts)
      const text = await res.text()
      setOutput(`HTTP ${res.status}\n${text}`)
    }catch(e){ setOutput(String(e)) }
  }

  async function clearAll(){
    const confirmed = window.confirm('⚠️ ACHTUNG: Dies wird ALLE Teams, Spieler und User löschen! Diese Operation kann nicht rückgängig gemacht werden.\n\nFortfahren?')
    if (!confirmed) return
    
    const doubleConfirm = window.confirm('Wirklich alle Daten löschen? Dies ist die letzte Warnung!')
    if (!doubleConfirm) return
    
    await call('/admin/clear-all', {method: 'DELETE'})
  }

  return (
    <div className="app-container">
      <h2>Admin Console</h2>
      <div className="card">
        <div style={{display:'flex',gap:8,flexWrap:'wrap'}}>
          <button className="btn primary" onClick={()=>call('/api/players')}>List Players</button>
          <button className="btn primary" onClick={()=>call('/api/teams')}>List Teams</button>
          <button className="btn secondary" onClick={()=>call('/api/admin/clear-users',{method:'POST'})}>Clear Users</button>
          <button className="btn" style={{backgroundColor:'#dc3545'}} onClick={clearAll}>🗑️ Delete ALL Data</button>
        </div>
        <div style={{marginTop:12}}>
          <input className="input" placeholder="teamId" value={teamId} onChange={e=>setTeamId(e.target.value)} style={{width:120, marginRight:8}} />
          <button className="btn secondary" onClick={()=>call(`/api/teams/${teamId}`)}>Get Team</button>
          <button className="btn secondary" onClick={()=>call(`/api/teams/${teamId}/state`)}>Get Team State</button>
        </div>
        <div style={{marginTop:12}}>
          <button className="btn" onClick={()=>{ localStorage.clear(); setOutput('localStorage cleared') }}>Clear localStorage</button>
        </div>
        <div style={{marginTop:12}}>
          <textarea readOnly value={output} style={{width:'100%',height:240}} />
        </div>
      </div>
    </div>
  )
}
