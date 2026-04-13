import React, { useState } from 'react'
import { useGame } from './GameContext'

export default function Options(){
  const { team, setTeam } = useGame()
  const [teamNameInput, setTeamNameInput] = useState(team?.name || '')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('') // 'success', 'error'

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  const updateTeamName = () => {
    if (!team || !teamNameInput.trim()) {
      setMessageType('error')
      setMessage('Bitte einen Teamnamen eingeben')
      return
    }

    setLoading(true)
    setMessage('')
    
    fetch(`${API_BASE}/api/teams/${team.id}/name`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: teamNameInput.trim() })
    })
      .then(r => {
        if (r.ok) {
          return r.json()
        } else {
          throw new Error('Fehler beim Aktualisieren des Teamnamens')
        }
      })
      .then(data => {
        setTeam(data)
        setMessageType('success')
        setMessage(`✓ Teamname zu "${data.name}" geändert!`)
        setLoading(false)
      })
      .catch(e => {
        setMessageType('error')
        setMessage(`✗ Fehler: ${e.message}`)
        setLoading(false)
      })
  }

  return (
    <div>
      <h3>Optionen</h3>
      <div className="card">
        <h4>Team-Einstellungen</h4>
        
        {team ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
                Teamname
              </label>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  type="text"
                  value={teamNameInput}
                  onChange={(e) => setTeamNameInput(e.target.value)}
                  placeholder="Neuer Teamname"
                  style={{
                    flex: 1,
                    padding: '8px 12px',
                    borderRadius: '4px',
                    border: '1px solid rgba(255,255,255,0.2)',
                    background: 'rgba(0,0,0,0.2)',
                    color: '#fff'
                  }}
                  disabled={loading}
                />
                <button
                  onClick={updateTeamName}
                  disabled={loading}
                  style={{
                    padding: '8px 16px',
                    borderRadius: '4px',
                    border: 'none',
                    background: '#4ade80',
                    color: '#000',
                    fontWeight: 'bold',
                    cursor: loading ? 'not-allowed' : 'pointer',
                    opacity: loading ? 0.6 : 1
                  }}
                >
                  {loading ? '⏳' : '💾'} Speichern
                </button>
              </div>
            </div>

            {message && (
              <div
                style={{
                  padding: 12,
                  borderRadius: 4,
                  background: messageType === 'success' ? 'rgba(52, 211, 153, 0.2)' : 'rgba(248, 113, 113, 0.2)',
                  color: messageType === 'success' ? '#4ade80' : '#ef4444',
                  border: `1px solid ${messageType === 'success' ? '#34d399' : '#f87171'}`
                }}
              >
                {message}
              </div>
            )}

            <div className="muted" style={{ marginTop: 8, fontSize: '0.9em' }}>
              <strong>Aktueller Teamname:</strong> {team.name}
            </div>
          </div>
        ) : (
          <p className="muted">Kein aktives Team</p>
        )}
      </div>
    </div>
  )
}
