import React, { useState } from 'react'
import { useGame } from './GameContext'

export default function Club(){
  const { sponsors, addSponsorObject, jersey, setJersey, balance, transactions, addTransaction } = useGame()
  const [showSponsorModal, setShowSponsorModal] = useState(false)
  const [showFinances, setShowFinances] = useState(false)

  const sponsorOptions = [
    { key:'s1', name:'SportCo', payouts: { appearance:200, win:500, survive:5000, title:20000 }, extra: { target:'Viertelfinale Pokal', payout:3000 } },
    { key:'s2', name:'MegaCorp', payouts: { appearance:500, win:1200, survive:12000, title:60000 }, extra: { target:'Halbfinale Pokal', payout:8000 } },
    { key:'s3', name:'LocalBank', payouts: { appearance:100, win:300, survive:2000, title:10000 }, extra: { target:'Viertelfinale Pokal', payout:2000 } },
  ]

  function chooseSponsor(option){
    const sponsor = { id: `${option.key}-${Date.now()}`, name: option.name, payouts: option.payouts, extra: option.extra }
    addSponsorObject(sponsor)
    setShowSponsorModal(false)
    // optional: add an initial signing fee? skipped for now
  }

  function simulateSponsorPayout(sponsor, kind){
    const amount = sponsor.payouts[kind]
    if (!amount) return
    addTransaction({ amount, type:'income', desc: `${sponsor.name} - ${kind}` })
  }

  function simulateExtraPayout(sponsor){
    if (!sponsor.extra) return
    addTransaction({ amount: sponsor.extra.payout, type:'income', desc: `${sponsor.name} - ${sponsor.extra.target}` })
  }

  return (
    <div>
      <h3>Verein</h3>

      <div className="card">
        <h4>Sponsoren</h4>
        <ul>
          {sponsors.length ? sponsors.map((s,i)=> (
            <li key={s.id || i} style={{marginBottom:8}}>
              <strong>{s.name}</strong>
              <div className="muted">Antritt: €{s.payouts.appearance} · Sieg: €{s.payouts.win} · Klassenerhalt: €{s.payouts.survive} · Titel: €{s.payouts.title}</div>
              {s.extra ? <div className="muted">Extra: {s.extra.target} (€{s.extra.payout})</div> : null}
            </li>
          )) : <li className="muted">Keine Sponsoren</li>}
        </ul>
        <div style={{display:'flex',gap:8, marginTop:8}}>
          <button className="btn primary" onClick={()=>setShowSponsorModal(true)}>Sponsor hinzufügen</button>
          <button className="btn secondary" onClick={()=>setShowFinances(true)}>Finanzen</button>
        </div>
      </div>

      <div className="card" style={{marginTop:12}}>
        <h4>Trikot</h4>
        <p>Farbe: <strong>{jersey.color}</strong></p>
        <div style={{display:'flex',gap:8}}>
          <button className="btn secondary" onClick={() => setJersey({color: 'rot'})}>Auf Rot setzen</button>
          <button className="btn secondary" onClick={() => setJersey({color: 'blau'})}>Auf Blau setzen</button>
        </div>
      </div>

      {showSponsorModal && (
        <div className="modal-backdrop" onClick={()=>setShowSponsorModal(false)}>
          <div className="modal" onClick={e=>e.stopPropagation()}>
            <h4>Neuen Sponsor auswählen</h4>
            <div style={{display:'flex',flexDirection:'column',gap:8}}>
              {sponsorOptions.map(opt => (
                <div key={opt.key} className="card" style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                  <div>
                    <strong>{opt.name}</strong>
                    <div className="muted">Antritt: €{opt.payouts.appearance} · Sieg: €{opt.payouts.win} · Klassenerhalt: €{opt.payouts.survive} · Titel: €{opt.payouts.title}</div>
                    <div className="muted">Extra: €{opt.extra.target} (€{opt.extra.payout})</div>
                  </div>
                  <div>
                    <button className="btn primary" onClick={() => chooseSponsor(opt)}>Auswählen</button>
                  </div>
                </div>
              ))}
            </div>
            <div style={{marginTop:12}}>
              <button className="btn secondary" onClick={()=>setShowSponsorModal(false)}>Schließen</button>
            </div>
          </div>
        </div>
      )}

      {showFinances && (
        <div className="modal-backdrop" onClick={()=>setShowFinances(false)}>
          <div className="modal" onClick={e=>e.stopPropagation()}>
            <h4>Finanzen</h4>
            <p>Kontostand: <strong>€{balance.toLocaleString()}</strong></p>
            <h5>Mögliche Einnahmen von Sponsoren</h5>
            <ul style={{listStyle:'none',padding:0}}>
              {sponsors.length ? sponsors.map(s => (
                <li key={s.id} style={{marginBottom:8,padding:8,borderRadius:8,background:'rgba(255,255,255,0.01)'}}>
                  <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                    <div>
                      <strong>{s.name}</strong>
                      <div className="muted">Antritt: €{s.payouts.appearance} · Sieg: €{s.payouts.win} · Klassenerhalt: €{s.payouts.survive} · Titel: €{s.payouts.title}</div>
                      {s.extra ? <div className="muted">Extra: {s.extra.target} (€{s.extra.payout})</div> : null}
                    </div>
                    <div style={{display:'flex',flexDirection:'column',gap:6}}>
                      <button className="btn" onClick={() => simulateSponsorPayout(s,'appearance')}>Zahle Antritt</button>
                      <button className="btn" onClick={() => simulateSponsorPayout(s,'win')}>Zahle Sieg</button>
                      <button className="btn" onClick={() => simulateSponsorPayout(s,'survive')}>Zahle Klassenerhalt</button>
                      <button className="btn" onClick={() => simulateSponsorPayout(s,'title')}>Zahle Titel</button>
                      {s.extra ? <button className="btn primary" onClick={() => simulateExtraPayout(s)}>Zahle Extra</button> : null}
                    </div>
                  </div>
                </li>
              )) : <li className="muted">Keine Sponsoren</li>}
            </ul>

            <h5 style={{marginTop:12}}>Transaktionen</h5>
            <ul style={{listStyle:'none',padding:0,maxHeight:200,overflow:'auto'}}>
              {transactions.length ? transactions.map(t => (
                <li key={t.id} style={{padding:6,borderBottom:'1px solid rgba(255,255,255,0.02)'}}>
                  <div style={{display:'flex',justifyContent:'space-between'}}>
                    <div><strong>{t.desc}</strong><div className="muted">{new Date(t.date).toLocaleString()}</div></div>
                    <div style={{color: t.amount>=0? '#9ae6b4':'#fda4af'}}>€{t.amount.toLocaleString()}</div>
                  </div>
                </li>
              )) : <li className="muted">Keine Transaktionen</li>}
            </ul>

            <div style={{marginTop:12}}>
              <button className="btn secondary" onClick={()=>setShowFinances(false)}>Schließen</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
