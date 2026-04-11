import React from 'react'
import { useNavigate } from 'react-router-dom'

export default function Settings(){
  const navigate = useNavigate()

  return (
    <div className="app-container">
      <div className="card">
        <h2>Einstellungen</h2>
        <p>Hier können später Einstellungen angepasst werden.</p>
        <button className="btn secondary" onClick={() => navigate('/')}>Zurück</button>
      </div>
    </div>
  )
}
