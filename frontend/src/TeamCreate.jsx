import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function TeamCreate() {
  const [name, setName] = useState('')
  const [created, setCreated] = useState(null)
  const navigate = useNavigate()

  const submit = (e) => {
    e.preventDefault()
    if (!name.trim()) return
    setCreated(name.trim())
    setName('')
  }

  return (
    <div className="app-container">
      <div className="card">
        <h2>Team erstellen</h2>
        {created ? (
          <div>
            <p>Team „{created}“ wurde erstellt.</p>
            <button className="btn primary" onClick={() => { setCreated(null); navigate('/') }}>Zurück zur Startseite</button>
          </div>
        ) : (
          <form onSubmit={submit} className="form">
            <label>
              Team-Name
              <input className="input" value={name} onChange={e => setName(e.target.value)} />
            </label>
            <div style={{display:'flex', gap:8}}>
              <button className="btn primary" type="submit">Erstellen</button>
              <button className="btn secondary" type="button" onClick={() => navigate('/')}>Abbrechen</button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
