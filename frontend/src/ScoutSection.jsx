import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function ScoutSection() {
  const { team, balance } = useGame()
  const [activeScout, setActiveScout] = useState(null)
  const [scoutedPlayers, setScoutedPlayers] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedRegion, setSelectedRegion] = useState('WestEuropa')
  const [selectedDays, setSelectedDays] = useState(1)
  const [showStartModal, setShowStartModal] = useState(false)

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  const REGIONS = [
    'WestEuropa', 'SüdostAsien', 'SüdAmerika', 'Nordamerika', 
    'Osteuropa', 'Afrika', 'AustralienOzeanien', 'Nahost'
  ]

  // Lade Scout-Daten beim Laden
  useEffect(() => {
    if (team && team.id) {
      loadScoutData()
    }
  }, [team])

  const loadScoutData = () => {
    setLoading(true)
    
    // Lade aktiven Scout
    fetch(`${API_BASE}/api/v2/scouts/team/${team.id}`)
      .then(r => r.json())
      .then(data => {
        setActiveScout(data.hasActiveScout === false ? null : data)
      })
      .catch(e => console.error('Fehler beim Laden des Scouts:', e))
    
    // Lade gescoutete Spieler
    fetch(`${API_BASE}/api/v2/scouts/players/team/${team.id}`)
      .then(r => r.json())
      .then(data => {
        setScoutedPlayers(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Spieler:', e)
        setLoading(false)
      })
  }

  const startScout = () => {
    const cost = selectedDays * 50000
    if (balance < cost) {
      alert(`Nicht genug Budget! Benötigt: ${cost.toLocaleString()}€`)
      return
    }

    fetch(`${API_BASE}/api/v2/scouts/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        teamId: team.id,
        region: selectedRegion,
        days: selectedDays
      })
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          alert(`Scout gestartet in ${selectedRegion}!`)
          setShowStartModal(false)
          loadScoutData()
        } else {
          alert('Fehler: ' + data.error)
        }
      })
      .catch(e => console.error('Fehler:', e))
  }

  const recruitPlayer = (playerId) => {
    fetch(`${API_BASE}/api/v2/scouts/recruit/${playerId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          alert('Spieler verpflichtet!')
          loadScoutData()
        } else {
          alert('Fehler: ' + data.error)
        }
      })
      .catch(e => console.error('Fehler:', e))
  }

  const recruitToAcademy = (playerId) => {
    fetch(`${API_BASE}/api/v2/scouts/recruit-academy/${playerId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          alert('Spieler zur Akademie hinzugefügt!')
          loadScoutData()
        } else {
          alert('Fehler: ' + data.error)
        }
      })
      .catch(e => console.error('Fehler:', e))
  }

  const rejectPlayer = (playerId) => {
    if (!confirm('Spieler wirklich ablehnen und löschen?')) return
    
    fetch(`${API_BASE}/api/v2/scouts/${playerId}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' }
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          alert('Spieler abgelehnt!')
          loadScoutData()
        } else {
          alert('Fehler: ' + data.error)
        }
      })
      .catch(e => console.error('Fehler:', e))
  }

  return (
    <div>
      <h4>🔍 Scouting</h4>

      {loading ? (
        <p className="muted">Lädt...</p>
      ) : (
        <>
          {/* Scout Status */}
          <div className="card" style={{ marginBottom: 16 }}>
            {activeScout ? (
              <div>
                <strong>Aktiver Scout: {activeScout.region}</strong>
                <div className="muted" style={{ fontSize: '0.9em', marginTop: 8 }}>
                  ⏱️ Verbleibende Tage: {activeScout.daysRemaining}
                </div>
              </div>
            ) : (
              <div>
                <div className="muted">Kein aktiver Scout</div>
                <button 
                  className="btn primary"
                  onClick={() => setShowStartModal(true)}
                  style={{ marginTop: 8 }}
                >
                  🚀 Scout starten
                </button>
              </div>
            )}
          </div>

          {/* Modal zum Scout starten */}
          {showStartModal && (
            <div className="modal-backdrop" onClick={() => setShowStartModal(false)}>
              <div className="modal" onClick={e => e.stopPropagation()}>
                <h4>Scout starten</h4>
                <p className="muted">Kosten: 50.000€ pro Tag</p>

                <div style={{ marginBottom: 12 }}>
                  <label>Region:</label>
                  <select 
                    value={selectedRegion}
                    onChange={e => setSelectedRegion(e.target.value)}
                    className="input"
                    style={{ marginTop: 4, width: '100%' }}
                  >
                    {REGIONS.map(r => <option key={r} value={r}>{r}</option>)}
                  </select>
                </div>

                <div style={{ marginBottom: 12 }}>
                  <label>Tage (max 7):</label>
                  <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                    {[1, 2, 3, 4, 5, 6, 7].map(d => (
                      <button
                        key={d}
                        onClick={() => setSelectedDays(d)}
                        style={{
                          padding: '6px 12px',
                          background: selectedDays === d ? '#4f46e5' : 'transparent',
                          border: '1px solid ' + (selectedDays === d ? '#4f46e5' : '#666'),
                          color: '#fff',
                          cursor: 'pointer',
                          borderRadius: 4,
                          flex: 1
                        }}
                      >
                        {d}
                      </button>
                    ))}
                  </div>
                </div>

                <div style={{ 
                  padding: 8, 
                  background: 'rgba(79, 70, 229, 0.1)', 
                  borderRadius: 4,
                  marginBottom: 12
                }}>
                  <strong>Kosten: {(selectedDays * 50000).toLocaleString()}€</strong>
                  <div className="muted" style={{ fontSize: '0.85em', marginTop: 4 }}>
                    Budget: {balance.toLocaleString()}€
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn primary" onClick={startScout} style={{ flex: 1 }}>
                    Starten
                  </button>
                  <button className="btn secondary" onClick={() => setShowStartModal(false)} style={{ flex: 1 }}>
                    Abbrechen
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Gescoutete Spieler */}
          <div className="card">
            <h5>Gescoutete Jugenspieler ({scoutedPlayers.length})</h5>
            {scoutedPlayers.length === 0 ? (
              <p className="muted">Keine Spieler gescoutet</p>
            ) : (
              <div style={{ 
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                gap: '12px'
              }}>
                {scoutedPlayers.map(player => (
                  <div key={player.id} style={{
                    padding: '12px',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: '8px',
                    background: 'rgba(0,0,0,0.2)'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                      <div>
                        <div style={{ fontWeight: 'bold' }}>{player.name}</div>
                        <div className="muted" style={{ fontSize: '0.85em' }}>
                          {player.position} • {player.country}
                        </div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '0.85em', color: '#fbbf24' }}>Alter: {player.age}</div>
                      </div>
                    </div>

                    <div style={{ 
                      display: 'grid', 
                      gridTemplateColumns: '1fr 1fr',
                      gap: '8px',
                      padding: '8px',
                      background: 'rgba(255,255,255,0.05)',
                      borderRadius: '6px',
                      marginBottom: '8px',
                      fontSize: '0.9em'
                    }}>
                      <div>
                        <div className="muted" style={{ fontSize: '0.8em' }}>Rating</div>
                        <div style={{ color: '#fbbf24', fontWeight: 'bold' }}>{player.rating}</div>
                      </div>
                      <div>
                        <div className="muted" style={{ fontSize: '0.8em' }}>Potential</div>
                        <div style={{ color: '#60a5fa', fontWeight: 'bold' }}>{player.overallPotential}</div>
                      </div>
                    </div>

                    {player.recruited ? (
                      <div style={{ 
                        padding: '6px 8px',
                        background: '#10b981',
                        borderRadius: '4px',
                        fontSize: '0.85em',
                        textAlign: 'center'
                      }}>
                        ✓ Verpflichtet
                      </div>
                    ) : player.age <= 16 ? (
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button 
                          className="btn primary"
                          onClick={() => recruitToAcademy(player.id)}
                          style={{ flex: 1, fontSize: '0.8em', padding: '6px 4px' }}
                        >
                          🏫 Akademie
                        </button>
                        <button 
                          className="btn secondary"
                          onClick={() => rejectPlayer(player.id)}
                          style={{ flex: 1, fontSize: '0.8em', padding: '6px 4px' }}
                        >
                          ✕ Ablehnen
                        </button>
                      </div>
                    ) : (
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button 
                          className="btn primary"
                          onClick={() => recruitPlayer(player.id)}
                          style={{ flex: 1, fontSize: '0.8em', padding: '6px 4px' }}
                        >
                          💰 Kader (100k€)
                        </button>
                        <button 
                          className="btn secondary"
                          onClick={() => rejectPlayer(player.id)}
                          style={{ flex: 1, fontSize: '0.8em', padding: '6px 4px' }}
                        >
                          ✕ Ablehnen
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
