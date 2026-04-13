import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGame } from './GameContext'

export default function Auth(){
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [teamName, setTeamName] = useState('')
  const [error, setError] = useState(null)
  const [leagues, setLeagues] = useState([])
  const [selectedLeagueId, setSelectedLeagueId] = useState(null)
  const [loading, setLoading] = useState(false)
  const { setTeam } = useGame()
  const navigate = useNavigate()

  // Lade verfügbare Ligen beim Mount
  useEffect(() => {
    loadLeagues()
  }, [])

  const loadLeagues = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/auth/leagues')
      if (res.ok) {
        const data = await res.json()
        setLeagues(data)
        if (data.length > 0) {
          setSelectedLeagueId(data[0].id)
        }
      }
    } catch (e) {
      console.error('Fehler beim Laden der Ligen:', e)
    }
  }

  async function registerWithLeague(){
    setError(null)
    if (!selectedLeagueId) {
      setError('Bitte wählen Sie eine Liga')
      return
    }
    
    setLoading(true)
    try{
      const res = await fetch('http://localhost:8080/api/auth/register-with-league', { 
        method:'POST', 
        headers:{'Content-Type':'application/json'}, 
        body: JSON.stringify({username, password, teamName, leagueId: selectedLeagueId}) 
      })
      if (!res.ok){ const t = await res.text(); throw new Error(t) }
      const j = await res.json()
      const token = j.token
      const teamId = j.teamId
      localStorage.setItem('fm_auth', JSON.stringify({token, username, teamId, leagueId: selectedLeagueId}))
      // fetch team data
      const r2 = await fetch(`http://localhost:8080/api/teams/${teamId}`)
      if (!r2.ok) { const t = await r2.text(); throw new Error(t || 'failed to load team') }
      const team = await r2.json()
      setTeam(team)
      try{ localStorage.setItem('fm_currentTeam', JSON.stringify(team)) }catch(e){}
      navigate('/game')
    }catch(e){ 
      setError(e.message || String(e))
    } finally {
      setLoading(false)
    }
  }

  async function register(){
    setError(null)
    setLoading(true)
    try{
      const res = await fetch('http://localhost:8080/api/auth/register', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username,password,teamName}) })
      if (!res.ok){ const t = await res.text(); throw new Error(t) }
      const j = await res.json()
      const token = j.token
      const teamId = j.teamId
      localStorage.setItem('fm_auth', JSON.stringify({token, username, teamId}))
      // fetch team data
      const r2 = await fetch(`http://localhost:8080/api/teams/${teamId}`)
      if (!r2.ok) { const t = await r2.text(); throw new Error(t || 'failed to load team') }
      const team = await r2.json()
      setTeam(team)
      try{ localStorage.setItem('fm_currentTeam', JSON.stringify(team)) }catch(e){}
      navigate('/game')
    }catch(e){ setError(e.message || String(e)) }
    finally { setLoading(false) }
  }

  async function login(){
    setError(null)
    setLoading(true)
    try{
      const res = await fetch('http://localhost:8080/api/auth/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username,password}) })
      if (!res.ok){ const t = await res.text(); throw new Error(t) }
      const j = await res.json()
      const token = j.token
      const teamId = j.teamId
      localStorage.setItem('fm_auth', JSON.stringify({token, username, teamId}))
      const r2 = await fetch(`http://localhost:8080/api/teams/${teamId}`)
      if (!r2.ok) { const t = await r2.text(); throw new Error(t || 'failed to load team') }
      const team = await r2.json()
      setTeam(team)
      try{ localStorage.setItem('fm_currentTeam', JSON.stringify(team)) }catch(e){}
      navigate('/game')
    }catch(e){ setError(e.message || String(e)) }
    finally { setLoading(false) }
  }

  return (
    <div className="app-container">
      <div className="card">
        <h2>{mode === 'login' ? 'Login' : 'Registrieren'}</h2>
        <div className="form">
          <input className="input" value={username} onChange={e=>setUsername(e.target.value)} placeholder="Benutzername" disabled={loading} />
          <input className="input" type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="Passwort" disabled={loading} />
          {mode==='register' && <input className="input" value={teamName} onChange={e=>setTeamName(e.target.value)} placeholder="Teamname" disabled={loading} />}
          
          {/* Liga-Auswahl beim Registrieren */}
          {mode==='register' && (
            <div style={{ marginBottom: 12 }}>
              <label style={{ display: 'block', marginBottom: 8, fontSize: '0.9em', color: '#999' }}>Liga wählen:</label>
              <select 
                value={selectedLeagueId || ''} 
                onChange={(e) => setSelectedLeagueId(Number(e.target.value))}
                disabled={loading}
                className="input"
                style={{ padding: '8px 12px' }}
              >
                <option value="">-- Liga wählen --</option>
                {leagues.map(league => (
                  <option key={league.id} value={league.id}>
                    {league.divisionLabel} ({league.filledSlots}/{league.totalSlots} Teams)
                  </option>
                ))}
              </select>
            </div>
          )}

          {error ? <div style={{color:'#fda4af', marginBottom: 12}}>{error}</div> : null}
          <div style={{display:'flex',gap:8}}>
            {mode==='login'
              ? <button className="btn primary" onClick={login} disabled={loading}>{loading ? '⏳' : 'Login'}</button>
              : <button className="btn primary" onClick={registerWithLeague} disabled={loading || !selectedLeagueId}>
                  {loading ? '⏳ Wird registriert...' : 'Registrieren'}
                </button>
            }
            <button className="btn secondary" onClick={()=>setMode(mode==='login'?'register':'login')} disabled={loading}>
              {mode==='login' ? 'Registrieren' : 'Login'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
