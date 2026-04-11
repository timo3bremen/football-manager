import React, { useState } from 'react'
import { useGame } from './GameContext'

export default function Infrastructure(){
  const { stadiumParts, getStadiumCapacity, setStadiumPart, removeStadiumPart, getStadiumSummary, generateOffersForPart, acceptOffer, rejectOffer, negotiateOffer, setPendingType, ensureOffersFresh, stadiumEntryPrice, setStadiumEntryPrice, fanFriendship } = useGame()
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState(null)
  const [negotiation, setNegotiation] = useState(null)

  const summary = getStadiumSummary()

  function onCellClick(idx){
    // ensure offers are fresh for this slot (will generate if missing or stale)
    ensureOffersFresh(idx)
    setSelected(idx)
  }

  function setType(type){
    if (selected == null) return
    setStadiumPart(selected, type)
    setSelected(null)
  }

  function remove(){
    if (selected == null) return
    removeStadiumPart(selected)
    setSelected(null)
  }

  return (
    <div>
      <h3>Infrastruktur</h3>
      <div style={{display:'flex',gap:12,alignItems:'center'}}>
        <div className="stadium-thumb card" onClick={()=>setOpen(true)}>
          <div>
            Stadion
            <br />
            <small>{summary.total} Plätze — {summary.seats.standing} Steh, {summary.seats.seated} Sitz, {summary.seats.vip} VIP</small>
          </div>
        </div>
        <div>
          <p className="muted">Klicke das Stadion, um Details anzuzeigen und Teile auszubauen.</p>
          <div style={{marginTop:8}}>
            <div className="muted">Eintrittspreise (€): Steh {stadiumEntryPrice.standing} / Sitz {stadiumEntryPrice.seated} / VIP {stadiumEntryPrice.vip}</div>
            <div className="muted">Fan-Freundschaft: {fanFriendship}%</div>
            <div style={{marginTop:6, display:'flex', gap:8}}>
              <input className="input" type="number" value={stadiumEntryPrice.standing} onChange={e=>setStadiumEntryPrice({...stadiumEntryPrice, standing: Number(e.target.value)})} style={{width:100}} />
              <input className="input" type="number" value={stadiumEntryPrice.seated} onChange={e=>setStadiumEntryPrice({...stadiumEntryPrice, seated: Number(e.target.value)})} style={{width:100}} />
              <input className="input" type="number" value={stadiumEntryPrice.vip} onChange={e=>setStadiumEntryPrice({...stadiumEntryPrice, vip: Number(e.target.value)})} style={{width:100}} />
            </div>
          </div>
        </div>
      </div>

      {open && (
        <div className="modal-backdrop" onClick={()=>{setOpen(false); setSelected(null)}}>
          <div className="modal" onClick={e=>e.stopPropagation()}>
            <h4>Stadion ausbauen</h4>
            <p className="muted">Jeder Bereich fügt 1.000 Plätze hinzu. Wähle Typ: Steh / Sitz / VIP.</p>
            <div style={{display:'flex',gap:16}}>
                <div style={{flex:1}}>
                  <div className="stadium-grid">
                    {(() => {
                      // create 3 rows to resemble stadium interior: 10 / 10 / 10
                      const rows = [10,10,10]
                      let idx = 0
                      return rows.map((count,rowIndex) => (
                        <div key={rowIndex} className="stadium-row">
                          {Array.from({length:count}).map((_,j) => {
                            const i = idx++
                            const p = (stadiumParts || [])[i]
                            const cls = p && p.type ? p.type : ''
                            const sel = selected === i ? ' selected' : ''
                            return (
                              <div key={i} className={`stadium-cell ${cls}${sel}`} onClick={()=>onCellClick(i)}>
                                {p && p.built ? p.type : `#${i+1}`}
                              </div>
                            )
                          })}
                        </div>
                      ))
                    })()}
                  </div>
                </div>

              <div style={{width:320}}>
                <h5>Angebote</h5>
                {selected == null ? <p className="muted">Wähle ein Feld im Stadion aus, um Angebote zu sehen.</p> : null}
                {selected != null && (()=>{
                  const item = (stadiumParts||[])[selected]
                  if (!item) return <p className="muted">Keine Daten</p>
                  // if not built and no pendingType, ask for type first
                  if (!item.built && !item.pendingType){
                    return (
                      <div>
                        <p className="muted">Wähle zuerst den gewünschten Bereichstyp, dann erscheinen Angebote.</p>
                        <div style={{display:'flex',gap:8}}>
                          <button className="btn primary" onClick={() => { setPendingType(selected,'standing'); generateOffersForPart(selected); }}>Steh</button>
                          <button className="btn secondary" onClick={() => { setPendingType(selected,'seated'); generateOffersForPart(selected); }}>Sitz</button>
                          <button className="btn" onClick={() => { setPendingType(selected,'vip'); generateOffersForPart(selected); }} style={{background:'#b78b1a', color:'#021'}}>VIP</button>
                        </div>
                      </div>
                    )
                  }

                  const offers = (item.offers || []).slice()
                  // filter out rejected (rejectOffer removes them)
                  return (
                    <div>
                            {offers.length === 0 ? <p className="muted">Keine Angebote verfügbar.</p> : (
                                    <ul style={{listStyle:'none',padding:0}}>
                                      {offers.map(o=> (
                                        <li key={o.id} style={{marginBottom:8, padding:8, borderRadius:8, background:'rgba(255,255,255,0.01)'}}>
                                          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                                            <div>
                                              <div style={{fontWeight:700}}>{o.proposer}</div>
                                              <div className="muted">Preis: €{o.price.toLocaleString()}</div>
                                              {o.counter ? <div className="muted">Gegenangebot: €{o.counter.price.toLocaleString()} — {o.counter.accepted? 'angenommen' : 'offen'}</div> : null}
                                            </div>
                                            <div style={{display:'flex',flexDirection:'column',gap:6}}>
                                              <button className="btn primary" onClick={() => acceptOffer(selected,o.id)}>Akzeptieren</button>
                                              <button className="btn secondary" onClick={() => setNegotiation({offerId: o.id, input: Math.round(o.price * 0.85)})}>Verhandeln</button>
                                              <button className="btn ghost" onClick={() => rejectOffer(selected,o.id)}>Ablehnen</button>
                                            </div>
                                          </div>
                                        </li>
                                      ))}
                                    </ul>
                                  )}

                            {item.acceptedOffer ? (
                        <div style={{marginTop:12}}>
                          <h5>Akzeptiertes Angebot: €{item.acceptedOffer.price.toLocaleString()}</h5>
                          <p className="muted">Der Bereich wird nun als <strong>{item.type || item.pendingType}</strong> gebaut.</p>
                        </div>
                      ) : null}
                            {/* negotiation input */}
                            { negotiation ? (
                              <div style={{marginTop:12}}>
                                <h5>Verhandeln - Angebot bearbeiten</h5>
                                <div style={{display:'flex',gap:8,alignItems:'center'}}>
                                  <input className="input" type="number" value={negotiation.input} onChange={e=>setNegotiation({...negotiation, input: Number(e.target.value)})} />
                                  <button className="btn primary" onClick={()=>{ negotiateOffer(selected, negotiation.offerId, negotiation.input); setNegotiation(null); }}>Absenden</button>
                                  <button className="btn ghost" onClick={()=>setNegotiation(null)}>Abbrechen</button>
                                </div>
                                <div className="muted" style={{marginTop:8}}>Gültige Verhandlungsspanne: 5–15% unter dem ursprünglichen Preis (Akzeptanz zufällig).</div>
                              </div>
                            ) : null}
                    </div>
                  )
                })()}

                <div style={{marginTop:12}}>
                  <button className="btn ghost" onClick={remove}>Entfernen</button>
                  <button className="btn secondary" style={{marginLeft:8}} onClick={()=>{setOpen(false); setSelected(null)}}>Schließen</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
