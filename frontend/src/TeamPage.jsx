import React, { useMemo } from 'react'
import { useGame } from './GameContext'

export default function TeamPage(){
  const { roster, lineup, formationRows, currentFormation, setFormation, assignPlayerToSlot, swapSlots, removePlayerFromSlot } = useGame()

  const assignedCount = useMemo(() => Object.values(lineup || {}).filter(Boolean).length, [lineup])

  const formations = ['4-4-2','4-3-3','3-5-2']

  function onDragStartFromRoster(e, playerId){
    e.dataTransfer.setData('application/json', JSON.stringify({playerId, fromSlot: null}))
  }

  function onDragStartFromSlot(e, slotId){
    const playerId = lineup[slotId]
    e.dataTransfer.setData('application/json', JSON.stringify({playerId, fromSlot: slotId}))
  }

  function onDropToSlot(e, slotId){
    e.preventDefault()
    try{
      const data = JSON.parse(e.dataTransfer.getData('application/json'))
      if (!data) return
      const { playerId, fromSlot } = data
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
        <div>
          <label className="muted" style={{marginRight:8}}>Formation:</label>
          <select value={currentFormation} onChange={e=>setFormation(e.target.value)} className="input">
            {formations.map(f=> <option key={f} value={f}>{f}</option>)}
          </select>
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
                  const player = roster.find(r=>r.id===pid)
                  return (
                    <div key={slotId}
                      className={`slot ${!player ? 'empty' : ''}`}
                      draggable={!!player}
                      onDragStart={player ? (e)=>onDragStartFromSlot(e, slotId) : undefined}
                      onDragOver={(e)=>e.preventDefault()}
                      onDrop={(e)=>onDropToSlot(e, slotId)}
                      title={slotId}
                    >
                      {player ? (<div>{player.name} <div className="draggable-hint">{player.position}</div></div>) : (<div className="muted">{slotId}</div>)}
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
            {roster.map(p => (
              <li key={p.id} className="roster-item" draggable onDragStart={(e)=>onDragStartFromRoster(e,p.id)}>
                <div style={{flex:1}}>{p.name}</div>
                <div style={{color:'var(--muted)'}}>{p.position}</div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
