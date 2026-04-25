import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function YouthAcademySection() {
  const { team } = useGame()
  const [academy, setAcademy] = useState([])
  const [loading, setLoading] = useState(false)

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  useEffect(() => {
    if (team && team.id) {
      loadAcademy()
    }
  }, [team])

  const loadAcademy = () => {
    setLoading(true)
    fetch(`${API_BASE}/api/v2/scouts/academy/team/${team.id}`)
      .then(r => r.json())
      .then(data => {
        setAcademy(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Akademie:', e)
        setLoading(false)
      })
  }

  return (
    <div>
      <h4>🏫 Jugenakademie</h4>

      {loading ? (
        <p className="muted">Lädt...</p>
      ) : academy.length === 0 ? (
        <p className="muted">Keine Jugenspieler in der Akademie</p>
      ) : (
        <div style={{ 
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
          gap: '12px'
        }}>
          {academy.map(player => (
            <div key={player.id} style={{
              padding: '12px',
              border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: '8px',
              background: 'rgba(0,0,0,0.2)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <div>
                  <div style={{ fontWeight: 'bold', fontSize: '0.95em' }}>
                    {player.name}
                  </div>
                  <div className="muted" style={{ fontSize: '0.85em' }}>
                    {player.position} • {player.country}
                  </div>
                </div>
              </div>

              <div style={{ 
                display: 'grid', 
                gridTemplateColumns: '1fr 1fr 1fr',
                gap: '8px',
                padding: '8px',
                background: 'rgba(255,255,255,0.05)',
                borderRadius: '6px',
                marginBottom: '8px',
                fontSize: '0.9em',
                textAlign: 'center'
              }}>
                <div>
                  <div className="muted" style={{ fontSize: '0.75em' }}>Alter</div>
                  <div style={{ fontWeight: 'bold' }}>{player.age}</div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: '0.75em' }}>Rating</div>
                  <div style={{ color: '#fbbf24', fontWeight: 'bold' }}>{player.rating}</div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: '0.75em' }}>Potential</div>
                  <div style={{ color: '#60a5fa', fontWeight: 'bold' }}>{player.overallPotential}</div>
                </div>
              </div>

              <div className="muted" style={{ fontSize: '0.8em', textAlign: 'center' }}>
                {player.age <= 16 ? '📚 In Ausbildung' : '📈 Bereit für Kader'}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
