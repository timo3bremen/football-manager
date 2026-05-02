import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function Contracts() {
  const { team } = useGame()
  const [contracts, setContracts] = useState([])
  const [loading, setLoading] = useState(false)
  const [extendingId, setExtendingId] = useState(null)

 const API_BASE = 'http://192.168.178.21:8080'

  // Lade Verträge beim Laden oder wenn Team sich ändert
  useEffect(() => {
    if (team && team.id) {
      loadContracts()
    }
  }, [team])

  const loadContracts = () => {
    if (!team || !team.id) return

    setLoading(true)
    fetch(`${API_BASE}/api/v2/players/team/${team.id}/contracts`)
      .then(r => r.json())
      .then(data => {
        setContracts(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Verträge:', e)
        setLoading(false)
      })
  }

  const extendContract = (playerId, playerName) => {
    setExtendingId(playerId)
    fetch(`${API_BASE}/api/v2/players/${playerId}/extend-contract`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          // Aktualisiere den Spieler in der Liste
          setContracts(contracts.map(p => 
            p.id === playerId ? data.player : p
          ))
          console.log(`✓ Vertrag von ${playerName} verlängert`)
        } else {
          alert('Fehler: ' + data.message)
        }
        setExtendingId(null)
      })
      .catch(e => {
        console.error('Fehler beim Verlängern des Vertrags:', e)
        alert('Fehler beim Verlängern des Vertrags')
        setExtendingId(null)
      })
  }

  const getSeasonColor = (seasons) => {
    if (seasons >= 3) return '#10b981' // Grün - gut
    if (seasons === 2) return '#f59e0b' // Gelb/Orange - warnung
    if (seasons === 1) return '#ef4444' // Rot - kritisch
    return '#dc2626' // Dunkelrot - auslaufend
  }

  const getSeasonText = (seasons) => {
    if (seasons === 0) return '⚠️ Keine'
    if (seasons === 1) return '⚠️ 1 Saison'
    return `✓ ${seasons} Saisons`
  }

  return (
    <div>
      <h3>Spielerverträge</h3>
      
      {loading ? (
        <p className="muted">Lädt Verträge...</p>
      ) : contracts.length === 0 ? (
        <p className="muted">Keine Spieler gefunden</p>
      ) : (
        <div className="card">
          <div style={{ 
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '12px'
          }}>
            {contracts.map(player => (
              <div key={player.id} style={{
                padding: '12px',
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: '8px',
                background: 'rgba(0,0,0,0.2)'
              }}>
                {/* Kopfzeile: Name und Position */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '8px' }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 'bold', fontSize: '0.95em' }}>
                      {player.name}
                    </div>
                    <div className="muted" style={{ fontSize: '0.8em' }}>
                      {player.position} • {player.country}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '0.85em', color: '#fbbf24' }}>
                      ⭐ {player.rating}
                    </div>
                    <div style={{ fontSize: '0.75em', color: '#9ca3af' }}>
                      Alter: {player.age}
                    </div>
                  </div>
                </div>

                {/* Vertrags-Informationen */}
                <div style={{
                  padding: '8px',
                  background: 'rgba(255,255,255,0.05)',
                  borderRadius: '6px',
                  marginBottom: '8px'
                }}>
                  {/* Verbleibende Saisons */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                    <div className="muted" style={{ fontSize: '0.85em' }}>Laufzeit:</div>
                    <div style={{ 
                      color: getSeasonColor(player.contractLength),
                      fontWeight: 'bold',
                      fontSize: '0.9em'
                    }}>
                      {getSeasonText(player.contractLength)}
                    </div>
                  </div>

                  {/* Gehalt */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                    <div className="muted" style={{ fontSize: '0.85em' }}>Gehalt:</div>
                    <div style={{ fontSize: '0.9em', color: '#10b981' }}>
                      €{(player.salary || 0).toLocaleString()}
                    </div>
                  </div>

                  {/* Marktwert */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div className="muted" style={{ fontSize: '0.85em' }}>Marktwert:</div>
                    <div style={{ fontSize: '0.9em', color: '#60a5fa' }}>
                      €{(player.marketValue || 0).toLocaleString()}
                    </div>
                  </div>
                </div>

                {/* Verlängerungs-Button */}
                <button
                  className="btn primary"
                  onClick={() => extendContract(player.id, player.name)}
                  disabled={extendingId === player.id}
                  style={{
                    width: '100%',
                    fontSize: '0.85em',
                    padding: '6px 8px',
                    opacity: extendingId === player.id ? 0.6 : 1
                  }}
                >
                  {extendingId === player.id ? '⏳ Verlängere...' : '🤝 Verlängern (+1 Saison)'}
                </button>

                {/* Info-Text wenn Vertrag bald ausläuft */}
                {player.contractLength <= 1 && (
                  <div className="muted" style={{ 
                    fontSize: '0.75em', 
                    marginTop: '6px',
                    padding: '6px',
                    background: 'rgba(239, 68, 68, 0.1)',
                    borderRadius: '4px',
                    borderLeft: '2px solid #ef4444'
                  }}>
                    ⚠️ Vertrag läuft bald aus. Verlängerung empfohlen!
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Info-Box */}
          <div style={{
            marginTop: '16px',
            padding: '12px',
            background: 'rgba(59, 130, 246, 0.1)',
            borderRadius: '8px',
            borderLeft: '3px solid #3b82f6'
          }}>
            <div className="muted" style={{ fontSize: '0.85em' }}>
              💡 <strong>Tipp:</strong> Spieler mit ablaufenden Verträgen können verkauft oder deren Verträge verlängert werden. 
              Eine Vertragsverlängerung um 1 Saison erhöht die Vertragslänge um 1 Jahr.
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
