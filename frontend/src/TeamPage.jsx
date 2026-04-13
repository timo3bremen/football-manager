import React, { useMemo } from 'react'
import { useGame } from './GameContext'

export default function TeamPage(){
  const { roster, lineup, formationRows, currentFormation, setFormation, assignPlayerToSlot, swapSlots, removePlayerFromSlot } = useGame()

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
      <h3>Kader</h3>
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
                       title={slotId}
                     >
                       {player ? (
                         <div>
                           <div>{player.name}</div>
                           <div className="draggable-hint">{player.position}</div>
                           <div style={{fontSize:'0.75em',color:'#fbbf24',marginTop:2}}>⭐ {player.rating}</div>
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
                    draggable 
                    onDragStart={(e)=>onDragStartFromRoster(e,p.id)}
                    style={{
                      backgroundColor: inLineup ? 'rgba(52, 211, 153, 0.2)' : 'transparent',
                      borderLeft: inLineup ? '3px solid #10b981' : '3px solid transparent',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <div style={{flex:1}}>
                      <div style={{fontWeight: inLineup ? 'bold' : 'normal', color: inLineup ? '#10b981' : 'inherit'}}>
                        {p.name} {inLineup ? '✓' : ''}
                      </div>
                      <div style={{fontSize: '0.85em', color:'var(--muted)'}}>{p.position} • {p.country}</div>
                      <div style={{fontSize: '0.8em', color:'#fbbf24',marginTop:2}}>⭐ Rating: {p.rating}</div>
                    </div>
                    {!inLineup && (
                      <button
                        onClick={() => addPlayerToLineup(p.id)}
                        style={{
                          marginLeft: 12,
                          padding: '4px 12px',
                          borderRadius: 4,
                          border: 'none',
                          background: '#6366f1',
                          color: '#fff',
                          cursor: 'pointer',
                          fontSize: '0.8em',
                          whiteSpace: 'nowrap',
                          flexShrink: 0
                        }}
                      >
                        ➕ Aufstellung
                      </button>
                    )}
                    {inLineup && (
                      <button
                        onClick={() => removePlayerFromSlot(Object.entries(lineup).find(([_, pid]) => pid === p.id)?.[0])}
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
  )
}
