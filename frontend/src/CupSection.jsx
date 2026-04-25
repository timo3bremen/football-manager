import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

/**
 * Cup Section - Integrated with Schedule
 * Shows cup tournament schedule for the selected country
 * Cup matches are held on every 3rd match day (3, 6, 9, 12, etc.)
 */
export default function CupSection({ selectedCountry, leagueId }) {
  const { team } = useGame()
  const [cupTournament, setCupTournament] = useState(null)
  const [cupMatches, setCupMatches] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [selectedRound, setSelectedRound] = useState(1)
  const [teamDetails, setTeamDetails] = useState({})
  const [selectedTeamDetails, setSelectedTeamDetails] = useState(null)
  const [showTeamModal, setShowTeamModal] = useState(false)

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  // Lade Cup-Turnier und Matches wenn Land oder Liga sich ändert
  useEffect(() => {
    if (selectedCountry) {
      loadCupTournament()
    }
  }, [selectedCountry, leagueId])

  const loadCupTournament = () => {
    setLoading(true)
    // Hole aktuellen Cup für dieses Land und die aktuelle Saison
    fetch(`${API_BASE}/api/v2/cup/tournament/${selectedCountry}`)
      .then(r => r.json())
      .then(data => {
        if (data && data.id) {
          setCupTournament(data)
          // Lade automatisch die aktuelle Runde (oder Runde 1 wenn gerade initialisiert)
          const roundToLoad = data.currentRound || 1
          loadCupMatches(data.id, roundToLoad)
          setSelectedRound(roundToLoad)
        } else {
          setMessage('ℹ️ Fehler beim Laden des Pokals')
        }
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden des Pokals:', e)
        setMessage('❌ Fehler beim Laden des Pokals')
        setLoading(false)
      })
  }

  const loadCupMatches = (tournamentId, round) => {
    fetch(`${API_BASE}/api/v2/cup/${tournamentId}/round/${round}`)
      .then(r => r.json())
      .then(data => {
        setCupMatches(Array.isArray(data) ? data : [])
        setSelectedRound(round)
        // Lade Team-Details für alle Teams in den Matches
        loadTeamStrengths(data)
      })
      .catch(e => console.error('Fehler beim Laden der Pokalspiele:', e))
  }

  const loadTeamStrengths = (matches) => {
    matches.forEach(match => {
      // Lade Team-Details wenn nicht bereits geladen
      if (match.homeTeamId && !teamDetails[match.homeTeamId]) {
        fetch(`${API_BASE}/api/teams/${match.homeTeamId}/details`)
          .then(r => r.json())
          .then(data => {
            setTeamDetails(prev => ({
              ...prev,
              [match.homeTeamId]: data.teamStrength || 0
            }))
          })
          .catch(e => console.error('Fehler beim Laden der Team-Stärke:', e))
      }
      if (match.awayTeamId && !teamDetails[match.awayTeamId]) {
        fetch(`${API_BASE}/api/teams/${match.awayTeamId}/details`)
          .then(r => r.json())
          .then(data => {
            setTeamDetails(prev => ({
              ...prev,
              [match.awayTeamId]: data.teamStrength || 0
            }))
          })
          .catch(e => console.error('Fehler beim Laden der Team-Stärke:', e))
      }
    })
  }

  const openTeamDetails = (teamId) => {
    fetch(`${API_BASE}/api/teams/${teamId}/details`)
      .then(r => r.json())
      .then(data => {
        setSelectedTeamDetails(data)
        setShowTeamModal(true)
      })
      .catch(e => console.error('Fehler beim Laden der Team-Details:', e))
  }

  const getRoundMatchDay = (round) => {
    return round * 3 // Runde 1 = Spieltag 3, Runde 2 = Spieltag 6, etc.
  }

  const getCupRoundName = (round) => {
    const names = {
      1: '1. Runde (64→32)',
      2: '2. Runde (32→16)',
      3: '3. Runde (16→8)',
      4: 'Viertelfinale (8→4)',
      5: 'Halbfinale (4→2)',
      6: 'Finale'
    }
    return names[round] || `Runde ${round}`
  }

  if (!selectedCountry) {
    return (
      <div style={{ padding: '10px' }}>
        <h3>🏆 Pokalwettbewerb</h3>
        <p className="muted">Wählen Sie ein Land aus, um den Pokal zu sehen</p>
      </div>
    )
  }

  if (loading) {
    return <p>Lädt Pokal...</p>
  }

  if (!cupTournament) {
    return (
      <div style={{ padding: '10px' }}>
        <h3>🏆 Pokalwettbewerb {selectedCountry}</h3>
        {message && (
          <div style={{
            padding: '10px',
            backgroundColor: '#1a3a1a',
            border: '1px solid #4a7c0c',
            borderRadius: '4px',
            color: '#90ee90'
          }}>
            {message}
          </div>
        )}
      </div>
    )
  }

  return (
    <div style={{ padding: '10px' }}>
      <h3>🏆 {selectedCountry} Pokalwettbewerb - Saison {cupTournament.season}</h3>

      <div style={{
        padding: '12px',
        backgroundColor: 'rgba(0,0,0,0.3)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: '4px',
        marginBottom: '15px'
      }}>
        <p style={{ margin: '5px 0' }}>
          <strong>Status:</strong> {cupTournament.status === 'active' ? '🔄 Laufend' : '✅ Abgeschlossen'} • 
          <strong style={{ marginLeft: '10px' }}>Runde:</strong> {cupTournament.currentRound}/6 • 
          <strong style={{ marginLeft: '10px' }}>Teams:</strong> {cupTournament.remainingTeams}
        </p>
        {cupTournament.winnerTeamId && (
          <p style={{ margin: '5px 0', color: '#ffd700' }}>
            <strong>🏆 Gewinner:</strong> {cupTournament.winnerTeamName}
          </p>
        )}
      </div>

      {/* Runden-Navigation */}
      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '8px',
        marginBottom: '15px'
      }}>
        {[1, 2, 3, 4, 5, 6].map(round => (
          <button
            key={round}
            onClick={() => loadCupMatches(cupTournament.id, round)}
            style={{
              padding: '8px 12px',
              backgroundColor: selectedRound === round ? '#0066cc' : '#333',
              color: '#fff',
              border: 'none',
              borderRadius: '3px',
              cursor: 'pointer',
              fontSize: '0.85em',
              fontWeight: selectedRound === round ? 'bold' : 'normal'
            }}
          >
            {round === 6 ? 'Finale' : 'R' + round}
          </button>
        ))}
      </div>

      {/* Runden-Info */}
      <div style={{
        padding: '10px',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        border: '1px solid #3b82f6',
        borderRadius: '4px',
        marginBottom: '15px',
        fontSize: '0.9em',
        color: '#aaa'
      }}>
        <strong>{getCupRoundName(selectedRound)}</strong><br/>
        📅 Wird an Spieltag {getRoundMatchDay(selectedRound)} durchgeführt
      </div>

      {/* Pokalspiele */}
      {cupMatches.length === 0 ? (
        <p className="muted">Keine Spiele für diese Runde</p>
      ) : (
        <div>
          {cupMatches.map(match => {
            const isCompleted = match.status === 'completed'
            const homeWon = isCompleted && match.homeGoals > match.awayGoals
            const awayWon = isCompleted && match.awayGoals > match.homeGoals
            
            return (
              <div key={match.id} style={{
                padding: '12px',
                marginBottom: '10px',
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: '4px',
                backgroundColor: 'rgba(0,0,0,0.2)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  {/* Home Team */}
                  <div style={{ 
                    flex: 1,
                    padding: '8px',
                    backgroundColor: homeWon ? 'rgba(52, 211, 153, 0.3)' : 'transparent',
                    borderRadius: '3px',
                    textAlign: 'right',
                    marginRight: '10px'
                  }}>
                    <div
                      onClick={() => match.homeTeamId && openTeamDetails(match.homeTeamId)}
                      style={{
                        cursor: match.homeTeamId ? 'pointer' : 'default',
                        textDecoration: match.homeTeamId ? 'underline' : 'none'
                      }}
                    >
                      <strong>{match.homeTeamName}</strong>
                    </div>
                    <div className="muted" style={{ fontSize: '0.85em' }}>
                      💪 {teamDetails[match.homeTeamId] || '-'}
                    </div>
                  </div>

                  {/* Score */}
                  <div style={{ 
                    fontSize: '1.1em', 
                    fontWeight: 'bold',
                    minWidth: '80px',
                    textAlign: 'center'
                  }}>
                    {isCompleted ? (
                      <div>
                        <span style={{ color: '#ffb347' }}>
                          {match.homeGoals}:{match.awayGoals}
                        </span>
                        {match.resultNote && (
                          <div style={{ fontSize: '0.8em', color: '#aaa', marginTop: '2px' }}>
                            {match.resultNote}
                          </div>
                        )}
                      </div>
                    ) : (
                      <span style={{ color: '#aaa' }}>vs</span>
                    )}
                  </div>

                  {/* Away Team */}
                  <div style={{ 
                    flex: 1,
                    padding: '8px',
                    backgroundColor: awayWon ? 'rgba(52, 211, 153, 0.3)' : 'transparent',
                    borderRadius: '3px',
                    textAlign: 'left',
                    marginLeft: '10px'
                  }}>
                    <div
                      onClick={() => match.awayTeamId && openTeamDetails(match.awayTeamId)}
                      style={{
                        cursor: match.awayTeamId ? 'pointer' : 'default',
                        textDecoration: match.awayTeamId ? 'underline' : 'none'
                      }}
                    >
                      <strong>{match.awayTeamName}</strong>
                    </div>
                    <div className="muted" style={{ fontSize: '0.85em' }}>
                      💪 {teamDetails[match.awayTeamId] || '-'}
                    </div>
                  </div>
                </div>

                {/* Status */}
                {match.status === 'scheduled' && (
                  <div style={{
                    marginTop: '8px',
                    padding: '6px',
                    backgroundColor: 'rgba(255, 179, 71, 0.1)',
                    borderRadius: '2px',
                    fontSize: '0.85em',
                    color: '#ffb347',
                    textAlign: 'center'
                  }}>
                    ⏳ Geplant
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* Team Details Modal */}
      {showTeamModal && selectedTeamDetails && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }} onClick={() => setShowTeamModal(false)}>
          <div style={{
            background: '#1a1a1a',
            border: '1px solid rgba(255,255,255,0.2)',
            borderRadius: '8px',
            padding: '20px',
            maxWidth: '600px',
            width: '90%',
            maxHeight: '80vh',
            overflowY: 'auto'
          }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 16 }}>
              <div>
                <h3 style={{ margin: 0, marginBottom: 4 }}>{selectedTeamDetails.teamName}</h3>
                <div className="muted">💪 Teamstärke: {selectedTeamDetails.teamStrength}</div>
                <div className="muted">Aufstellung: {selectedTeamDetails.playersInLineup}/11</div>
              </div>
              <button
                onClick={() => setShowTeamModal(false)}
                style={{
                  background: 'rgba(255,255,255,0.1)',
                  border: 'none',
                  color: '#fff',
                  fontSize: '20px',
                  cursor: 'pointer',
                  padding: '0 8px'
                }}
              >
                ✕
              </button>
            </div>

            {selectedTeamDetails.lineup && selectedTeamDetails.lineup.length > 0 ? (
              <div>
                <h4>Aufstellung (4-4-2)</h4>
                {selectedTeamDetails.lineup.map((player) => (
                  <div key={player.playerId} className="card" style={{ marginBottom: 8 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div>
                        <strong>{player.playerName}</strong>
                        <div className="muted">{player.position}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <strong>Rating: {player.rating}</strong>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="muted">Keine Aufstellung verfügbar</p>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
