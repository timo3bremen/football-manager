import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function Home() {
  const [action, setAction] = useState(null)
  const navigate = useNavigate()

  const handle = (name) => {
    setAction(name)
    switch (name) {
      case 'team-create': navigate('/team-create'); break
      case 'settings': navigate('/settings'); break
      case 'join-team': navigate('/join-team'); break
      case 'start-game': navigate('/start-game'); break
      default: break
    }
  }

  return (
    <div className="app-container">
      <header className="header">
        <div className="brand">
          <div className="logo">FM</div>
          <div>
            <h1 className="title">Startseite</h1>
            <div className="subtitle">Willkommen! Wähle eine Aktion</div>
          </div>
        </div>
      </header>

      <div className="card home-cta">
        <div className="buttons-row">
          <button className="btn primary" onClick={() => handle('team-create')}>Team Erstellen</button>
          <button className="btn secondary" onClick={() => handle('settings')}>Einstellungen</button>
          <button className="btn primary" onClick={() => handle('join-team')}>Team Beitreten</button>
          <button className="btn secondary" onClick={() => handle('start-game')}>Spiel Starten</button>
            <button className="btn ghost" onClick={() => navigate('/auth')}>Login / Registrieren</button>
            <button className="btn ghost" onClick={() => navigate('/admin')}>Admin</button>
        </div>

        {action && (
          <div className="panel">
            <strong>Ausgewählte Aktion:</strong> <span className="muted">{action}</span>
            <div style={{marginTop:8}}>
              <button className="btn ghost" onClick={() => setAction(null)}>Zurück</button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
