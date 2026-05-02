import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'
import CupSection from './CupSection'

export default function Schedule(){
  const { team, currentMatchday, setCurrentMatchday, userLeagueId, setUserLeagueId, userLeagueLabel, setUserLeagueLabel } = useGame()
   const [tab, setTab] = useState('league') // 'league', 'table', 'statistics'
   const [displayedMatchday, setDisplayedMatchday] = useState(currentMatchday)
   const [matchday, setMatchday] = useState(null)
   const [standings, setStandings] = useState([])
   const [statistics, setStatistics] = useState([])
   const [loading, setLoading] = useState(false)
   const [selectedTeamDetails, setSelectedTeamDetails] = useState(null)
   const [showTeamModal, setShowTeamModal] = useState(false)
   const [simulatingMatchId, setSimulatingMatchId] = useState(null)
   const [lastSimulatedMatchId, setLastSimulatedMatchId] = useState(null)
   const [matchReport, setMatchReport] = useState(null)
   const [showMatchReport, setShowMatchReport] = useState(false)
   // NEW: Liga-Wechsel mit Land-Filter
   const [countries, setCountries] = useState([])
   const [selectedCountry, setSelectedCountry] = useState(null)
   const [allLeagues, setAllLeagues] = useState([]) // ALLE Ligen (nicht gefiltert)
   const [leagues, setLeagues] = useState([]) // Gefilterte Ligen für das Land
   const [selectedLeague, setSelectedLeague] = useState(userLeagueId)
   const [countriesLoaded, setCountriesLoaded] = useState(false)
   const [initialLoadComplete, setInitialLoadComplete] = useState(false)
   const [showAllScorers, setShowAllScorers] = useState(false) // Für Torschützen aufklappen
   const [showAllAssisters, setShowAllAssisters] = useState(false) // Für Vorlagengeber aufklappen

 const API_BASE = 'http://192.168.178.21:8080'

  // ...existing code...

  // Lade Spieltag beim Wechsel
  useEffect(() => {
    loadMatchday(displayedMatchday)
  }, [displayedMatchday])

  // NEW: Lade BEIDE (Matchday + Standings) SOFORT wenn Liga gewechselt wird
  useEffect(() => {
    if (selectedLeague) {
      // Lade IMMER, egal ob es userLeagueId ist oder nicht!
      const matchdayUrl = `${API_BASE}/api/v2/schedule/matchday/league/${selectedLeague}/${displayedMatchday}`
      const standingsUrl = `${API_BASE}/api/v2/schedule/standings/league/${selectedLeague}`
      
      setLoading(true)
      
      // Parallel beide laden
      Promise.all([
        fetch(matchdayUrl).then(r => r.json()),
        fetch(standingsUrl).then(r => r.json())
      ])
        .then(([matchdayData, standingsData]) => {
          setMatchday(matchdayData)
          setStandings(standingsData)
          setLoading(false)
        })
        .catch(e => {
          console.error('Fehler beim Laden der Liga-Daten:', e)
          setLoading(false)
        })
    }
  }, [selectedLeague])

  // NEW: Wenn User-Liga sich ändert, update auch selectedLeague
  useEffect(() => {
    if (userLeagueId && !selectedLeague) {
      setSelectedLeague(userLeagueId)
    }
  }, [userLeagueId, selectedLeague])

   // NEW: Lade Länder UND alle Ligen beim Mount
   useEffect(() => {
     loadCountries()
     loadAllLeagues()
   }, [])

   // NEW: Wenn Team verfügbar ist, lade seine Liga vom Backend UND initialisiere
   useEffect(() => {
     if (team && team.id && allLeagues.length > 0 && !initialLoadComplete) {
       loadUserTeamLeagueInfoAndInitialize()
     }
   }, [team, allLeagues, initialLoadComplete])

   // NEW: Wenn Country wechselt (manuell NACH initial Load), filtere Ligen neu
   useEffect(() => {
     if (selectedCountry && allLeagues.length > 0 && initialLoadComplete) {
       // Filtere Ligen für das neue Land
       const countryLeagues = allLeagues.filter(l => l.country === selectedCountry)
       setLeagues(countryLeagues)
       // Setze erste Liga des neuen Landes
       if (countryLeagues.length > 0) {
         setSelectedLeague(countryLeagues[0].id)
       }
     }
   }, [selectedCountry, allLeagues, initialLoadComplete])

  // NEW: Lade Standings ERST wenn team verfügbar ist (damit User-Team Name korrekt ist)
  useEffect(() => {
    if (team && team.id) {
      loadStandings()
    }
   }, [team])

   // REMOVED: Label soll beim Liga-Wechsel NICHT ändern - wird nur beim initialen Load gesetzt

   // ...existing code...

   // Lade Tabelle wenn Tab wechselt
   useEffect(() => {
     if (tab === 'table') {
       loadStandings()
     } else if (tab === 'statistics') {
       loadStatistics()
     }
   }, [tab])

   // WICHTIG: Lade Statistiken AUCH wenn Liga in Statistik-Tab gewechselt wird
   useEffect(() => {
     if (tab === 'statistics') {
       loadStatistics()
     }
   }, [selectedLeague, tab])

   const loadCurrentMatchday = () => {
     fetch(`${API_BASE}/api/v2/schedule/current-matchday`)
       .then(r => r.json())
       .then(data => {
         setCurrentMatchday(data.currentMatchday || 1)
         setDisplayedMatchday(data.currentMatchday || 1)
       })
       .catch(e => console.error('Fehler beim Laden des Spieltags:', e))
   }

   // NEW: Lade die Liga des aktuellen Teams vom Backend UND initialisiere sofort
   const loadUserTeamLeagueInfoAndInitialize = async () => {
     try {
       if (!team || !team.id || allLeagues.length === 0) return
       
       const authRaw = localStorage.getItem('fm_auth')
       const token = authRaw ? JSON.parse(authRaw).token : null
       const headers = {}
       if (token) headers['X-Auth-Token'] = token
       
       // Frage vom Backend ab, in welcher Liga dieses Team ist
       const res = await fetch(`${API_BASE}/api/v2/teams/${team.id}/league-info`, { headers })
       if (res.ok) {
         const leagueInfo = await res.json()
         
         if (leagueInfo.leagueId) {
           // Finde die Liga in allLeagues
           const userLeague = allLeagues.find(l => l.id === leagueInfo.leagueId)
           if (userLeague && userLeague.country) {
             // Filtere Ligen für dieses Land
             const countryLeagues = allLeagues.filter(l => l.country === userLeague.country)
             
             // Alle State-Updates synchron
             setUserLeagueId(leagueInfo.leagueId)
             setSelectedCountry(userLeague.country)
             setLeagues(countryLeagues)
             setSelectedLeague(leagueInfo.leagueId)
             setUserLeagueLabel(userLeague.divisionLabel)
             setInitialLoadComplete(true)
           }
         }
       }
     } catch (e) {
       console.error('Fehler beim Laden der Team-Liga:', e)
     }
   }

  const loadMatchday = (dayNumber) => {
    setLoading(true)
    // Lade Matchday für die aktuelle Liga
    const leagueId = selectedLeague || userLeagueId
    const url = leagueId 
      ? `${API_BASE}/api/v2/schedule/matchday/league/${leagueId}/${dayNumber}`
      : `${API_BASE}/api/v2/schedule/matchday/${dayNumber}`
    
    fetch(url)
      .then(r => r.json())
      .then(data => {
        setMatchday(data)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden des Spieltags:', e)
        setLoading(false)
      })
  }

  const loadStandings = () => {
    setLoading(true)
    // Lade die Standings der ausgewählten Liga (oder User-Liga als Standard)
    const leagueId = selectedLeague || userLeagueId
    const url = leagueId 
      ? `${API_BASE}/api/v2/schedule/standings/league/${leagueId}`
      : `${API_BASE}/api/v2/schedule/standings`
    
    fetch(url)
      .then(r => r.json())
      .then(data => {
        setStandings(data)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Tabelle:', e)
        setLoading(false)
      })
  }

   const loadCountries = () => {
     fetch(`${API_BASE}/api/v2/schedule/countries`)
       .then(r => r.json())
       .then(data => {
         setCountries(data)
         setCountriesLoaded(true)
       })
       .catch(e => {
         console.error('Fehler beim Laden der Länder:', e)
         setCountriesLoaded(true)
       })
   }

   // NEW: Lade ALLE Ligen (mit Land-Info)
   const loadAllLeagues = () => {
     fetch(`${API_BASE}/api/v2/schedule/leagues`)
       .then(r => r.json())
       .then(data => {
         setAllLeagues(data)
       })
       .catch(e => {
         console.error('Fehler beim Laden der Ligen:', e)
         setAllLeagues([])
       })
   }

   // Legacy: Lade verfügbare Ligen
   const loadLeagues = () => {
     fetch(`${API_BASE}/api/v2/schedule/leagues`)
       .then(r => r.json())
       .then(data => {
         setLeagues(data)
         // Setze Standard-Liga wenn nicht gesetzt
         if (data.length > 0 && !userLeagueId) {
           setSelectedLeague(data[0].id)
           setUserLeagueId(data[0].id)
           setUserLeagueLabel(data[0].divisionLabel)
         }
       })
       .catch(e => console.error('Fehler beim Laden der Ligen:', e))
   }

  const loadStatistics = () => {
    setLoading(true)
    // Lade Statistiken für die aktuelle oder ausgewählte Liga
    const leagueId = selectedLeague || userLeagueId
    const url = leagueId 
      ? `${API_BASE}/api/v2/schedule/extended-statistics/league/${leagueId}`
      : `${API_BASE}/api/v2/schedule/extended-statistics`
    
    fetch(url)
      .then(r => r.json())
      .then(data => {
        setStatistics(data)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Statistiken:', e)
        setLoading(false)
      })
  }

  const loadMatchReport = (matchId) => {
    setLoading(true)
    fetch(`${API_BASE}/api/v2/schedule/match/${matchId}/report`)
      .then(r => r.json())
      .then(data => {
        setMatchReport(data)
        setShowMatchReport(true)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden des Spielberichts:', e)
        setLoading(false)
      })
  }

   const simulateMatch = (matchId) => {
     setSimulatingMatchId(matchId)
     fetch(`${API_BASE}/api/v2/schedule/simulate-match/${matchId}`, { method: 'POST' })
       .then(r => {
         if (!r.ok) {
           return r.text().then(text => {
             throw new Error(text || `Error: ${r.status}`)
           })
         }
         return r.json()
       })
       .then(data => {
         // Update matchday with result
         loadMatchday(displayedMatchday)
         loadStandings()
         // Lade automatisch den Spielbericht
         loadMatchReport(matchId)
         setLastSimulatedMatchId(matchId)
         setSimulatingMatchId(null)
         // Update budget und team info (sponsor payouts)
         window.dispatchEvent(new Event('teamUpdated'))
       })
       .catch(e => {
         console.error('Fehler bei Simulation:', e)
         alert('Simulation fehlgeschlagen: ' + e.message)
         setSimulatingMatchId(null)
       })
   }

  const getTeamName = (teamId) => {
    if (!teamId) return 'TBD'
    if (team && team.id === teamId) return team.name
    
    // Suche Teamnamen in der Tabelle
    const standingEntry = standings.find(s => s.teamId === teamId)
    if (standingEntry) return standingEntry.teamName
    
    return `Team ${teamId}`
  }

  const getTeamStrength = (teamId) => {
    if (!teamId) return 0
    // Prüfe zuerst ob es das User-Team ist (vollständige Stats)
    if (team && team.id === teamId) {
      const standingEntry = standings.find(s => s.teamId === teamId)
      if (standingEntry) return standingEntry.teamStrength
      // Wenn nicht in standings, berechne aus Spielern
      return calculateTeamStrength(teamId)
    }
    const standingEntry = standings.find(s => s.teamId === teamId)
    return standingEntry ? standingEntry.teamStrength : 0
  }

  const calculateTeamStrength = (teamId) => {
    // Fallback wenn Team nicht in Standings ist - nutze team Objekt
    if (team && team.id === teamId) {
      let totalStrength = 0
      if (team.roster && team.roster.length > 0) {
        totalStrength = team.roster.reduce((sum, player) => sum + (player.rating || 0), 0)
      }
      return Math.round(totalStrength / Math.max(team.roster ? team.roster.length : 1, 1))
    }
    return 0
  }

  const openTeamDetails = (teamId) => {
    setLoading(true)
    fetch(`${API_BASE}/api/teams/${teamId}/details`)
      .then(r => r.json())
      .then(data => {
        setSelectedTeamDetails(data)
        setShowTeamModal(true)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Team-Details:', e)
        setLoading(false)
      })
  }

  const formatMatchResult = (match) => {
    if (match.status === 'played') {
      return `${match.homeGoals}:${match.awayGoals}`
    }
    return 'vs'
  }

  const getPositionClass = (position) => {
    if (position <= 2) return 'promotion' // Top 2 (Green)
    if (position >= 9) return 'relegation' // Bottom 4 (Red)
    return ''
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div>
            <h3 style={{ margin: 0 }}>Spielplan & Tabelle</h3>
            {/* Liga-Anzeige als Text + separater Wechsel-Dropdown */}
             <div style={{ fontSize: '0.9em', color: '#999', marginTop: 4, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
               <div>
                 📌 Deine Liga: <strong>{userLeagueLabel || 'Lädt...'}</strong>
               </div>
               
               {/* Land-Auswahl */}
               {countries.length > 0 && (
                 <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                   <span style={{ fontSize: '0.85em' }}>Land:</span>
                   <select 
                     value={selectedCountry || ''}
                     onChange={(e) => setSelectedCountry(e.target.value)}
                     style={{
                       padding: '4px 8px',
                       borderRadius: 4,
                       border: '1px solid rgba(255,255,255,0.2)',
                       background: 'rgba(0,0,0,0.3)',
                       color: '#fff',
                       cursor: 'pointer',
                       fontSize: '0.85em',
                     }}
                   >
                     {countries.map(country => (
                       <option key={country} value={country}>
                         {country}
                       </option>
                     ))}
                   </select>
                 </div>
               )}
               
               {/* Liga-Wechsel Dropdown (nur für View-Only) */}
               {leagues.length > 0 && (
                 <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                   <span style={{ fontSize: '0.85em' }}>Liga:</span>
                   <select 
                     value={selectedLeague || userLeagueId || ''}
                     onChange={(e) => {
                       const leagueId = Number(e.target.value)
                       setSelectedLeague(leagueId)
                     }}
                     style={{
                       padding: '4px 8px',
                       borderRadius: 4,
                       border: '1px solid rgba(255,255,255,0.2)',
                       background: 'rgba(0,0,0,0.3)',
                       color: '#fff',
                       cursor: 'pointer',
                       fontSize: '0.85em',
                     }}
                   >
                     {leagues.map(league => (
                       <option key={league.id} value={league.id}>
                         {league.divisionLabel}
                       </option>
                     ))}
                   </select>
                 </div>
               )}
             </div>
          </div>
        </div>
        
         <button
            onClick={() => {
              setLoading(true)
              fetch(`${API_BASE}/api/v2/schedule/advance-matchday`, { method: 'POST' })
               .then(r => r.json())
               .then(data => {
                 // Aktualisiere den aktuellen Spieltag
                 setCurrentMatchday(data.newMatchday)
                 setDisplayedMatchday(data.newMatchday)
                 // Lade die neuen Daten
                 loadCurrentMatchday()
                 loadStandings()
                 loadMatchday(data.newMatchday)
                  setLoading(false)
                  // Zeige eine Nachricht
                  if (data.message) {
                    alert(data.message)
                  }
                  // Wenn Saison Reset, lade auch Ligen neu
                  if (data.seasonReset) {
                    loadLeagues()
                  }
                  // Update budget (season end bonuses) und neuer Spieltag
                  window.dispatchEvent(new Event('matchdayChanged'))
                  window.dispatchEvent(new Event('teamUpdated'))
               })
               .catch(e => {
                 console.error('Fehler beim Weiterschalten:', e)
                 setLoading(false)
               })
           }}
          disabled={loading}
          style={{
            padding: '8px 16px',
            borderRadius: 4,
            border: 'none',
            background: currentMatchday >= 25 ? '#f59e0b' : '#6366f1',
            color: '#fff',
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '0.9em',
            fontWeight: 'bold',
            opacity: loading ? 0.6 : 1
          }}
        >
          {loading ? '⏳ Wird verarbeitet...' : (currentMatchday >= 25 ? '🏆 NEUE SAISON STARTEN!' : '→ Nächster Tag')}
        </button>

         <button
           onClick={() => {
             setLoading(true)
             fetch(`${API_BASE}/api/v2/schedule/simulate-season`, { method: 'POST' })
               .then(r => r.json())
               .then(data => {
                 // Aktualisiere den aktuellen Spieltag
                 setCurrentMatchday(data.newMatchday)
                 setDisplayedMatchday(data.newMatchday)
                 // Lade die neuen Daten
                 loadCurrentMatchday()
                 loadStandings()
                 loadMatchday(data.newMatchday)
                  setLoading(false)
                  // Zeige eine Nachricht
                  if (data.message) {
                    alert(data.message)
                  }
                  // Update budget (all season payouts) und neuer Spieltag
                  window.dispatchEvent(new Event('matchdayChanged'))
                  window.dispatchEvent(new Event('teamUpdated'))
               })
               .catch(e => {
                 console.error('Fehler bei Saison-Simulation:', e)
                 setLoading(false)
               })
           }}
          disabled={loading}
          style={{
            padding: '8px 16px',
            borderRadius: 4,
            border: 'none',
            background: '#10b981',
            color: '#fff',
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '0.9em',
            fontWeight: 'bold',
            opacity: loading ? 0.6 : 1,
            marginLeft: '8px'
          }}
        >
          {loading ? '⏳ Wird verarbeitet...' : '⚡ Saison Simulieren'}
        </button>
      </div>

      <div className="card">
        <div className="menu" style={{ marginBottom: 12 }}>
          <button className={tab === 'league' ? 'active' : ''} onClick={() => setTab('league')}>
            Spielplan
          </button>
          <button className={tab === 'table' ? 'active' : ''} onClick={() => setTab('table')}>
            Tabelle
          </button>
          <button className={tab === 'statistics' ? 'active' : ''} onClick={() => setTab('statistics')}>
            Statistiken
          </button>
          <button className={tab === 'cup' ? 'active' : ''} onClick={() => setTab('cup')}>
            🏆 Pokal
          </button>
        </div>

        <div className="panel">
          {tab === 'league' && (
            <div>
              <h4>Liga - Spieltag {displayedMatchday} {matchday?.isOffSeason ? '(Off-Season)' : ''} {matchday && matchday.dayNumber && matchday.dayNumber !== currentMatchday && <span style={{ fontSize: '0.8em', opacity: 0.7 }}>(aktuell: {currentMatchday})</span>}</h4>

               {/* Spieltag Navigation */}
               <div style={{ 
                 display: 'flex', 
                 gap: 4, 
                 marginBottom: 12, 
                 flexWrap: 'wrap',
                 maxHeight: 100,
                 overflowY: 'auto',
                 padding: 8,
                 border: '1px solid rgba(255,255,255,0.1)',
                 borderRadius: 4
               }}>
                 {Array.from({ length: 25 }, (_, i) => i + 1).map(day => {
                   const isCurrentMatchday = day === currentMatchday
                   const isOffSeason = day > 22  // Off-Season nur nach Spieltag 22 (Spieltag 23-25)
                   return (
                     <button
                       key={day}
                       onClick={() => setDisplayedMatchday(day)}
                       title={isCurrentMatchday ? 'Aktueller Spieltag' : (isOffSeason ? 'Off-Season' : 'Wechsle zu Spieltag ' + day)}
                       style={{
                         padding: '6px 10px',
                         borderRadius: 4,
                         border: isCurrentMatchday ? '2px solid #fbbf24' : (isOffSeason ? '1px solid #888' : '1px solid rgba(255,255,255,0.2)'),
                         background: isCurrentMatchday ? 'rgba(251, 191, 36, 0.3)' : (isOffSeason ? 'rgba(136, 136, 136, 0.2)' : 'rgba(0,0,0,0.2)'),
                         color: isCurrentMatchday ? '#fbbf24' : (isOffSeason ? '#999' : '#fff'),
                         cursor: 'pointer',
                         fontSize: '0.85em',
                         fontWeight: isCurrentMatchday ? 'bold' : 'normal'
                       }}
                     >
                       {isOffSeason ? '○' : day}
                    </button>
                  )
                })}
              </div>

               {/* Matches or Off-Season Message */}
               {loading ? (
                 <p className="muted">Lädt...</p>
               ) : matchday?.isOffSeason ? (
                 <div style={{ 
                   padding: '20px', 
                   textAlign: 'center', 
                   background: 'rgba(136, 136, 136, 0.1)',
                   borderRadius: 4,
                   border: '1px solid rgba(136, 136, 136, 0.3)'
                 }}>
                   <p style={{ fontSize: '1.2em', fontWeight: 'bold', marginBottom: 8 }}>🔄 OFF-SEASON</p>
                   <p className="muted">Transferfenster geöffnet</p>
                   <p className="muted" style={{ fontSize: '0.9em', marginTop: 8 }}>Keine Spiele in dieser Periode</p>
                 </div>
                 ) : matchday && matchday.matches ? (
                   <div>
                     {matchday.matches.map((match, idx) => {
                       // Prüfe ob dies das Spiel des User-Teams ist
                       const isUserMatch = (match.homeTeamId === team.id || match.awayTeamId === team.id)
                       
                       return (
                      <div 
                        key={idx}
                        className="card match-card"
                        style={{ 
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          padding: '12px',
                          marginBottom: 8,
                          background: isUserMatch ? 'linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(37, 99, 235, 0.05))' : undefined,
                          border: isUserMatch ? '2px solid rgba(59, 130, 246, 0.4)' : undefined
                        }}
                      >
                       {/* Desktop: Horizontal Layout */}
                       <div className="match-desktop" style={{
                         display: 'flex',
                         justifyContent: 'space-between',
                         alignItems: 'center',
                         width: '100%',
                         gap: '16px'
                       }}>
                         <div style={{ flex: 1, textAlign: 'right', marginRight: 16 }}>
                           <strong 
                             style={{ 
                               cursor: match.homeTeamId ? 'pointer' : 'default', 
                               textDecoration: match.homeTeamId ? 'underline' : 'none',
                               opacity: match.homeTeamId ? 1 : 0.6
                             }}
                             onClick={() => match.homeTeamId && openTeamDetails(match.homeTeamId)}
                           >
                             {getTeamName(match.homeTeamId)}
                           </strong>
                           <div className="muted" style={{ fontSize: '0.85em' }}>
                             💪 {getTeamStrength(match.homeTeamId)}
                           </div>
                         </div>
                         <div style={{ 
                           fontSize: '1.1em', 
                           fontWeight: 'bold',
                           minWidth: 60,
                           textAlign: 'center'
                         }}>
                           {formatMatchResult(match)}
                         </div>
                         <div style={{ flex: 1, textAlign: 'left', marginLeft: 16 }}>
                           <strong 
                             style={{ 
                               cursor: match.awayTeamId ? 'pointer' : 'default', 
                               textDecoration: match.awayTeamId ? 'underline' : 'none',
                               opacity: match.awayTeamId ? 1 : 0.6
                             }}
                             onClick={() => match.awayTeamId && openTeamDetails(match.awayTeamId)}
                           >
                             {getTeamName(match.awayTeamId)}
                           </strong>
                           <div className="muted" style={{ fontSize: '0.85em' }}>
                             💪 {getTeamStrength(match.awayTeamId)}
                           </div>
                         </div>

                         {/* Desktop Buttons */}
                         {match.homeTeamId && match.awayTeamId && (
                           <>
                             {match.status !== 'played' && (
                               <button
                                 onClick={() => simulateMatch(match.id)}
                                 disabled={simulatingMatchId === match.id || displayedMatchday !== currentMatchday}
                                 title={displayedMatchday !== currentMatchday ? 'Nur Spiele des aktuellen Spieltags können simuliert werden' : ''}
                                 style={{
                                   marginLeft: 12,
                                   padding: '6px 12px',
                                   borderRadius: 4,
                                   border: 'none',
                                   background: displayedMatchday === currentMatchday ? '#6366f1' : '#666',
                                   color: '#fff',
                                   cursor: simulatingMatchId === match.id || displayedMatchday !== currentMatchday ? 'not-allowed' : 'pointer',
                                   opacity: simulatingMatchId === match.id || displayedMatchday !== currentMatchday ? 0.6 : 1,
                                   fontSize: '0.9em',
                                   whiteSpace: 'nowrap'
                                 }}
                               >
                                 {simulatingMatchId === match.id ? '⏳' : '▶️'} Sim
                               </button>
                             )}
                             {match.status === 'played' && (
                               <button
                                 onClick={() => loadMatchReport(match.id)}
                                 style={{
                                   marginLeft: 12,
                                   padding: '6px 12px',
                                   borderRadius: 4,
                                   border: 'none',
                                   background: '#10b981',
                                   color: '#fff',
                                   cursor: 'pointer',
                                   fontSize: '0.9em',
                                   whiteSpace: 'nowrap'
                                 }}
                               >
                                 📋 Bericht
                               </button>
                             )}
                           </>
                         )}
                       </div>

                       {/* Mobile: Vertical Layout */}
                       <div className="match-mobile" style={{
                         display: 'none',
                         flexDirection: 'column',
                         width: '100%'
                       }}>
                       {/* Heimteam */}
                       <div style={{ 
                         display: 'flex', 
                         justifyContent: 'space-between',
                         alignItems: 'flex-start',
                         marginBottom: '8px',
                         paddingBottom: '8px',
                         borderBottom: '1px solid rgba(255,255,255,0.1)'
                       }}>
                         <div style={{ flex: 1 }}>
                           <strong 
                             style={{ 
                               cursor: match.homeTeamId ? 'pointer' : 'default', 
                               textDecoration: match.homeTeamId ? 'underline' : 'none',
                               opacity: match.homeTeamId ? 1 : 0.6,
                               fontSize: '14px'
                             }}
                             onClick={() => match.homeTeamId && openTeamDetails(match.homeTeamId)}
                           >
                             {getTeamName(match.homeTeamId)}
                           </strong>
                           <div className="muted" style={{ fontSize: '0.8em', marginTop: '2px' }}>
                             💪 {getTeamStrength(match.homeTeamId)}
                           </div>
                         </div>
                         <div style={{ 
                           fontSize: '16px', 
                           fontWeight: 'bold',
                           textAlign: 'center',
                           minWidth: '50px'
                         }}>
                           {formatMatchResult(match).split(':')[0]}
                         </div>
                       </div>

                       {/* Auswärtsteam */}
                       <div style={{ 
                         display: 'flex', 
                         justifyContent: 'space-between',
                         alignItems: 'flex-start',
                         marginBottom: '8px'
                       }}>
                         <div style={{ flex: 1 }}>
                           <strong 
                             style={{ 
                               cursor: match.awayTeamId ? 'pointer' : 'default', 
                               textDecoration: match.awayTeamId ? 'underline' : 'none',
                               opacity: match.awayTeamId ? 1 : 0.6,
                               fontSize: '14px'
                             }}
                             onClick={() => match.awayTeamId && openTeamDetails(match.awayTeamId)}
                           >
                             {getTeamName(match.awayTeamId)}
                           </strong>
                           <div className="muted" style={{ fontSize: '0.8em', marginTop: '2px' }}>
                             💪 {getTeamStrength(match.awayTeamId)}
                           </div>
                         </div>
                         <div style={{ 
                           fontSize: '16px', 
                           fontWeight: 'bold',
                           textAlign: 'center',
                           minWidth: '50px'
                         }}>
                           {formatMatchResult(match).split(':')[1]}
                         </div>
                       </div>

                         {/* Buttons */}
                        {match.homeTeamId && match.awayTeamId && (
                          <div style={{
                            display: 'flex',
                            gap: '8px',
                            marginTop: '10px',
                            flexWrap: 'wrap'
                          }}>
                            {match.status !== 'played' && (
                              <button
                                onClick={() => simulateMatch(match.id)}
                                disabled={simulatingMatchId === match.id || displayedMatchday !== currentMatchday}
                                title={displayedMatchday !== currentMatchday ? 'Nur Spiele des aktuellen Spieltags können simuliert werden' : ''}
                                style={{
                                  padding: '8px 12px',
                                  borderRadius: 4,
                                  border: 'none',
                                  background: displayedMatchday === currentMatchday ? '#6366f1' : '#666',
                                  color: '#fff',
                                  cursor: simulatingMatchId === match.id || displayedMatchday !== currentMatchday ? 'not-allowed' : 'pointer',
                                  opacity: simulatingMatchId === match.id || displayedMatchday !== currentMatchday ? 0.6 : 1,
                                  fontSize: '0.85em',
                                  whiteSpace: 'nowrap',
                                  flex: '1 1 auto',
                                  minWidth: '80px'
                                }}
                              >
                                {simulatingMatchId === match.id ? '⏳' : '▶️'} Sim
                              </button>
                            )}
                            {match.status === 'played' && (
                              <button
                                onClick={() => loadMatchReport(match.id)}
                                style={{
                                  padding: '8px 12px',
                                  borderRadius: 4,
                                  border: 'none',
                                  background: '#10b981',
                                  color: '#fff',
                                  cursor: 'pointer',
                                  fontSize: '0.85em',
                                  whiteSpace: 'nowrap',
                                  flex: '1 1 auto',
                                  minWidth: '80px'
                                }}
                              >
                                📋 Bericht
                              </button>
                            )}
                          </div>
                        )}
                       </div>
                     </div>
                    )
                   })}
                  </div>
              ) : (
                <p className="muted">Keine Spiele verfügbar</p>
              )}
            </div>
          )}

           {tab === 'table' && (
             <div>
               <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                 <h4 style={{ margin: 0 }}>Tabelle</h4>
               </div>
               {loading ? (
                 <p className="muted">Lädt...</p>
               ) : standings.length > 0 ? (
                 <div style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
                   <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '400px' }}>
                     <thead>
                       <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.2)' }}>
                         <th style={{ padding: '8px', textAlign: 'left', fontSize: '12px' }}>Pos</th>
                         <th style={{ padding: '8px', textAlign: 'left', fontSize: '12px' }}>Team</th>
                         <th style={{ padding: '8px', textAlign: 'center', fontSize: '11px' }}>Sp</th>
                         <th style={{ padding: '8px', textAlign: 'center', fontSize: '11px' }}>W</th>
                         <th style={{ padding: '8px', textAlign: 'center', fontSize: '11px' }}>U</th>
                         <th style={{ padding: '8px', textAlign: 'center', fontSize: '11px' }}>V</th>
                         <th style={{ padding: '8px', textAlign: 'center', fontSize: '11px' }}>Tor</th>
                         <th style={{ padding: '8px', textAlign: 'center', fontSize: '12px' }}>Pkte</th>
                       </tr>
                     </thead>
                    <tbody>
                      {standings.map((row) => {
                        const rowClass = getPositionClass(row.position)
                        const bgColor = rowClass === 'promotion' ? 'rgba(52, 211, 153, 0.1)' : 
                                       rowClass === 'relegation' ? 'rgba(248, 113, 113, 0.1)' : 
                                       'transparent'
                        return (
                          <tr 
                            key={row.teamId} 
                            style={{ 
                              borderBottom: '1px solid rgba(255,255,255,0.05)',
                              background: bgColor,
                              cursor: 'pointer'
                            }}
                          >
                            <td style={{ padding: '8px', fontWeight: 'bold' }}>{row.position}</td>
                            <td 
                              style={{ 
                                padding: '8px', 
                                cursor: row.teamId ? 'pointer' : 'default', 
                                textDecoration: row.teamId ? 'underline' : 'none',
                                opacity: row.teamId ? 1 : 0.6
                              }}
                              onClick={() => row.teamId && openTeamDetails(row.teamId)}
                            >
                              {row.teamName}
                            </td>
                            <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold' }}>{row.teamStrength}</td>
                            <td style={{ padding: '8px', textAlign: 'center' }}>{row.played}</td>
                            <td style={{ padding: '8px', textAlign: 'center' }}>{row.won}</td>
                            <td style={{ padding: '8px', textAlign: 'center' }}>{row.drawn}</td>
                            <td style={{ padding: '8px', textAlign: 'center' }}>{row.lost}</td>
                            <td style={{ padding: '8px', textAlign: 'center' }}>{row.goalsFor}:{row.goalsAgainst}</td>
                            <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold' }}>{row.points}</td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                  <div style={{ marginTop: 16, fontSize: '0.9em' }}>
                    <div style={{ display: 'flex', gap: 16 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div style={{ width: 16, height: 16, background: 'rgba(52, 211, 153, 0.3)', borderRadius: 2 }}></div>
                        <span>Aufstieg (Platz 1-2)</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div style={{ width: 16, height: 16, background: 'rgba(248, 113, 113, 0.3)', borderRadius: 2 }}></div>
                        <span>Abstieg (Platz 9-12)</span>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <p className="muted">Keine Tabelle verfügbar</p>
              )}
            </div>
          )}

          {tab === 'cup' && (
            <CupSection selectedCountry={selectedCountry} leagueId={selectedLeague} />
          )}
        </div>
      </div>

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
            maxWidth: '700px',
            width: '90%',
            maxHeight: '90vh',
            overflowY: 'auto'
          }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 16 }}>
              <div>
                <h3 style={{ margin: 0, marginBottom: 8 }}>{selectedTeamDetails.teamName}</h3>
                <div className="muted" style={{ fontSize: '0.9em', lineHeight: '1.4' }}>
                  <div>💪 Teamstärke: <strong>{selectedTeamDetails.teamStrength}</strong></div>
                  <div>🏟️ Stadion: <strong>{selectedTeamDetails.stadiumCapacity?.toLocaleString() || 'Unbekannt'} Plätze</strong></div>
                  {selectedTeamDetails.country && <div>🌍 Land: <strong>{selectedTeamDetails.country}</strong></div>}
                  {selectedTeamDetails.leagueName && <div>🏆 Liga: <strong>{selectedTeamDetails.leagueName}</strong></div>}
                  {selectedTeamDetails.budget !== undefined && (
                    <div style={{ color: '#ffd700', marginTop: 4 }}>
                      💰 Budget: <strong>{(selectedTeamDetails.budget / 1000000).toFixed(2)}M €</strong> <span style={{ fontSize: '0.8em', opacity: 0.7 }}>(Test-Info)</span>
                    </div>
                  )}
                </div>
              </div>
              <button
                onClick={() => setShowTeamModal(false)}
                style={{
                  background: 'rgba(255,255,255,0.1)',
                  border: 'none',
                  color: '#fff',
                  fontSize: '20px',
                  cursor: 'pointer',
                  padding: '0 8px',
                  minWidth: '32px'
                }}
              >
                ✕
              </button>
            </div>

            {/* Aufstellung Tab */}
            <div style={{ marginBottom: '24px' }}>
              <h4 style={{ marginBottom: '12px' }}>Aufstellung (Spieltag)</h4>
              {selectedTeamDetails.lineup && selectedTeamDetails.lineup.length > 0 ? (
                <div>
                  {selectedTeamDetails.lineup.map((player) => (
                    <div key={player.playerId} className="card" style={{ marginBottom: 8, padding: '10px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <strong>{player.playerName}</strong>
                          <div className="muted" style={{ fontSize: '0.85em' }}>
                            {player.position} • {player.age || '?'} Jahre
                          </div>
                        </div>
                        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                          <div style={{ textAlign: 'right' }}>
                            <strong>{player.rating}</strong>
                            <div className="muted" style={{ fontSize: '0.85em' }}>Rating</div>
                          </div>
                          <div style={{ textAlign: 'right' }}>
                            <strong style={{ color: player.fitness >= 80 ? '#4CAF50' : player.fitness >= 50 ? '#FFC107' : '#F44336' }}>
                              {player.fitness || '?'}
                            </strong>
                            <div className="muted" style={{ fontSize: '0.85em' }}>Fitness</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="muted">Keine Aufstellung verfügbar</p>
              )}
            </div>

            {/* Alle Spieler im Kader */}
            <div>
              <h4 style={{ marginBottom: '12px' }}>Kader ({selectedTeamDetails.allPlayers?.length || 0} Spieler)</h4>
              {selectedTeamDetails.allPlayers && selectedTeamDetails.allPlayers.length > 0 ? (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9em' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.2)' }}>
                        <th style={{ padding: '8px', textAlign: 'left' }}>Name</th>
                        <th style={{ padding: '8px', textAlign: 'center' }}>Pos</th>
                        <th style={{ padding: '8px', textAlign: 'center' }}>Rating</th>
                        <th style={{ padding: '8px', textAlign: 'center' }}>Fitness</th>
                        <th style={{ padding: '8px', textAlign: 'center' }}>Alter</th>
                        <th style={{ padding: '8px', textAlign: 'center' }}>Land</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedTeamDetails.allPlayers.map((player) => (
                        <tr key={player.playerId} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                          <td style={{ padding: '8px' }}>{player.playerName}</td>
                          <td style={{ padding: '8px', textAlign: 'center', color: '#999', fontSize: '0.9em' }}>{player.position}</td>
                          <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold' }}>{player.rating}</td>
                          <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold', color: player.fitness >= 80 ? '#4CAF50' : player.fitness >= 50 ? '#FFC107' : '#F44336' }}>
                            {player.fitness || '?'}
                          </td>
                          <td style={{ padding: '8px', textAlign: 'center', color: '#999' }}>{player.age}</td>
                          <td style={{ padding: '8px', textAlign: 'center', color: '#999', fontSize: '0.85em' }}>{player.country}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="muted">Kein Kader verfügbar</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Statistik-Tab */}
      {tab === 'statistics' && (
        <div>
          <h4>Spieler-Statistiken</h4>
          {loading ? (
            <p className="muted">Lädt...</p>
          ) : statistics && Object.keys(statistics).length > 0 ? (
            <div>
              {/* 1. Torschützen */}
              <div style={{ marginBottom: 24 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <h5 style={{ margin: 0, color: '#4ade80' }}>⚽ Torschützen</h5>
                  {statistics.totalScorers > 20 && (
                    <button
                      onClick={() => {
                        if (!showAllScorers) {
                          // Lade alle Torschützen
                          const leagueId = selectedLeague || userLeagueId
                          fetch(`${API_BASE}/api/v2/schedule/all-scorers/league/${leagueId}`)
                            .then(r => r.json())
                            .then(data => {
                              setStatistics({...statistics, topScorers: data})
                              setShowAllScorers(true)
                            })
                            .catch(e => console.error('Fehler beim Laden aller Torschützen:', e))
                        } else {
                          // Zurück zu Top 20
                          setShowAllScorers(false)
                          loadStatistics()
                        }
                      }}
                      style={{
                        padding: '6px 12px',
                        borderRadius: 4,
                        border: '1px solid rgba(255,255,255,0.2)',
                        background: 'rgba(255,255,255,0.1)',
                        color: '#fff',
                        cursor: 'pointer',
                        fontSize: '0.9em'
                      }}
                    >
                      {showAllScorers ? '📋 Top 20' : '📋 Alle anzeigen'}
                    </button>
                  )}
                </div>
                {statistics.topScorers && statistics.topScorers.length > 0 ? (
                  <div>
                    <div style={{ overflowX: 'auto' }}>
                      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                          <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.2)' }}>
                            <th style={{ padding: '8px', textAlign: 'left' }}>Spieler</th>
                            <th style={{ padding: '8px', textAlign: 'left' }}>Position</th>
                            <th style={{ padding: '8px', textAlign: 'left' }}>Team</th>
                            <th style={{ padding: '8px', textAlign: 'center' }}>Tore</th>
                          </tr>
                        </thead>
                        <tbody>
                          {statistics.topScorers.map((player, idx) => (
                            <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                              <td style={{ padding: '8px' }}>{player.playerName}</td>
                              <td style={{ padding: '8px', fontSize: '0.9em', color: '#999' }}>{player.position}</td>
                              <td style={{ padding: '8px' }}>{player.teamName}</td>
                              <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold', color: '#4ade80' }}>{player.goals}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {!showAllScorers && statistics.totalScorers > 20 && (
                      <div style={{ marginTop: 8, fontSize: '0.9em', color: '#999', textAlign: 'center' }}>
                        Zeige 20 von {statistics.totalScorers} Torschützen
                      </div>
                    )}
                  </div>
                ) : (
                  <p className="muted">Keine Torschützen</p>
                )}
              </div>

              {/* 2. Vorlagengeber */}
              <div style={{ marginBottom: 24 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <h5 style={{ margin: 0, color: '#60a5fa' }}>🎯 Vorlagengeber</h5>
                  {statistics.totalAssisters > 20 && (
                    <button
                      onClick={() => {
                        if (!showAllAssisters) {
                          // Lade alle Vorlagengeber
                          const leagueId = selectedLeague || userLeagueId
                          fetch(`${API_BASE}/api/v2/schedule/all-assisters/league/${leagueId}`)
                            .then(r => r.json())
                            .then(data => {
                              setStatistics({...statistics, topAssisters: data})
                              setShowAllAssisters(true)
                            })
                            .catch(e => console.error('Fehler beim Laden aller Vorlagengeber:', e))
                        } else {
                          // Zurück zu Top 20
                          setShowAllAssisters(false)
                          loadStatistics()
                        }
                      }}
                      style={{
                        padding: '6px 12px',
                        borderRadius: 4,
                        border: '1px solid rgba(255,255,255,0.2)',
                        background: 'rgba(255,255,255,0.1)',
                        color: '#fff',
                        cursor: 'pointer',
                        fontSize: '0.9em'
                      }}
                    >
                      {showAllAssisters ? '📋 Top 20' : '📋 Alle anzeigen'}
                    </button>
                  )}
                </div>
                {statistics.topAssisters && statistics.topAssisters.length > 0 ? (
                  <div>
                    <div style={{ overflowX: 'auto' }}>
                      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                          <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.2)' }}>
                            <th style={{ padding: '8px', textAlign: 'left' }}>Spieler</th>
                            <th style={{ padding: '8px', textAlign: 'left' }}>Position</th>
                            <th style={{ padding: '8px', textAlign: 'left' }}>Team</th>
                            <th style={{ padding: '8px', textAlign: 'center' }}>Vorlagen</th>
                          </tr>
                        </thead>
                        <tbody>
                          {statistics.topAssisters.map((player, idx) => (
                            <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                              <td style={{ padding: '8px' }}>{player.playerName}</td>
                              <td style={{ padding: '8px', fontSize: '0.9em', color: '#999' }}>{player.position}</td>
                              <td style={{ padding: '8px' }}>{player.teamName}</td>
                              <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold', color: '#60a5fa' }}>{player.goals}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {!showAllAssisters && statistics.totalAssisters > 20 && (
                      <div style={{ marginTop: 8, fontSize: '0.9em', color: '#999', textAlign: 'center' }}>
                        Zeige 20 von {statistics.totalAssisters} Vorlagengebern
                      </div>
                    )}
                  </div>
                ) : (
                  <p className="muted">Keine Vorlagengeber</p>
                )}
              </div>

              {/* 3. Zu Null Spiele */}
              <div style={{ marginBottom: 24 }}>
                <h5 style={{ marginBottom: 12, color: '#10b981' }}>🛡️ Zu Null Spiele</h5>
                {statistics.cleanSheets && statistics.cleanSheets.length > 0 ? (
                  <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                      <thead>
                        <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.2)' }}>
                          <th style={{ padding: '8px', textAlign: 'left' }}>Team</th>
                          <th style={{ padding: '8px', textAlign: 'center' }}>Zu Null Spiele</th>
                        </tr>
                      </thead>
                      <tbody>
                        {statistics.cleanSheets.map((team, idx) => (
                          <tr key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                            <td style={{ padding: '8px' }}>{team.teamName}</td>
                            <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold', color: '#10b981' }}>{team.cleanSheets}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="muted">Keine Zu Null Spiele</p>
               )}
               </div>
             </div>
           ) : (
             <p className="muted">Keine Statistiken verfügbar</p>
           )}
         </div>
       )}

      {/* Spielbericht Modal */}
      {showMatchReport && matchReport && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }} onClick={() => setShowMatchReport(false)}>
          <div style={{
            background: '#1a1a1a',
            border: '1px solid rgba(255,255,255,0.2)',
            borderRadius: '8px',
            padding: '20px',
            maxWidth: '700px',
            width: '90%',
            maxHeight: '80vh',
            overflowY: 'auto'
          }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 16 }}>
              <div>
                <h3 style={{ margin: 0, marginBottom: 8 }}>📋 Spielbericht</h3>
                <div style={{ fontSize: '1.3em', fontWeight: 'bold', marginBottom: 8 }}>
                  {matchReport.homeTeamName} {matchReport.homeGoals} : {matchReport.awayGoals} {matchReport.awayTeamName}
                </div>
                {matchReport.attendance && (
                  <div style={{ 
                    fontSize: '14px', 
                    color: '#999', 
                    marginBottom: 12,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px'
                  }}>
                    <span>👥</span>
                    <span>{matchReport.attendance.toLocaleString()} Zuschauer</span>
                  </div>
                )}
              </div>
              <button
                onClick={() => setShowMatchReport(false)}
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

            <div>
              <h4>⚽ Tore</h4>
              {matchReport.events && matchReport.events.length > 0 ? (
                <div>
                  {matchReport.events.map((event, idx) => {
                    return (
                      <div key={idx} style={{
                        padding: '8px',
                        marginBottom: '4px',
                        background: 'rgba(255,255,255,0.05)',
                        borderLeft: '3px solid #4ade80',
                        borderRadius: '2px'
                      }}>
                        <span style={{ marginRight: '8px' }}>⚽</span>
                        <strong style={{ color: '#4ade80' }}>{event.minute}'</strong>
                        <span style={{ marginLeft: '8px' }}>
                          {event.playerName} 
                          {event.teamName && <span style={{ color: '#999', marginLeft: '4px' }}>({event.teamName})</span>}
                        </span>
                      </div>
                    )
                  })}
                </div>
              ) : (
                <p className="muted">Keine Tore</p>
              )}
            </div>

            {/* Spieler-Bewertungen */}
            {(matchReport.homePlayerRatings || matchReport.awayPlayerRatings) && (
              <div style={{ marginTop: 24 }}>
                <h4>📊 Spielerbewertungen</h4>
                
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                   {/* Heimteam Bewertungen */}
                  <div>
                    <h5 style={{ marginBottom: 12, color: '#4ade80' }}>{matchReport.homeTeamName}</h5>
                    {matchReport.homePlayerRatings && matchReport.homePlayerRatings.length > 0 ? (
                      <div style={{ fontSize: '14px' }}>
                        {matchReport.homePlayerRatings.map((player, idx) => (
                          <div key={idx} style={{
                            padding: '8px',
                            marginBottom: '6px',
                            background: 'rgba(255,255,255,0.05)',
                            borderRadius: '4px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                          }}>
                            <div style={{ flex: 1 }}>
                              <div style={{ fontWeight: 500 }}>{player.playerName}</div>
                              <div style={{ fontSize: '12px', color: '#999', marginTop: 2 }}>
                                {player.position}
                                {player.goals > 0 && <span style={{ marginLeft: 6 }}>⚽ {player.goals}</span>}
                                {player.assists > 0 && <span style={{ marginLeft: 6 }}>🎯 {player.assists}</span>}
                                {player.yellowCards > 0 && <span style={{ marginLeft: 6 }}>🟨 {player.yellowCards}</span>}
                                {player.redCards > 0 && <span style={{ marginLeft: 6 }}>🟥 {player.redCards}</span>}
                              </div>
                            </div>
                            <div style={{
                              fontWeight: 'bold',
                              fontSize: '16px',
                              padding: '4px 8px',
                              borderRadius: '4px',
                              background: player.rating <= 2.0 ? '#4ade80' : 
                                         player.rating <= 3.0 ? '#60a5fa' :
                                         player.rating <= 4.0 ? '#fbbf24' :
                                         player.rating <= 5.0 ? '#fb923c' : '#ef4444',
                              color: '#000'
                            }}>
                              {player.rating.toFixed(1)}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p style={{ color: '#999', fontSize: '14px' }}>Keine Bewertungen</p>
                    )}
                  </div>

                  {/* Auswärtsteam Bewertungen */}
                  <div>
                    <h5 style={{ marginBottom: 12, color: '#60a5fa' }}>{matchReport.awayTeamName}</h5>
                    {matchReport.awayPlayerRatings && matchReport.awayPlayerRatings.length > 0 ? (
                      <div style={{ fontSize: '14px' }}>
                        {matchReport.awayPlayerRatings.map((player, idx) => (
                          <div key={idx} style={{
                            padding: '8px',
                            marginBottom: '6px',
                            background: 'rgba(255,255,255,0.05)',
                            borderRadius: '4px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                          }}>
                            <div style={{ flex: 1 }}>
                              <div style={{ fontWeight: 500 }}>{player.playerName}</div>
                              <div style={{ fontSize: '12px', color: '#999', marginTop: 2 }}>
                                {player.position}
                                {player.goals > 0 && <span style={{ marginLeft: 6 }}>⚽ {player.goals}</span>}
                                {player.assists > 0 && <span style={{ marginLeft: 6 }}>🎯 {player.assists}</span>}
                                {player.yellowCards > 0 && <span style={{ marginLeft: 6 }}>🟨 {player.yellowCards}</span>}
                                {player.redCards > 0 && <span style={{ marginLeft: 6 }}>🟥 {player.redCards}</span>}
                              </div>
                            </div>
                            <div style={{
                              fontWeight: 'bold',
                              fontSize: '16px',
                              padding: '4px 8px',
                              borderRadius: '4px',
                              background: player.rating <= 2.0 ? '#4ade80' : 
                                         player.rating <= 3.0 ? '#60a5fa' :
                                         player.rating <= 4.0 ? '#fbbf24' :
                                         player.rating <= 5.0 ? '#fb923c' : '#ef4444',
                              color: '#000'
                            }}>
                              {player.rating.toFixed(1)}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p style={{ color: '#999', fontSize: '14px' }}>Keine Bewertungen</p>
                    )}
                  </div>
                </div>

                {/* Noten-Legende */}
                <div style={{ 
                  marginTop: 16, 
                  padding: 12, 
                  background: 'rgba(255,255,255,0.05)', 
                  borderRadius: 4,
                  fontSize: '12px'
                }}>
                  <strong>Noten-Legende:</strong>
                  <div style={{ display: 'flex', gap: 12, marginTop: 6, flexWrap: 'wrap' }}>
                    <span><span style={{ background: '#4ade80', color: '#000', padding: '2px 6px', borderRadius: 3, fontWeight: 'bold' }}>1.0-2.0</span> Hervorragend</span>
                    <span><span style={{ background: '#60a5fa', color: '#000', padding: '2px 6px', borderRadius: 3, fontWeight: 'bold' }}>2.1-3.0</span> Gut</span>
                    <span><span style={{ background: '#fbbf24', color: '#000', padding: '2px 6px', borderRadius: 3, fontWeight: 'bold' }}>3.1-4.0</span> Durchschnitt</span>
                    <span><span style={{ background: '#fb923c', color: '#000', padding: '2px 6px', borderRadius: 3, fontWeight: 'bold' }}>4.1-5.0</span> Schwach</span>
                    <span><span style={{ background: '#ef4444', color: '#000', padding: '2px 6px', borderRadius: 3, fontWeight: 'bold' }}>5.1-6.0</span> Sehr schwach</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
