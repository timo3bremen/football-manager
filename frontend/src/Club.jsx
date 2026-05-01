import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'
import YouthAcademySection from './YouthAcademySection'

export default function Club(){
  const { team, jersey, setJersey, balance, transactions, addTransaction, gameDay } = useGame()
  const [clubSubTab, setClubSubTab] = useState('main') // 'main', 'finances', 'academy'
  const [showSponsorModal, setShowSponsorModal] = useState(false)
  const [sponsor, setSponsor] = useState(null)
  const [loading, setLoading] = useState(false)
  const [sponsorOptions, setSponsorOptions] = useState([])

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  // Load sponsor when team changes or on mount
  useEffect(() => {
    if (team && team.id) {
      loadSponsor()
      loadSponsorOptions()
    }
  }, [team && team.id])

  // Handle teamUpdated event to reload sponsor
  useEffect(() => {
    const handleTeamUpdate = () => {
      if (team && team.id) {
        loadSponsor()
        loadSponsorOptions()
      }
    }
    
    window.addEventListener('teamUpdated', handleTeamUpdate)
    return () => window.removeEventListener('teamUpdated', handleTeamUpdate)
  }, [team && team.id])

  const loadSponsor = () => {
    if (!team || !team.id) return
    
    setLoading(true)
    fetch(`${API_BASE}/api/sponsors/${team.id}`)
      .then(r => r.json())
      .then(data => {
        if (data && data.id) {
          setSponsor(data)
        } else {
          setSponsor(null)
        }
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden des Sponsors:', e)
        setLoading(false)
      })
  }

  const loadSponsorOptions = () => {
    if (!team || !team.id) return
    
    fetch(`${API_BASE}/api/sponsors/options/${team.id}`)
      .then(r => r.json())
      .then(data => {
        if (data && data.options) {
          setSponsorOptions(data.options)
          console.log('Sponsor-Optionen geladen für Division', data.division, 'mit Multiplikator', data.multiplier)
        }
      })
      .catch(e => {
        console.error('Fehler beim Laden der Sponsor-Optionen:', e)
        // Fallback auf Default-Optionen
        setSponsorOptions([
          { key:'s1', name:'SportCo', payouts: { appearance:40000, win:100000, survive:4000000, title:15000000 } },
          { key:'s2', name:'MegaCorp', payouts: { appearance:70000, win:20000, survive:7000000, title:12000000 } },
          { key:'s3', name:'LocalBank', payouts: { appearance:120000, win:100000, survive:1000000, title:5000000 } },
        ])
      })
  }

  function chooseSponsor(option){
    if (!team || !team.id) return
    
    setLoading(true)
    fetch(`${API_BASE}/api/sponsors`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        teamId: team.id,
        name: option.name,
        appearancePayout: option.payouts.appearance,
        winPayout: option.payouts.win,
        survivePayout: option.payouts.survive,
        titlePayout: option.payouts.title
      })
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          setSponsor(data.sponsor)
          setShowSponsorModal(false)
        } else {
          alert('Fehler: ' + data.message)
        }
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Hinzufügen des Sponsors:', e)
        alert('Fehler beim Hinzufügen des Sponsors')
        setLoading(false)
      })
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

    function getFinanceSummary(){
      const categories = {
        'attendance': '🎫 Zuschauereinnahmen',
        'sponsors': '🤝 Sponsoren',
        'competition': '🏆 Wettbewerbsprämien',
        'salaries': '👥 Spielergehälter',
        'infrastructure': '🏗️ Infrastruktur',
        'interest': '💸 Zinsen',
        'transfers': '🔄 Transfers'
      }

      const summary = {}

      // Initialize all categories
      Object.keys(categories).forEach(cat => {
        summary[cat] = { label: categories[cat], seasonAmount: 0, todayAmount: 0 }
      })

      // Aggregate transactions
      transactions.forEach(t => {
        const cat = t.category || 'other'
        if (summary[cat]) {
          summary[cat].seasonAmount += t.amount
          
          // Check if transaction is from today (gameDay)
          if (t.createdAt) {
            // Nutze gameDay als Indikator für den aktuellen In-Game-Tag
            // Alle Transaktionen, die nach dem letzten gameDay-Wechsel erstellt wurden
            const transDate = new Date(t.createdAt).getTime()
            const lastGameDayTime = localStorage.getItem('fm_lastGameDayTime') ? parseInt(localStorage.getItem('fm_lastGameDayTime')) : Date.now()
            
            if (transDate > lastGameDayTime) {
              summary[cat].todayAmount += t.amount
            }
          }
        }
      })

      return Object.values(summary)
    }

  return (
    <div>
      <h3>Verein</h3>

      {/* Sub-Tab Navigation */}
       <div className="menu" style={{marginBottom: 12}}>
         <button 
           className={clubSubTab === 'main' ? 'active' : ''} 
           onClick={() => setClubSubTab('main')}
         >
           🏛️ Verwaltung
         </button>
         <button 
           className={clubSubTab === 'finances' ? 'active' : ''} 
           onClick={() => setClubSubTab('finances')}
         >
           💰 Finanzen
         </button>
         <button 
           className={clubSubTab === 'academy' ? 'active' : ''} 
           onClick={() => setClubSubTab('academy')}
         >
           🏫 Jugenakademie
         </button>
       </div>

      {/* Main Tab */}
      {clubSubTab === 'main' && (
        <div>
           <div className="card">
             <h4>Sponsoren</h4>
             {loading ? (
               <p className="muted">Lädt...</p>
             ) : sponsor ? (
               <div>
                 <div style={{marginBottom:16, padding:16, background:'linear-gradient(135deg, rgba(100,150,255,0.15), rgba(100,150,255,0.05))', borderRadius:12, border:'1px solid rgba(100,150,255,0.3)'}}>
                   <div style={{fontSize:'1.2em', fontWeight:'bold', marginBottom:12, color:'#60a5fa'}}>
                     🤝 {sponsor.name}
                   </div>
                   
                   <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px', marginBottom:12}}>
                     <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                       <div className="muted" style={{fontSize:'0.85em', marginBottom:'4px'}}>🎮 Antritt pro Spiel</div>
                       <div style={{fontSize:'1.1em', fontWeight:'bold', color:'#4ade80'}}>€{sponsor.appearancePayout.toLocaleString()}</div>
                     </div>
                     <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                       <div className="muted" style={{fontSize:'0.85em', marginBottom:'4px'}}>⚽ Sieg</div>
                       <div style={{fontSize:'1.1em', fontWeight:'bold', color:'#fbbf24'}}>€{sponsor.winPayout.toLocaleString()}</div>
                     </div>
                     <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                       <div className="muted" style={{fontSize:'0.85em', marginBottom:'4px'}}>📊 Klassenerhalt (Platz 1-8)</div>
                       <div style={{fontSize:'1.1em', fontWeight:'bold', color:'#60a5fa'}}>€{sponsor.survivePayout.toLocaleString()}</div>
                     </div>
                     <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                       <div className="muted" style={{fontSize:'0.85em', marginBottom:'4px'}}>🥇 Titel (Platz 1-2)</div>
                       <div style={{fontSize:'1.1em', fontWeight:'bold', color:'#f472b6'}}>€{sponsor.titlePayout.toLocaleString()}</div>
                     </div>
                   </div>
                   
                   <p className="muted" style={{fontSize:'0.85em', marginBottom:0}}>
                     ℹ️ Der Sponsor zahlt automatisch bei Spielsimulation und Saison-Ende.
                   </p>
                 </div>
                 <p className="muted" style={{fontSize:'0.9em'}}>
                   ℹ️ Ein Verein kann nur 1 Sponsor haben. Der Sponsor wird am Saison-Ende automatisch entfernt.
                 </p>
               </div>
             ) : (
               <p className="muted">Kein Sponsor aktiv</p>
             )}
            <div style={{display:'flex',gap:8, marginTop:8}}>
              <button 
                className="btn primary" 
                onClick={()=>setShowSponsorModal(true)}
                disabled={sponsor !== null || loading}
                style={{
                  opacity: sponsor !== null ? 0.5 : 1,
                  cursor: sponsor !== null ? 'not-allowed' : 'pointer'
                }}
              >
                Sponsor hinzufügen
              </button>
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
        </div>
      )}

        {showSponsorModal && (
          <div className="modal-backdrop" onClick={()=>setShowSponsorModal(false)}>
            <div className="modal" onClick={e=>e.stopPropagation()} style={{maxWidth:'700px'}}>
              <h4 style={{marginBottom:8}}>🤝 Neuen Sponsor auswählen</h4>
              <p className="muted" style={{marginBottom:20, fontSize:'0.9em'}}>⚠️ Ein Verein kann nur 1 Sponsor haben. Der Sponsor wird am Saison-Ende automatisch entfernt.</p>
              <div style={{display:'grid',gridTemplateColumns:'1fr',gap:12}}>
                {sponsorOptions.map((opt, idx) => (
                  <div 
                    key={opt.key} 
                    className="card" 
                    style={{
                      background:`linear-gradient(135deg, ${idx === 0 ? 'rgba(59,130,246,0.1)' : idx === 1 ? 'rgba(168,85,247,0.1)' : 'rgba(34,197,94,0.1)'}, ${idx === 0 ? 'rgba(59,130,246,0.05)' : idx === 1 ? 'rgba(168,85,247,0.05)' : 'rgba(34,197,94,0.05)'})`,
                      border:`2px solid ${idx === 0 ? 'rgba(59,130,246,0.3)' : idx === 1 ? 'rgba(168,85,247,0.3)' : 'rgba(34,197,94,0.3)'}`,
                      padding:'16px',
                      borderRadius:'12px',
                      display:'flex',
                      justifyContent:'space-between',
                      alignItems:'stretch',
                      gap:'16px'
                    }}
                  >
                    <div style={{flex:1}}>
                      <div style={{fontSize:'1.15em', fontWeight:'bold', marginBottom:12, color: idx === 0 ? '#3b82f6' : idx === 1 ? '#a855f7' : '#22c55e'}}>
                        {idx === 0 ? '💼' : idx === 1 ? '🏢' : '🏦'} {opt.name}
                      </div>
                      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:'10px',marginBottom:12}}>
                        <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                          <div className="muted" style={{fontSize:'0.8em', marginBottom:'4px'}}>🎮 Antritt/Spiel</div>
                          <div style={{fontSize:'1em', fontWeight:'bold', color: idx === 0 ? '#3b82f6' : idx === 1 ? '#a855f7' : '#22c55e'}}>€{opt.payouts.appearance.toLocaleString()}</div>
                        </div>
                        <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                          <div className="muted" style={{fontSize:'0.8em', marginBottom:'4px'}}>⚽ Sieg</div>
                          <div style={{fontSize:'1em', fontWeight:'bold', color:'#fbbf24'}}>€{opt.payouts.win.toLocaleString()}</div>
                        </div>
                        <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                          <div className="muted" style={{fontSize:'0.8em', marginBottom:'4px'}}>📊 Klassenerhalt</div>
                          <div style={{fontSize:'1em', fontWeight:'bold', color:'#60a5fa'}}>€{opt.payouts.survive.toLocaleString()}</div>
                        </div>
                        <div style={{background:'rgba(255,255,255,0.05)', padding:'10px', borderRadius:'6px'}}>
                          <div className="muted" style={{fontSize:'0.8em', marginBottom:'4px'}}>🥇 Titel</div>
                          <div style={{fontSize:'1em', fontWeight:'bold', color:'#f472b6'}}>€{opt.payouts.title.toLocaleString()}</div>
                        </div>
                      </div>
                    </div>
                    <div style={{display:'flex',flexDirection:'column',justifyContent:'center',alignItems:'center',minWidth:'140px'}}>
                      <button 
                        className="btn primary" 
                        onClick={() => chooseSponsor(opt)}
                        disabled={loading}
                        style={{
                          width:'100%',
                          background: idx === 0 ? '#3b82f6' : idx === 1 ? '#a855f7' : '#22c55e',
                          opacity: loading ? 0.6 : 1,
                          cursor: loading ? 'not-allowed' : 'pointer'
                        }}
                      >
                        {loading ? '⏳ Lädt...' : '✅ Auswählen'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
              <div style={{marginTop:16}}>
                <button className="btn secondary" onClick={()=>setShowSponsorModal(false)}>Schließen</button>
              </div>
            </div>
          </div>
        )}

        {/* Finances Tab */}
        {clubSubTab === 'finances' && (
          <div>
            <div className="card">
              <h4>Kontostand</h4>
              <div style={{fontSize:'1.3em', fontWeight:'bold', color: balance >= 0 ? '#4ade80' : '#ef4444', marginBottom:16}}>
                €{balance.toLocaleString()}
              </div>

              <h4 style={{marginTop:20}}>Finanzübersicht</h4>
              <div style={{overflowX:'auto'}}>
                <table style={{ width:'100%', borderCollapse:'collapse' }}>
                  <thead>
                    <tr style={{borderBottom:'2px solid rgba(255,255,255,0.2)'}}>
                      <th style={{textAlign:'left', padding:'12px 8px', fontWeight:'bold'}}>Kategorie</th>
                      <th style={{textAlign:'right', padding:'12px 8px', fontWeight:'bold'}}>Saison</th>
                      <th style={{textAlign:'right', padding:'12px 8px', fontWeight:'bold'}}>Heute</th>
                    </tr>
                  </thead>
                  <tbody>
                    {getFinanceSummary().map((row, idx) => (
                      <tr key={idx} style={{borderBottom:'1px solid rgba(255,255,255,0.05)'}}>
                        <td style={{padding:'10px 8px'}}>{row.label}</td>
                        <td style={{textAlign:'right', padding:'10px 8px', color: row.seasonAmount >= 0 ? '#4ade80' : '#fda4af', fontWeight:'bold'}}>
                          €{row.seasonAmount.toLocaleString()}
                        </td>
                        <td style={{textAlign:'right', padding:'10px 8px', color: row.todayAmount >= 0 ? '#4ade80' : '#fda4af', fontWeight:'bold'}}>
                          €{row.todayAmount.toLocaleString()}
                        </td>
                      </tr>
                    ))}
                    <tr style={{borderTop:'2px solid rgba(255,255,255,0.2)', fontWeight:'bold', background:'rgba(255,255,255,0.05)'}}>
                      <td style={{padding:'12px 8px'}}>📊 Bilanz</td>
                      <td style={{textAlign:'right', padding:'12px 8px', color: getFinanceSummary().reduce((sum, r) => sum + r.seasonAmount, 0) >= 0 ? '#4ade80' : '#fda4af'}}>
                        €{getFinanceSummary().reduce((sum, r) => sum + r.seasonAmount, 0).toLocaleString()}
                      </td>
                      <td style={{textAlign:'right', padding:'12px 8px', color: getFinanceSummary().reduce((sum, r) => sum + r.todayAmount, 0) >= 0 ? '#4ade80' : '#fda4af'}}>
                        €{getFinanceSummary().reduce((sum, r) => sum + r.todayAmount, 0).toLocaleString()}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <h4 style={{marginTop:20}}>Transaktionen</h4>
              <div style={{maxHeight:'300px',overflow:'auto'}}>
                {transactions.length ? (
                  <div>
                    {transactions.map(t => (
                      <div key={t.id} style={{padding:12, borderBottom:'1px solid rgba(255,255,255,0.1)', display:'flex', justifyContent:'space-between', alignItems:'start'}}>
                        <div>
                          <strong>{t.description}</strong>
                          <div className="muted" style={{fontSize:'0.85em', marginTop:4}}>
                            {t.createdAt ? new Date(t.createdAt).toLocaleString('de-DE') : 'Unbekanntes Datum'}
                          </div>
                        </div>
                        <div style={{color: t.amount >= 0 ? '#4ade80' : '#fda4af', fontWeight:'bold', marginLeft:'12px'}}>
                          €{t.amount.toLocaleString()}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="muted">Keine Transaktionen</p>
                )}
              </div>
           </div>
         </div>
       )}

      {/* Academy Tab */}
      {clubSubTab === 'academy' && (
        <div className="card">
          <YouthAcademySection />
        </div>
      )}
    </div>
  )
}
