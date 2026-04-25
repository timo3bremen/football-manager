import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function ContractsSection() {
  const { team } = useGame()
  const [contracts, setContracts] = useState([])
  const [loading, setLoading] = useState(false)
  const [showNegotiationModal, setShowNegotiationModal] = useState(false)
  const [selectedPlayer, setSelectedPlayer] = useState(null)
  const [negotiationOffer, setNegotiationOffer] = useState(null)
  const [playerResponse, setPlayerResponse] = useState(null)
  const [message, setMessage] = useState('')

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

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

  const generateNegotiationOffer = (player) => {
    // Gehalt: 5-20% höher als vorher
    const salaryIncrease = 0.05 + Math.random() * 0.15
    const newSalary = Math.round(player.salary * (1 + salaryIncrease))
    
    // Laufzeit: +1 bis (5 - aktuelle Laufzeit)
    const maxExtension = Math.max(1, 5 - player.contractLength)
    const newLength = player.contractLength + (1 + Math.floor(Math.random() * maxExtension))
    
    return {
      player: player,
      newSalary: newSalary,
      newLength: newLength,
      salaryIncrease: Math.round(salaryIncrease * 100)
    }
  }

  const openNegotiation = (player) => {
    if (player.contractLength >= 5) {
      setMessage('❌ Spieler hat bereits maximalen 5-Saisons-Vertrag')
      return
    }
    
    const offer = generateNegotiationOffer(player)
    setSelectedPlayer(player)
    setNegotiationOffer(offer)
    setPlayerResponse(null)
    setMessage('')
    setShowNegotiationModal(true)
  }

  const handleNegotiationResponse = (accepted) => {
    if (accepted) {
      // Spieler akzeptiert
      setPlayerResponse({
        accepted: true,
        message: `✅ ${selectedPlayer.name} nimmt das Angebot an!`
      })
      
      // Sende zum Backend
      fetch(`${API_BASE}/api/v2/players/${selectedPlayer.id}/extend-contract`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          newSalary: negotiationOffer.newSalary,
          newContractLength: negotiationOffer.newLength
        })
      })
        .then(r => r.json())
        .then(data => {
          if (data.success) {
            setContracts(contracts.map(p => 
              p.id === selectedPlayer.id ? data.player : p
            ))
            setTimeout(() => {
              setShowNegotiationModal(false)
              setMessage(`✅ Vertrag von ${selectedPlayer.name} verlängert!`)
            }, 1500)
          } else {
            setPlayerResponse({
              accepted: false,
              message: '❌ Fehler beim Speichern: ' + data.message
            })
          }
        })
        .catch(e => {
          console.error('Fehler:', e)
          setPlayerResponse({
            accepted: false,
            message: '❌ Fehler beim Verlängern des Vertrags'
          })
        })
    } else {
      // Spieler lehnt ab
      setPlayerResponse({
        accepted: false,
        message: `❌ ${selectedPlayer.name} lehnt das Angebot ab. Zu niedrig?`
      })
      setTimeout(() => setShowNegotiationModal(false), 2000)
    }
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

  const formatCurrency = (value) => {
    if (value >= 1000000) return '€' + (value / 1000000).toFixed(1) + 'M'
    if (value >= 1000) return '€' + (value / 1000).toFixed(1) + 'K'
    return '€' + value
  }

  return (
    <div>
      <h4>Spielerverträge</h4>
      
      {message && (
        <div style={{
          padding: '10px',
          marginBottom: '10px',
          backgroundColor: message.includes('✅') ? '#2d5016' : '#5d2d2d',
          border: '1px solid ' + (message.includes('✅') ? '#4a7c0c' : '#8b4040'),
          color: message.includes('✅') ? '#90ee90' : '#ff6b6b',
          borderRadius: '4px',
          fontSize: '0.9em'
        }}>
          {message}
        </div>
      )}
      
      {loading ? (
        <p className="muted">Lädt Verträge...</p>
      ) : contracts.length === 0 ? (
        <p className="muted">Keine Spieler gefunden</p>
      ) : (
        <div>
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
                      {formatCurrency(player.salary || 0)}
                    </div>
                  </div>

                  {/* Marktwert */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div className="muted" style={{ fontSize: '0.85em' }}>Marktwert:</div>
                    <div style={{ fontSize: '0.9em', color: '#60a5fa' }}>
                      {formatCurrency(player.marketValue || 0)}
                    </div>
                  </div>
                </div>

                {/* Verlängerungs-Button */}
                <button
                  className="btn primary"
                  onClick={() => openNegotiation(player)}
                  disabled={player.contractLength >= 5}
                  style={{
                    width: '100%',
                    fontSize: '0.85em',
                    padding: '6px 8px',
                    opacity: player.contractLength >= 5 ? 0.4 : 1,
                    cursor: player.contractLength >= 5 ? 'not-allowed' : 'pointer',
                    backgroundColor: player.contractLength >= 5 ? '#666' : undefined
                  }}
                >
                  {player.contractLength >= 5 ? '✓ Max. Vertrag' : '🤝 Verlängern'}
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
              💡 <strong>Tipp:</strong> Klicke auf "Verlängern" um ein Vertragsangebot zu machen. Der Spieler wird mit seinem Gehaltswunsch antworten.
            </div>
          </div>
        </div>
      )}

      {/* Verhandlungs-Modal */}
      {showNegotiationModal && negotiationOffer && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.7)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: '#1a1a1a',
            border: '2px solid #666',
            borderRadius: '8px',
            padding: '20px',
            maxWidth: '400px',
            width: '90%'
          }}>
            <h4 style={{ marginTop: 0, marginBottom: '15px' }}>
              📋 Vertragsverhandlung mit {selectedPlayer.name}
            </h4>

            {!playerResponse ? (
              <div>
                <div style={{
                  backgroundColor: '#1a3a1a',
                  padding: '12px',
                  borderRadius: '6px',
                  marginBottom: '15px'
                }}>
                  <p style={{ margin: '8px 0', fontSize: '0.9em' }}>
                    <strong>Spieler schlägt vor:</strong>
                  </p>
                  <p style={{ margin: '8px 0', color: '#90ee90' }}>
                    💰 Neues Gehalt: {formatCurrency(negotiationOffer.newSalary)}/Saison
                    <br/>
                    <span style={{ fontSize: '0.85em', color: '#70d070' }}>
                      (+{negotiationOffer.salaryIncrease}% zu vorher {formatCurrency(selectedPlayer.salary)})
                    </span>
                  </p>
                  <p style={{ margin: '8px 0', color: '#60a5fa' }}>
                    📅 Neue Laufzeit: {negotiationOffer.newLength} Saisons
                    <br/>
                    <span style={{ fontSize: '0.85em', color: '#4a9eff' }}>
                      (+{negotiationOffer.newLength - selectedPlayer.contractLength} Saisons zu vorher {selectedPlayer.contractLength})
                    </span>
                  </p>
                </div>

                <div style={{
                  display: 'flex',
                  gap: '10px',
                  justifyContent: 'space-between'
                }}>
                  <button
                    onClick={() => handleNegotiationResponse(true)}
                    style={{
                      flex: 1,
                      padding: '10px',
                      backgroundColor: '#10b981',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontWeight: 'bold'
                    }}
                  >
                    ✅ Akzeptieren
                  </button>
                  <button
                    onClick={() => handleNegotiationResponse(false)}
                    style={{
                      flex: 1,
                      padding: '10px',
                      backgroundColor: '#ef4444',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontWeight: 'bold'
                    }}
                  >
                    ❌ Ablehnen
                  </button>
                </div>
              </div>
            ) : (
              <div style={{ textAlign: 'center' }}>
                <div style={{
                  fontSize: '2em',
                  marginBottom: '10px'
                }}>
                  {playerResponse.accepted ? '🎉' : '😔'}
                </div>
                <p style={{
                  color: playerResponse.accepted ? '#90ee90' : '#ff6b6b',
                  marginBottom: '15px',
                  fontSize: '0.95em'
                }}>
                  {playerResponse.message}
                </p>
                {playerResponse.accepted && (
                  <p style={{ fontSize: '0.85em', color: '#aaa' }}>
                    Der Vertrag wird aktualisiert...
                  </p>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
