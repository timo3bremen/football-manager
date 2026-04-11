import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGame } from './GameContext'

export default function Auth(){
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [teamName, setTeamName] = useState('')
  const [error, setError] = useState(null)
  const { setTeam } = useGame()
  const navigate = useNavigate()

  async function register(){
    setError(null)
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
  }

  async function login(){
    setError(null)
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
  }

  return (
    <div className="app-container">
      <div className="card">
        <h2>{mode === 'login' ? 'Login' : 'Registrieren'}</h2>
        <div className="form">
          <input className="input" value={username} onChange={e=>setUsername(e.target.value)} placeholder="Benutzername" />
          <input className="input" type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="Passwort" />
          {mode==='register' && <input className="input" value={teamName} onChange={e=>setTeamName(e.target.value)} placeholder="Teamname" />}
          {error ? <div style={{color:'#fda4af'}}>{error}</div> : null}
          <div style={{display:'flex',gap:8}}>
            {mode==='login'
              ? <button className="btn primary" onClick={login}>Login</button>
              : <button className="btn primary" onClick={register}>Registrieren</button>
            }
            <button className="btn secondary" onClick={()=>setMode(mode==='login'?'register':'login')}>{mode==='login' ? 'Registrieren' : 'Login'}</button>
          </div>
        </div>
      </div>
    </div>
  )
}
