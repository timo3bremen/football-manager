import React, { useMemo, useState } from 'react'
import { useGame } from './GameContext'
import ContractsSection from './ContractsSection'
import PlayerDetailsModal from './PlayerDetailsModal'

export default function TeamPage(){
  const { roster, lineup, formationRows, currentFormation, setFormation, assignPlayerToSlot, swapSlots, removePlayerFromSlot } = useGame()
  const [teamSubTab, setTeamSubTab] = useState('lineup') // 'lineup' oder 'contracts'
  const [selectedPlayerId, setSelectedPlayerId] = useState(null) // Für Player Details Modal

  // Debug logging
  React.useEffect(() => {
    console.log('[TeamPage] Roster:', roster)
    console.log('[TeamPage] Lineup:', lineup)
    console.log('[TeamPage] FormationRows:', formationRows)
  }, [roster, lineup, formationRows])

  const assignedCount = useMemo(() => Object.values(lineup || {}).filter(Boolean).length, [lineup])
  
  // Berechne die Teamstärke (Summe der Ratings aller Spieler in der Aufstellung)
  const teamStrength = useMemo(() => {
    let strength = 0
    Object.values(lineup || {}).forEach(playerId => {
      if (playerId) {
        const player = roster.find(r => r.id === playerId || String(r.id) === String(playerId))
        if (player && player.rating) {
          strength += player.rating
        }
      }
    })
    return strength
  }, [lineup, roster])

  // Finde erste freie Position für einen Spieler
  const findFirstFreeSlot = (playerPosition) => {
    for (const row of formationRows) {
      for (const slotId of row) {
        if (!lineup[slotId]) {
          const slotPosition = getSlotPosition(slotId)
          if (!slotPosition || slotPosition === playerPosition) {
            return slotId
          }
        }
      }
    }
    return null
  }

  const addPlayerToLineup = (playerId) => {
    const player = roster.find(r => r.id === playerId)
    if (!player) return

    if (player.isInjured) {
      alert(`${player.name} ist verletzt und kann nicht spielen! (${player.injuryMatchdaysRemaining} Tage ausfallzeit)`)
      return
    }

    if (player.isSuspended) {
      alert(`${player.name} ist gesperrt und kann nicht spielen! (${player.suspensionMatchesRemaining} Spiele Sperrung)`)
      return
    }

    const freeSlot = findFirstFreeSlot(player.position)
    if (freeSlot) {
      assignPlayerToSlot(freeSlot, playerId)
    } else {
      alert(`Keine freie Position für einen ${player.position} Spieler verfügbar!`)
    }
  }

  // Bestimme die erforderliche Position basierend auf dem Slot-Namen
  const getSlotPosition = (slotId) => {
    if (slotId === 'GK') return 'GK'
    if (slotId.startsWith('D')) return 'DEF'
    if (slotId.startsWith('M')) return 'MID'
    if (slotId.startsWith('F')) return 'FWD'
    return null
  }

  // Prüfe ob ein Spieler in der Aufstellung ist
  const isPlayerInLineup = (playerId) => {
    return Object.values(lineup || {}).some(pid => pid === playerId || String(pid) === String(playerId))
  }

  const formations = ['4-4-2','4-3-3','3-5-2']

  function onDragStartFromRoster(e, playerId){
    e.dataTransfer.setData('application/json', JSON.stringify({playerId, fromSlot: null}))
  }

  function onDragStartFromSlot(e, slotId){
    const playerId = lineup && lineup[slotId]
    if (!playerId) {
      e.preventDefault()
      return
    }
    e.dataTransfer.setData('application/json', JSON.stringify({playerId, fromSlot: slotId}))
  }

  function onDropToSlot(e, slotId){
    e.preventDefault()
    try{
      const data = JSON.parse(e.dataTransfer.getData('application/json'))
      if (!data) return
      const { playerId, fromSlot } = data
      const player = roster.find(r => r.id === playerId || String(r.id) === String(playerId))
      
      // Validiere die Position
      if (!player) return
      
      // Prüfe ob Spieler gesperrt ist
      if (player.isSuspended) {
        alert(`${player.name} ist gesperrt und kann nicht spielen! (${player.suspensionMatchesRemaining} Spiele Sperrung)`)
        return
      }
      
      // Prüfe ob Spieler verletzt ist
      if (player.isInjured) {
        alert(`${player.name} ist verletzt und kann nicht spielen! (${player.injuryMatchdaysRemaining} Tage ausfallzeit)`)
        return
      }
      
      const allowedPosition = getSlotPosition(slotId)
      if (allowedPosition && player.position !== allowedPosition) {
        alert(`Nur ${allowedPosition} Spieler können auf dieser Position eingesetzt werden!`)
        return
      }
      
      if (fromSlot){
        // swap source slot and target slot
        swapSlots(fromSlot, slotId)
      } else {
        // assign player from roster to target slot
        assignPlayerToSlot(slotId, playerId)
      }
    }catch(err){console.error(err)}
  }

  function onDropToRoster(e){
    e.preventDefault()
    try{
      const data = JSON.parse(e.dataTransfer.getData('application/json'))
      if (!data) return
      const { fromSlot } = data
      if (fromSlot){
        removePlayerFromSlot(fromSlot)
      }
    }catch(err){console.error(err)}
  }

  return (
    <div>
      <h3>Team</h3>
      
      {/* Sub-Tab Navigation */}
      <div className="menu" style={{marginBottom: 12}}>
        <button 
          className={teamSubTab === 'lineup' ? 'active' : ''} 
          onClick={() => setTeamSubTab('lineup')}
        >
          🏟️ Aufstellung
        </button>
        <button 
          className={teamSubTab === 'contracts' ? 'active' : ''} 
          onClick={() => setTeamSubTab('contracts')}
        >
          📋 Verträge
        </button>
      </div>

      {/* Aufstellung Sub-Tab */}
      {teamSubTab === 'lineup' && (
        <div>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
            <p className="muted">Ziehe Spieler per Drag & Drop auf die freien Positionen.</p>
            <div style={{display:'flex',gap:16,alignItems:'center'}}>
              <div style={{textAlign:'right'}}>
                <div className="muted" style={{fontSize:'0.9em'}}>Teamstärke</div>
                <div style={{fontSize:'1.3em',fontWeight:'bold',color:'#4ade80'}}>💪 {teamStrength}</div>
              </div>
              <div>
                <label className="muted" style={{marginRight:8}}>Formation:</label>
                <select value={currentFormation} onChange={e=>setFormation(e.target.value)} className="input">
                  {formations.map(f=> <option key={f} value={f}>{f}</option>)}
                </select>
              </div>
            </div>
          </div>

          <div className="columns">
            <div className="col-2 card">
              <h4>Aufstellung ({assignedCount}/{Object.keys(lineup||{}).length})</h4>
              <div className="pitch">
                {formationRows.map((row,ri)=> (
                  <div key={ri} className="pitch-row">
                    {row.map(slotId => {
                       const pid = lineup[slotId]
                       const player = roster.find(r => r.id === pid || String(r.id) === String(pid))
                       return (
                          <div key={slotId}
                            className={`slot ${!player ? 'empty' : ''}`}
                            draggable={!!player}
                            onDragStart={player ? (e)=>onDragStartFromSlot(e, slotId) : undefined}
                            onDragOver={(e)=>e.preventDefault()}
                            onDrop={(e)=>onDropToSlot(e, slotId)}
                            onClick={() => player && setSelectedPlayerId(player.id)}
                            title={slotId}
                            style={{ cursor: player ? 'pointer' : 'default' }}
                          >
                            {player ? (
                              <div style={{width:'100%',height:'100%',display:'flex',flexDirection:'column',justifyContent:'space-between',padding:'4px', opacity: (player.isInjured || player.isSuspended) ? 0.6 : 1, backgroundColor: player.isSuspended ? 'rgba(239, 68, 68, 0.2)' : player.isInjured ? 'rgba(239, 68, 68, 0.1)' : 'transparent', borderRadius: '4px'}}>
                                <div>
                                  <div style={{fontSize:'0.85em',fontWeight:'bold'}}>
                                    {player.isSuspended ? '🟥 ' : player.isInjured ? '🤕 ' : ''}{player.name}
                                  </div>
                                  {player.isSuspended && (
                                    <div style={{fontSize:'0.65em', color:'#ef4444', marginTop: '2px'}}>
                                      {player.suspensionMatchesRemaining}S gesperrt
                                    </div>
                                  )}
                                  {player.isInjured && (
                                    <div style={{fontSize:'0.65em', color:'#ef4444', marginTop: '2px'}}>
                                      {player.injuryMatchdaysRemaining} Tage
                                    </div>
                                  )}
                                  <div className="draggable-hint" style={{fontSize:'0.7em'}}>{player.position} • {player.age || '?'}J</div>
                                  <div style={{fontSize:'0.7em',color:'#fbbf24',marginTop:2}}>⭐ {player.rating}</div>
                                </div>
                                
                                {/* Fitness Bar */}
                                <div style={{marginTop:'4px'}}>
                                  {(() => {
                                    const fitness = player.fitness !== undefined && player.fitness !== null ? player.fitness : 100;
                                    const fitnessColor = fitness >= 80 ? '#10b981' : fitness >= 50 ? '#f59e0b' : '#ef4444';
                                    return (
                                      <>
                                        <div style={{
                                          width:'100%',
                                          height:'6px',
                                          background:'rgba(255,255,255,0.1)',
                                          borderRadius:'3px',
                                          overflow:'hidden',
                                          border:'1px solid rgba(255,255,255,0.2)'
                                        }}>
                                          <div style={{
                                            width:`${fitness}%`,
                                            height:'100%',
                                            background: fitnessColor,
                                            transition:'all 0.3s ease'
                                          }}></div>
                                        </div>
                                        <div style={{fontSize:'0.6em',color:'var(--muted)',marginTop:'2px',textAlign:'center'}}>
                                          {fitness}%
                                        </div>
                                      </>
                                    );
                                  })()}
                                </div>
                              </div>
                            ) : (
                              <div className="muted">{slotId}</div>
                            )}
                          </div>
                       )
                     })}
                  </div>
                ))}
              </div>
            </div>

             <div className="col-1 card" onDragOver={(e)=>e.preventDefault()} onDrop={onDropToRoster}>
              <h4>Spieler (Roster)</h4>
              <div className="draggable-hint">Ziehe Spieler auf das Feld oder hierher, um sie abzusetzen.</div>
              <ul className="roster-list">
                  {roster.map(p => {
                   const inLineup = isPlayerInLineup(p.id)
                   return (
                      <li 
                        key={p.id} 
                        className="roster-item" 
                        draggable={!p.isInjured}
                        onDragStart={(e)=>onDragStartFromRoster(e,p.id)}
                        onClick={() => setSelectedPlayerId(p.id)}
                        style={{
                          backgroundColor: p.isInjured ? 'rgba(239, 68, 68, 0.3)' : inLineup ? 'rgba(52, 211, 153, 0.2)' : 'transparent',
                          borderLeft: p.isInjured ? '3px solid #ef4444' : inLineup ? '3px solid #10b981' : '3px solid transparent',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          cursor: p.isInjured ? 'not-allowed' : 'pointer',
                          opacity: p.isInjured ? 0.7 : 1
                        }}
                      >
                        <div style={{flex:1}}>
                          <div style={{fontWeight: inLineup ? 'bold' : 'normal', color: inLineup ? '#10b981' : 'inherit'}}>
                            {p.isSuspended ? '🟥 ' : p.isInjured ? '🤕 ' : ''}{p.name} {inLineup ? '✓' : ''}
                            {p.isSuspended && <span style={{color: '#ef4444', fontSize: '0.8em', marginLeft: '4px'}}>({p.suspensionMatchesRemaining}S)</span>}
                            {p.isInjured && <span style={{color: '#ef4444', fontSize: '0.8em', marginLeft: '4px'}}>({p.injuryMatchdaysRemaining}T)</span>}
                          </div>
                          <div style={{fontSize: '0.85em', color:'var(--muted)'}}>{p.position} • {p.age || '?'} Jahre • {p.country}</div>
                          {p.isSuspended && (
                            <div style={{fontSize: '0.75em', color: '#ef4444', marginTop: '2px'}}>
                              Gesperrt: {p.suspensionReason || 'Unbekannter Grund'}
                            </div>
                          )}
                          {p.isInjured && (
                            <div style={{fontSize: '0.75em', color: '#ef4444', marginTop: '2px'}}>
                              Verletzt: {p.injuryName}
                            </div>
                          )}
                          <div style={{fontSize: '0.8em', color:'#fbbf24',marginTop:2}}>⭐ Rating: {p.rating}</div>
                          
                          {/* Fitness Bar im Roster */}
                          <div style={{marginTop:'6px'}}>
                            {(() => {
                              const fitness = p.fitness !== undefined && p.fitness !== null ? p.fitness : 100;
                              const fitnessColor = fitness >= 80 ? '#10b981' : fitness >= 50 ? '#f59e0b' : '#ef4444';
                              return (
                                <>
                                  <div style={{
                                    width:'100%',
                                    height:'4px',
                                    background:'rgba(255,255,255,0.1)',
                                    borderRadius:'2px',
                                    overflow:'hidden',
                                    border:'1px solid rgba(255,255,255,0.15)'
                                  }}>
                                    <div style={{
                                      width:`${fitness}%`,
                                      height:'100%',
                                      background: fitnessColor,
                                      transition:'all 0.3s ease'
                                    }}></div>
                                  </div>
                                  <div style={{fontSize:'0.65em',color:'var(--muted)',marginTop:'2px'}}>
                                    💪 Fitness: {fitness}%
                                  </div>
                                </>
                              );
                            })()}
                          </div>
                        </div>
                        {!inLineup && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              addPlayerToLineup(p.id)
                            }}
                            disabled={p.isInjured || p.isSuspended}
                            style={{
                              marginLeft: 12,
                              padding: '4px 12px',
                              borderRadius: 4,
                              border: 'none',
                              background: (p.isInjured || p.isSuspended) ? '#9ca3af' : '#6366f1',
                              color: '#fff',
                              cursor: (p.isInjured || p.isSuspended) ? 'not-allowed' : 'pointer',
                              fontSize: '0.8em',
                              whiteSpace: 'nowrap',
                              flexShrink: 0,
                              opacity: (p.isInjured || p.isSuspended) ? 0.6 : 1
                            }}
                            title={p.isInjured ? 'Spieler ist verletzt' : p.isSuspended ? 'Spieler ist gesperrt' : ''}
                          >
                            ➕ Aufstellung
                          </button>
                        )}
                        {inLineup && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              removePlayerFromSlot(Object.entries(lineup).find(([_, pid]) => pid === p.id)?.[0])
                            }}
                            style={{
                              marginLeft: 12,
                              padding: '4px 12px',
                              borderRadius: 4,
                              border: 'none',
                              background: '#ef4444',
                              color: '#fff',
                              cursor: 'pointer',
                              fontSize: '0.8em',
                              whiteSpace: 'nowrap',
                              flexShrink: 0
                            }}
                          >
                            ➖ Entfernen
                          </button>
                        )}
                      </li>
                   )
                 })}
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* Verträge Sub-Tab */}
      {teamSubTab === 'contracts' && <ContractsSection />}

      {/* Player Details Modal */}
      {selectedPlayerId && (
        <PlayerDetailsModal
          playerId={selectedPlayerId}
          onClose={() => setSelectedPlayerId(null)}
        />
      )}
    </div>
  )
}
