import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function ContractsSection() {
  const { team } = useGame()
  const [contracts, setContracts] = useState([])
  const [loading, setLoading] = useState(false)
  const [showNegotiationModal, setShowNegotiationModal] = useState(false)
  const [selectedPlayer, setSelectedPlayer] = useState(null)
  const [proposedSalary, setProposedSalary] = useState(0)
  const [proposedContractLength, setProposedContractLength] = useState(3)
  const [negotiationResult, setNegotiationResult] = useState(null)
  const [attemptHistory, setAttemptHistory] = useState([]) // Array of {success: boolean, salaryFeedback, contractFeedback}
  const [message, setMessage] = useState('')

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

  const openNegotiation = (player) => {
    if (player.contractLength >= 4) {
      setMessage('❌ Spieler mit 4+ Saisons Restvertrag können nicht verlängert werden')
      return
    }
    
    setSelectedPlayer(player)
    // Initiales Angebot: Aktuelles Gehalt + 15%
    setProposedSalary(Math.round(player.salary * 1.15))
    // Setze initiale Laufzeit auf mindestens currentLength + 1
    setProposedContractLength(Math.min(5, player.contractLength + 1))
    setNegotiationResult(null)
    setMessage('')
    setShowNegotiationModal(true)
    
    // Lade Verhandlungshistorie
    fetch(`${API_BASE}/api/v2/players/${player.id}/negotiation-history`)
      .then(r => r.json())
      .then(data => {
        // Erstelle Attempt-History aus Backend-Daten
        const history = []
        for (let i = 0; i < data.attemptCount; i++) {
          history.push({
            success: false,
            aborted: data.failed && i === data.attemptCount - 1,
            salaryFeedback: 'unhappy',
            contractFeedback: 'unhappy'
          })
        }
        setAttemptHistory(history)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Historie:', e)
        setAttemptHistory([])
      })
  }

  const makeOffer = () => {
    if (attemptHistory.length >= 3) {
      setMessage('❌ Maximale Anzahl Versuche erreicht')
      return
    }

    fetch(`${API_BASE}/api/v2/players/${selectedPlayer.id}/negotiate-contract`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        proposedSalary: proposedSalary,
        proposedContractLength: proposedContractLength
      })
    })
      .then(r => r.json())
      .then(data => {
        setNegotiationResult(data)
        
        // Erstelle neue History basierend auf attemptCount vom Backend
        const newHistory = []
        for (let i = 0; i < data.attemptCount; i++) {
          newHistory.push({
            success: i === data.attemptCount - 1 && data.accepted,
            aborted: i === data.attemptCount - 1 && data.negotiationAborted,
            salaryFeedback: i === data.attemptCount - 1 ? data.salaryFeedback : 'unhappy',
            contractFeedback: i === data.attemptCount - 1 ? data.contractFeedback : 'unhappy'
          })
        }
        setAttemptHistory(newHistory)

        if (data.accepted) {
          // Erfolg! Aktualisiere Contracts und schließe Modal nach kurzer Verzögerung
          setTimeout(() => {
            loadContracts()
            setShowNegotiationModal(false)
            setMessage(`✅ ${selectedPlayer.name} hat den Vertrag unterschrieben!`)
          }, 2000)
        } else if (data.negotiationAborted) {
          // Verhandlung abgebrochen
          setTimeout(() => {
            setShowNegotiationModal(false)
            setMessage(`❌ ${selectedPlayer.name} hat die Verhandlungen abgebrochen!`)
          }, 2500)
        }
      })
      .catch(e => {
        console.error('Fehler:', e)
        setNegotiationResult({
          accepted: false,
          negotiationAborted: true,
          message: 'Fehler beim Verhandeln',
          salaryFeedback: 'unhappy',
          contractFeedback: 'unhappy',
          attemptCount: attemptHistory.length + 1
        })
      })
  }

  const closeModal = () => {
    setShowNegotiationModal(false)
    setNegotiationResult(null)
    setAttemptHistory([])
  }

  const getSmiley = (feedback) => {
    if (feedback === 'happy') return '😊'
    if (feedback === 'neutral') return '😐'
    if (feedback === 'unhappy') return '😢'
    return '❓'
  }

  const getSmileyColor = (feedback) => {
    if (feedback === 'happy') return '#10b981'
    if (feedback === 'neutral') return '#f59e0b'
    if (feedback === 'unhappy') return '#ef4444'
    return '#666'
  }

  const getSeasonColor = (seasons) => {
    if (seasons >= 3) return '#10b981'
    if (seasons === 2) return '#f59e0b'
    if (seasons === 1) return '#ef4444'
    return '#dc2626'
  }

  const getSeasonText = (seasons) => {
    if (seasons === 0) return '⚠️ Keine'
    if (seasons === 1) return '⚠️ 1 Saison'
    return `✓ ${seasons} Saisons`
  }

  const formatCurrency = (value) => {
    if (value >= 1000000) return '€' + (value / 1000000).toFixed(1) + 'M'
    if (value >= 1000) return '€' + (value / 1000).toFixed(0) + 'K'
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
                      {formatCurrency(player.salary || 0)}/Spieltag
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
                  disabled={player.contractLength >= 4}
                  title={player.contractLength >= 4 ? 'Spieler mit 4+ Saisons können nicht verlängert werden' : ''}
                  style={{
                    width: '100%',
                    fontSize: '0.85em',
                    padding: '6px 8px',
                    opacity: player.contractLength >= 4 ? 0.4 : 1,
                    cursor: player.contractLength >= 4 ? 'not-allowed' : 'pointer',
                    backgroundColor: player.contractLength >= 4 ? '#666' : undefined
                  }}
                >
                  {player.contractLength >= 4 ? '✓ Langer Vertrag' : '🤝 Verlängern'}
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
              💡 <strong>Tipp:</strong> Spieler sortiert nach Rating. Klicke auf "Verlängern" um Vertragsverhandlungen zu starten. Du hast maximal 3 Versuche pro Spieler!
            </div>
          </div>
        </div>
      )}

      {/* Verhandlungs-Modal */}
      {showNegotiationModal && selectedPlayer && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }} onClick={() => !negotiationResult && closeModal()}>
          <div style={{
            backgroundColor: '#1e293b',
            border: '2px solid rgba(255,255,255,0.2)',
            borderRadius: '12px',
            padding: '24px',
            maxWidth: '500px',
            width: '90%',
            maxHeight: '90vh',
            overflowY: 'auto'
          }} onClick={(e) => e.stopPropagation()}>
            <h3 style={{ marginTop: 0, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              🤝 Vertragsverhandlung
            </h3>

            {/* Spieler Info */}
            <div style={{
              padding: '12px',
              background: 'rgba(255,255,255,0.05)',
              borderRadius: '8px',
              marginBottom: '16px'
            }}>
              <div style={{ fontWeight: 'bold', fontSize: '1.1em', marginBottom: '4px' }}>
                {selectedPlayer.name}
              </div>
              <div style={{ fontSize: '0.85em', color: '#9ca3af' }}>
                {selectedPlayer.position} • ⭐ {selectedPlayer.rating} • {selectedPlayer.age} Jahre
              </div>
              <div style={{ fontSize: '0.85em', color: '#9ca3af', marginTop: '4px' }}>
                Aktuell: {formatCurrency(selectedPlayer.salary)}/Spieltag • {selectedPlayer.contractLength} Saisons
              </div>
            </div>

            {/* Versuche-Balken */}
            <div style={{ marginBottom: '16px' }}>
              <div className="muted" style={{ fontSize: '0.85em', marginBottom: '8px' }}>
                Verhandlungsversuche ({attemptHistory.length}/3)
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                {[0, 1, 2].map(index => {
                  const attempt = attemptHistory[index]
                  let bgColor = '#374151' // Grau (nicht verwendet)
                  if (attempt) {
                    if (attempt.aborted) {
                      bgColor = '#7f1d1d' // Dunkelrot (abgebrochen)
                    } else if (attempt.success) {
                      bgColor = '#10b981' // Grün (Erfolg)
                    } else {
                      bgColor = '#ef4444' // Rot (Fehlschlag)
                    }
                  }
                  return (
                    <div key={index} style={{
                      flex: 1,
                      height: '8px',
                      background: bgColor,
                      borderRadius: '4px',
                      transition: 'all 0.3s ease'
                    }} />
                  )
                })}
              </div>
            </div>

            {!negotiationResult ? (
              <div>
                {/* Gehalts-Input */}
                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.9em' }}>
                    💰 Gehalt pro Spieltag
                  </label>
                  <input
                    type="text"
                    value={proposedSalary.toLocaleString('de-DE')}
                    onChange={(e) => {
                      // Entferne alle Nicht-Ziffern
                      const rawValue = e.target.value.replace(/\D/g, '')
                      const numValue = rawValue === '' ? 0 : parseInt(rawValue)
                      setProposedSalary(numValue)
                    }}
                    onFocus={(e) => {
                      // Wenn 0, selektiere alles damit es beim Tippen ersetzt wird
                      if (proposedSalary === 0) {
                        e.target.select()
                      }
                    }}
                    style={{
                      width: '100%',
                      padding: '8px',
                      borderRadius: '4px',
                      border: '1px solid rgba(255,255,255,0.2)',
                      background: 'rgba(0,0,0,0.3)',
                      color: '#fff',
                      fontSize: '0.95em'
                    }}
                  />
                  <div style={{ fontSize: '0.8em', color: '#9ca3af', marginTop: '4px' }}>
                    {formatCurrency(proposedSalary)}
                  </div>
                </div>

                {/* Vertragslaufzeit-Input */}
                <div style={{ marginBottom: '20px' }}>
                  <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.9em' }}>
                    📅 Vertragslaufzeit (nur Verlängerung möglich)
                  </label>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    {[2, 3, 4, 5].map(length => {
                      const isDisabled = length <= selectedPlayer.contractLength
                      return (
                        <button
                          key={length}
                          onClick={() => !isDisabled && setProposedContractLength(length)}
                          disabled={isDisabled}
                          title={isDisabled ? `Aktuell: ${selectedPlayer.contractLength} Saisons - Nur höhere Laufzeiten möglich` : ''}
                          style={{
                            flex: 1,
                            padding: '10px',
                            borderRadius: '6px',
                            border: proposedContractLength === length ? '2px solid #3b82f6' : '1px solid rgba(255,255,255,0.2)',
                            background: isDisabled ? 'rgba(100,100,100,0.3)' : (proposedContractLength === length ? 'rgba(59, 130, 246, 0.2)' : 'rgba(0,0,0,0.3)'),
                            color: isDisabled ? '#666' : '#fff',
                            cursor: isDisabled ? 'not-allowed' : 'pointer',
                            fontWeight: proposedContractLength === length ? 'bold' : 'normal',
                            fontSize: '0.9em',
                            opacity: isDisabled ? 0.5 : 1
                          }}
                        >
                          {length} {length === 1 ? 'Saison' : 'Saisons'}
                        </button>
                      )
                    })}
                  </div>
                </div>

                {/* Buttons */}
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    onClick={makeOffer}
                    disabled={attemptHistory.length >= 3 || (negotiationResult && negotiationResult.negotiationAborted)}
                    style={{
                      flex: 1,
                      padding: '12px',
                      backgroundColor: '#10b981',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: attemptHistory.length >= 3 ? 'not-allowed' : 'pointer',
                      fontWeight: 'bold',
                      fontSize: '0.95em',
                      opacity: attemptHistory.length >= 3 ? 0.5 : 1
                    }}
                  >
                    📝 Angebot machen
                  </button>
                  <button
                    onClick={closeModal}
                    style={{
                      padding: '12px 20px',
                      backgroundColor: '#6b7280',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontSize: '0.95em'
                    }}
                  >
                    ❌ Abbrechen
                  </button>
                </div>
              </div>
            ) : (
              <div>
                {/* Feedback nach Angebot */}
                <div style={{
                  padding: '16px',
                  background: negotiationResult.accepted ? 'rgba(16, 185, 129, 0.1)' : 
                             negotiationResult.negotiationAborted ? 'rgba(239, 68, 68, 0.1)' : 
                             'rgba(245, 158, 11, 0.1)',
                  borderRadius: '8px',
                  border: '2px solid ' + (negotiationResult.accepted ? '#10b981' : 
                                         negotiationResult.negotiationAborted ? '#ef4444' : '#f59e0b'),
                  marginBottom: '16px'
                }}>
                  {/* Spieler Reaktion */}
                  <div style={{ textAlign: 'center', marginBottom: '16px' }}>
                    <div style={{ fontSize: '3em', marginBottom: '8px' }}>
                      {negotiationResult.accepted ? '🎉' : 
                       negotiationResult.negotiationAborted ? '🚫' : '🤔'}
                    </div>
                    <div style={{ 
                      fontWeight: 'bold', 
                      fontSize: '1.1em',
                      color: negotiationResult.accepted ? '#10b981' : 
                             negotiationResult.negotiationAborted ? '#ef4444' : '#f59e0b'
                    }}>
                      {negotiationResult.message}
                    </div>
                  </div>

                  {/* Feedback-Details nur wenn nicht abgebrochen und nicht akzeptiert */}
                  {!negotiationResult.accepted && !negotiationResult.negotiationAborted && (
                    <div style={{ display: 'flex', gap: '16px', justifyContent: 'center' }}>
                      <div style={{ textAlign: 'center' }}>
                        <div style={{ fontSize: '0.85em', color: '#9ca3af', marginBottom: '4px' }}>
                          Gehalt
                        </div>
                        <div style={{ 
                          fontSize: '2.5em',
                          color: getSmileyColor(negotiationResult.salaryFeedback)
                        }}>
                          {getSmiley(negotiationResult.salaryFeedback)}
                        </div>
                      </div>
                      <div style={{ textAlign: 'center' }}>
                        <div style={{ fontSize: '0.85em', color: '#9ca3af', marginBottom: '4px' }}>
                          Vertragslaufzeit
                        </div>
                        <div style={{ 
                          fontSize: '2.5em',
                          color: getSmileyColor(negotiationResult.contractFeedback)
                        }}>
                          {getSmiley(negotiationResult.contractFeedback)}
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Buttons */}
                {negotiationResult.accepted || negotiationResult.negotiationAborted ? (
                  <button
                    onClick={closeModal}
                    style={{
                      width: '100%',
                      padding: '12px',
                      backgroundColor: '#3b82f6',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      fontSize: '0.95em'
                    }}
                  >
                    Schließen
                  </button>
                ) : attemptHistory.length < 3 ? (
                  <button
                    onClick={() => setNegotiationResult(null)}
                    style={{
                      width: '100%',
                      padding: '12px',
                      backgroundColor: '#f59e0b',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      fontSize: '0.95em'
                    }}
                  >
                    🔄 Neues Angebot machen ({3 - attemptHistory.length} Versuche übrig)
                  </button>
                ) : (
                  <button
                    onClick={closeModal}
                    style={{
                      width: '100%',
                      padding: '12px',
                      backgroundColor: '#ef4444',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      fontSize: '0.95em'
                    }}
                  >
                    Verhandlung beenden (Keine Versuche mehr)
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
