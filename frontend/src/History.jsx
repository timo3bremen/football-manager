import React, { useState, useEffect } from 'react'

export default function History() {
  const [seasons, setSeasons] = useState([])
  const [selectedSeason, setSelectedSeason] = useState(null)
  const [countries, setCountries] = useState([])
  const [selectedCountry, setSelectedCountry] = useState(null)
  const [leagues, setLeagues] = useState([])
  const [selectedLeague, setSelectedLeague] = useState(null)
  const [leagueStandings, setLeagueStandings] = useState([])
  const [cupHistory, setCupHistory] = useState(null)
  const [view, setView] = useState('league') // 'league', 'cup', 'alltime'
  const [allTimeTable, setAllTimeTable] = useState([])
  const [allTimeDivision, setAllTimeDivision] = useState(1)
  const [loading, setLoading] = useState(false)

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  // Lade verfügbare Saisons
  useEffect(() => {
    fetch(`${API_BASE}/api/v2/history/seasons`)
      .then(r => r.json())
      .then(data => {
        setSeasons(data)
        if (data.length > 0) {
          setSelectedSeason(data[data.length - 1]) // Wähle letzte Saison
        }
      })
      .catch(e => console.error('Fehler beim Laden der Saisons:', e))
  }, [])

  // Lade Länder für ausgewählte Saison
  useEffect(() => {
    if (selectedSeason) {
      fetch(`${API_BASE}/api/v2/history/season/${selectedSeason}/countries`)
        .then(r => r.json())
        .then(data => {
          setCountries(data)
          if (data.length > 0) {
            setSelectedCountry(data[0]) // Wähle erstes Land
          }
        })
        .catch(e => console.error('Fehler beim Laden der Länder:', e))
    }
  }, [selectedSeason])

  // Lade Ligen für ausgewählte Saison und Land
  useEffect(() => {
    if (selectedSeason && selectedCountry) {
      fetch(`${API_BASE}/api/v2/history/season/${selectedSeason}/country/${selectedCountry}/leagues`)
        .then(r => r.json())
        .then(data => {
          setLeagues(data)
          if (data.length > 0) {
            setSelectedLeague(data[0]) // Wähle erste Liga
          }
        })
        .catch(e => console.error('Fehler beim Laden der Ligen:', e))
    }
  }, [selectedSeason, selectedCountry])

  // Lade Ligatabelle
  useEffect(() => {
    if (selectedSeason && selectedLeague && view === 'league') {
      setLoading(true)
      fetch(`${API_BASE}/api/v2/history/season/${selectedSeason}/league/${selectedLeague.leagueId}`)
        .then(r => r.json())
        .then(data => {
          setLeagueStandings(data)
          setLoading(false)
        })
        .catch(e => {
          console.error('Fehler beim Laden der Tabelle:', e)
          setLoading(false)
        })
    }
  }, [selectedSeason, selectedLeague, view])

  // Lade Pokalhistorie
  useEffect(() => {
    if (selectedSeason && selectedCountry && view === 'cup') {
      setLoading(true)
      fetch(`${API_BASE}/api/v2/history/season/${selectedSeason}/cup/${selectedCountry}`)
        .then(r => r.json())
        .then(data => {
          setCupHistory(data)
          setLoading(false)
        })
        .catch(e => {
          console.error('Fehler beim Laden der Pokalhistorie:', e)
          setLoading(false)
        })
    }
  }, [selectedSeason, selectedCountry, view])

  // Lade Ewige Tabelle
  useEffect(() => {
    if (view === 'alltime') {
      setLoading(true)
      fetch(`${API_BASE}/api/v2/history/all-time-table/${allTimeDivision}`)
        .then(r => r.json())
        .then(data => {
          setAllTimeTable(data)
          setLoading(false)
        })
        .catch(e => {
          console.error('Fehler beim Laden der ewigen Tabelle:', e)
          setLoading(false)
        })
    }
  }, [view, allTimeDivision])

  if (seasons.length === 0) {
    return (
      <div>
        <h3>📚 Geschichte</h3>
        <div className="card">
          <p className="muted">Noch keine abgeschlossenen Saisons vorhanden.</p>
          <p className="muted">Spiele mindestens eine Saison zu Ende, um hier die Historie zu sehen.</p>
        </div>
      </div>
    )
  }

  return (
    <div>
      <h3>📚 Geschichte</h3>

      {/* View Navigation */}
      <div className="menu" style={{marginBottom: 16}}>
        <button 
          className={view === 'league' ? 'active' : ''} 
          onClick={() => setView('league')}
        >
          🏆 Ligen
        </button>
        <button 
          className={view === 'cup' ? 'active' : ''} 
          onClick={() => setView('cup')}
        >
          🏅 Pokal
        </button>
        <button 
          className={view === 'alltime' ? 'active' : ''} 
          onClick={() => setView('alltime')}
        >
          ⭐ Ewige Tabelle
        </button>
      </div>

      {/* Ligen-Ansicht */}
      {view === 'league' && (
        <div className="card">
          <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12, marginBottom: 16}}>
            <div>
              <label style={{fontSize: '0.9em', opacity: 0.7, display: 'block', marginBottom: 4}}>Saison</label>
              <select 
                value={selectedSeason || ''} 
                onChange={(e) => setSelectedSeason(parseInt(e.target.value))}
                style={{width: '100%', padding: 8, borderRadius: 4, border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.3)', color: 'white'}}
              >
                {seasons.map(s => (
                  <option key={s} value={s}>Saison {s}</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{fontSize: '0.9em', opacity: 0.7, display: 'block', marginBottom: 4}}>Land</label>
              <select 
                value={selectedCountry || ''} 
                onChange={(e) => setSelectedCountry(e.target.value)}
                style={{width: '100%', padding: 8, borderRadius: 4, border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.3)', color: 'white'}}
              >
                {countries.map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{fontSize: '0.9em', opacity: 0.7, display: 'block', marginBottom: 4}}>Liga</label>
              <select 
                value={selectedLeague?.leagueId || ''} 
                onChange={(e) => {
                  const league = leagues.find(l => l.leagueId === parseInt(e.target.value))
                  setSelectedLeague(league)
                }}
                style={{width: '100%', padding: 8, borderRadius: 4, border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.3)', color: 'white'}}
              >
                {leagues.map(l => (
                  <option key={l.leagueId} value={l.leagueId}>{l.leagueName}</option>
                ))}
              </select>
            </div>
          </div>

          {loading ? (
            <p className="muted">Lädt...</p>
          ) : leagueStandings.length > 0 ? (
            <div style={{overflowX: 'auto'}}>
              <table style={{width: '100%', borderCollapse: 'collapse'}}>
                <thead>
                  <tr style={{borderBottom: '2px solid rgba(255,255,255,0.2)'}}>
                    <th style={{textAlign: 'left', padding: '8px'}}>Platz</th>
                    <th style={{textAlign: 'left', padding: '8px'}}>Team</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Sp</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>S</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>U</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>N</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Tore</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Diff</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Pkt</th>
                  </tr>
                </thead>
                <tbody>
                  {leagueStandings.map((team, idx) => {
                    const games = team.wins + team.draws + team.losses
                    const goalDiff = team.goalsFor - team.goalsAgainst
                    const bgColor = idx === 0 ? 'rgba(255, 215, 0, 0.1)' : 
                                   idx === 1 ? 'rgba(192, 192, 192, 0.1)' : 
                                   idx === 2 ? 'rgba(205, 127, 50, 0.1)' : 'transparent'
                    
                    return (
                      <tr key={team.teamId} style={{borderBottom: '1px solid rgba(255,255,255,0.05)', background: bgColor}}>
                        <td style={{padding: '8px', fontWeight: 'bold'}}>{team.position}</td>
                        <td style={{padding: '8px'}}>{team.teamName}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{games}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.wins}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.draws}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.losses}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.goalsFor}:{team.goalsAgainst}</td>
                        <td style={{textAlign: 'center', padding: '8px', color: goalDiff > 0 ? '#4ade80' : goalDiff < 0 ? '#fda4af' : 'white'}}>
                          {goalDiff > 0 ? '+' : ''}{goalDiff}
                        </td>
                        <td style={{textAlign: 'center', padding: '8px', fontWeight: 'bold'}}>{team.points}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="muted">Keine Daten verfügbar</p>
          )}
        </div>
      )}

      {/* Pokal-Ansicht */}
      {view === 'cup' && (
        <div className="card">
          <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 16}}>
            <div>
              <label style={{fontSize: '0.9em', opacity: 0.7, display: 'block', marginBottom: 4}}>Saison</label>
              <select 
                value={selectedSeason || ''} 
                onChange={(e) => setSelectedSeason(parseInt(e.target.value))}
                style={{width: '100%', padding: 8, borderRadius: 4, border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.3)', color: 'white'}}
              >
                {seasons.map(s => (
                  <option key={s} value={s}>Saison {s}</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{fontSize: '0.9em', opacity: 0.7, display: 'block', marginBottom: 4}}>Land</label>
              <select 
                value={selectedCountry || ''} 
                onChange={(e) => setSelectedCountry(e.target.value)}
                style={{width: '100%', padding: 8, borderRadius: 4, border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.3)', color: 'white'}}
              >
                {countries.map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
          </div>

          {loading ? (
            <p className="muted">Lädt...</p>
          ) : cupHistory && cupHistory.rounds ? (
            <div>
              {cupHistory.winner && (
                <div style={{padding: 16, marginBottom: 16, background: 'linear-gradient(135deg, rgba(255,215,0,0.2), rgba(255,215,0,0.05))', borderRadius: 8, border: '1px solid rgba(255,215,0,0.3)'}}>
                  <div style={{fontSize: '1.2em', fontWeight: 'bold', marginBottom: 4}}>🏆 Pokalsieger</div>
                  <div style={{fontSize: '1.1em'}}>{cupHistory.winner}</div>
                </div>
              )}

              {Object.keys(cupHistory.rounds).sort((a, b) => parseInt(a) - parseInt(b)).map(round => {
                const matches = cupHistory.rounds[round]
                const roundNames = {
                  1: '1. Runde (64 Teams)',
                  2: '2. Runde (32 Teams)',
                  3: 'Achtelfinale (16 Teams)',
                  4: 'Viertelfinale (8 Teams)',
                  5: 'Halbfinale (4 Teams)',
                  6: 'Finale (2 Teams)'
                }

                return (
                  <div key={round} style={{marginBottom: 20}}>
                    <h4 style={{marginBottom: 12}}>{roundNames[round] || `Runde ${round}`}</h4>
                    <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 8}}>
                      {matches.map((match, idx) => (
                        <div 
                          key={idx} 
                          style={{
                            padding: 10, 
                            background: 'rgba(255,255,255,0.05)', 
                            borderRadius: 6,
                            border: '1px solid rgba(255,255,255,0.1)'
                          }}
                        >
                          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                            <span style={{flex: 1, fontWeight: match.homeGoals > match.awayGoals ? 'bold' : 'normal'}}>
                              {match.homeTeamName}
                            </span>
                            <span style={{padding: '0 8px', fontWeight: 'bold'}}>
                              {match.homeGoals}:{match.awayGoals}
                            </span>
                            <span style={{flex: 1, textAlign: 'right', fontWeight: match.awayGoals > match.homeGoals ? 'bold' : 'normal'}}>
                              {match.awayTeamName}
                            </span>
                          </div>
                          {match.resultNote && (
                            <div style={{textAlign: 'center', fontSize: '0.85em', opacity: 0.7, marginTop: 4}}>
                              {match.resultNote}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )
              })}
            </div>
          ) : (
            <p className="muted">Keine Pokaldaten verfügbar</p>
          )}
        </div>
      )}

      {/* Ewige Tabelle */}
      {view === 'alltime' && (
        <div className="card">
          <div style={{marginBottom: 16}}>
            <label style={{fontSize: '0.9em', opacity: 0.7, display: 'block', marginBottom: 4}}>Division</label>
            <div style={{display: 'flex', gap: 8}}>
              <button 
                className={allTimeDivision === 1 ? 'btn primary' : 'btn secondary'} 
                onClick={() => setAllTimeDivision(1)}
              >
                1. Liga
              </button>
              <button 
                className={allTimeDivision === 2 ? 'btn primary' : 'btn secondary'} 
                onClick={() => setAllTimeDivision(2)}
              >
                2. Liga
              </button>
              <button 
                className={allTimeDivision === 3 ? 'btn primary' : 'btn secondary'} 
                onClick={() => setAllTimeDivision(3)}
              >
                3. Liga
              </button>
            </div>
          </div>

          {loading ? (
            <p className="muted">Lädt...</p>
          ) : allTimeTable.length > 0 ? (
            <div style={{overflowX: 'auto'}}>
              <table style={{width: '100%', borderCollapse: 'collapse'}}>
                <thead>
                  <tr style={{borderBottom: '2px solid rgba(255,255,255,0.2)'}}>
                    <th style={{textAlign: 'left', padding: '8px'}}>Platz</th>
                    <th style={{textAlign: 'left', padding: '8px'}}>Team</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Saisons</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Titel</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>S</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>U</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>N</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Tore</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Diff</th>
                    <th style={{textAlign: 'center', padding: '8px'}}>Pkt</th>
                  </tr>
                </thead>
                <tbody>
                  {allTimeTable.map((team) => {
                    const goalDiff = team.goalsFor - team.goalsAgainst
                    const bgColor = team.position === 1 ? 'rgba(255, 215, 0, 0.1)' : 
                                   team.position === 2 ? 'rgba(192, 192, 192, 0.1)' : 
                                   team.position === 3 ? 'rgba(205, 127, 50, 0.1)' : 'transparent'
                    
                    return (
                      <tr key={team.teamId} style={{borderBottom: '1px solid rgba(255,255,255,0.05)', background: bgColor}}>
                        <td style={{padding: '8px', fontWeight: 'bold'}}>{team.position}</td>
                        <td style={{padding: '8px'}}>{team.teamName}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.seasons}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>
                          {team.titles > 0 && <span style={{color: '#ffd700'}}>🏆 {team.titles}</span>}
                        </td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.wins}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.draws}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.losses}</td>
                        <td style={{textAlign: 'center', padding: '8px'}}>{team.goalsFor}:{team.goalsAgainst}</td>
                        <td style={{textAlign: 'center', padding: '8px', color: goalDiff > 0 ? '#4ade80' : goalDiff < 0 ? '#fda4af' : 'white'}}>
                          {goalDiff > 0 ? '+' : ''}{goalDiff}
                        </td>
                        <td style={{textAlign: 'center', padding: '8px', fontWeight: 'bold'}}>{team.points}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="muted">Keine Daten verfügbar</p>
          )}
        </div>
      )}
    </div>
  )
}
