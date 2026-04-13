import React, { useState } from 'react'

export default function Admin(){
  const [output, setOutput] = useState('')
  const [teamId, setTeamId] = useState('')
  const [loading, setLoading] = useState(false)
  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'
  const API = API_BASE + '/api'

  async function call(path, opts){
    try{
      let url
      if (path.startsWith('http')) url = path
      else if (path.startsWith('/api')) url = API_BASE + path
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

  async function generateTeam(){
    setLoading(true)
    try {
      const res = await fetch(`${API_BASE}/api/admin/generate-team`, { method: 'POST' })
      const data = await res.json()
      setOutput(`✓ Team "${data.name}" erfolgreich generiert!\nTeam ID: ${data.id}\nBudget: ${data.budget}`)
    } catch(e) {
      setOutput(`✗ Fehler: ${e.message}`)
    }
    setLoading(false)
  }

  return (
    <div className="app-container">
      <h2>Admin Console</h2>
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Team-Verwaltung</h3>
        <div style={{display:'flex',gap:8,flexWrap:'wrap', marginBottom: 12}}>
          <button 
            className="btn primary" 
            onClick={generateTeam}
            disabled={loading}
            style={{ opacity: loading ? 0.6 : 1, cursor: loading ? 'not-allowed' : 'pointer' }}
          >
            {loading ? '⏳ Generiere...' : '➕ Neues Team generieren'}
          </button>
          <button className="btn primary" onClick={()=>call('/api/players')}>List Players</button>
          <button className="btn primary" onClick={()=>call('/api/teams')}>List Teams</button>
          <button className="btn secondary" onClick={()=>call('/api/admin/clear-users',{method:'POST'})}>Clear Users</button>
          <button className="btn" style={{backgroundColor:'#dc3545'}} onClick={clearAll}>🗑️ Delete ALL Data</button>
        </div>

        <h4>Team Details</h4>
        <div style={{marginBottom: 12}}>
          <input className="input" placeholder="teamId" value={teamId} onChange={e=>setTeamId(e.target.value)} style={{width:120, marginRight:8}} />
          <button className="btn secondary" onClick={()=>call(`/api/teams/${teamId}`)}>Get Team</button>
          <button className="btn secondary" onClick={()=>call(`/api/teams/${teamId}/state`)}>Get Team State</button>
        </div>

        <h4>Utilities</h4>
        <div style={{marginBottom: 12}}>
          <button className="btn" onClick={()=>{ localStorage.clear(); setOutput('localStorage cleared') }}>Clear localStorage</button>
        </div>

        <h4>Output</h4>
        <textarea readOnly value={output} style={{width:'100%',height:240}} />
      </div>
    </div>
  )
}
