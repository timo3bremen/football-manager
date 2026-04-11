import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGame } from './GameContext'

export default function StartGame(){
  const [teamName, setTeamName] = useState('')
  const { createTeam } = useGame()
  const navigate = useNavigate()

  function submit(e){
    e.preventDefault()
    if (!teamName.trim()) return
    createTeam(teamName.trim())
    navigate('/game')
  }

  return (
    <div className="app-container">
      <div className="card">
        <h2>Spiel Starten — Team erstellen</h2>
        <form onSubmit={submit} className="form">
          <label>
            Teamname
            <input className="input" value={teamName} onChange={e=>setTeamName(e.target.value)} />
          </label>
          <div style={{display:'flex',gap:8}}>
            <button className="btn primary" type="submit">Team erstellen und starten</button>
            <button className="btn secondary" type="button" onClick={()=>navigate('/')}>Abbrechen</button>
          </div>
        </form>
      </div>
    </div>
  )
}
