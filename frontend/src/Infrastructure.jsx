import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

const API_BASE = 'http://192.168.178.21:8080'

export default function Infrastructure(){
  const { getStadiumSummary, setStadiumPart, stadiumEntryPrice, setStadiumEntryPrice, fanFriendship, balance, addTransaction } = useGame()
  const { team } = useGame()
  const [infraSubTab, setInfraSubTab] = useState('stadium') // 'stadium', 'training', 'fanshop', 'gastro'
  const [showBuildModal, setShowBuildModal] = useState(false)
  const [buildSeats, setBuildSeats] = useState(1000)
  const [buildType, setBuildType] = useState('standing')
  const [buildInProgress, setBuildInProgress] = useState(null)
  const [timeRemaining, setTimeRemaining] = useState(0)

  const summary = getStadiumSummary()

  // Load build status from backend on mount
  useEffect(() => {
    if (!team?.id) return
    loadBuildStatus()
  }, [team?.id])

  // Load build status from API
  const loadBuildStatus = async () => {
    try {
      const authRaw = localStorage.getItem('fm_auth')
      const token = authRaw ? JSON.parse(authRaw).token : null
      const headers = token ? { 'X-Auth-Token': token } : {}

      const res = await fetch(`${API_BASE}/api/v2/stadium-build/team/${team.id}`, { headers })
      const data = await res.json()

      if (data.id) {
        // Build exists
        setBuildInProgress({
          id: data.id,
          totalSeats: data.totalSeats,
          seatType: data.seatType,
          endTime: new Date(data.endTime).getTime()
        })
      }
    } catch (err) {
      console.error('Error loading build status:', err)
    }
  }

  // Berechne Baudauer: 1.000 Plätze = 1 Tag (86.400 Sekunden)
  // 1 Platz = 86,4 Sekunden
  const calculateBuildDuration = (numSeats) => {
    return numSeats * 86.4 // in Sekunden
  }

  // Update countdown timer
  useEffect(() => {
    if (!buildInProgress) return

    const interval = setInterval(() => {
      const now = Date.now()
      const remaining = Math.max(0, buildInProgress.endTime - now)
      
      setTimeRemaining(remaining)

      if (remaining === 0) {
        // Bau fertig!
        const sections = Math.ceil(buildInProgress.totalSeats / 1000)
        for (let i = 0; i < sections; i++) {
          setStadiumPart(i, buildInProgress.seatType)
        }
        
        // Melde Fertigstellung zum Backend
        const completeBuild = async () => {
          try {
            const authRaw = localStorage.getItem('fm_auth')
            const token = authRaw ? JSON.parse(authRaw).token : null
            const headers = {
              'Content-Type': 'application/json',
              ...(token && { 'X-Auth-Token': token })
            }

            await fetch(`${API_BASE}/api/v2/stadium-build/${buildInProgress.id}/complete`, {
              method: 'PUT',
              headers
            })
          } catch (err) {
            console.error('Error completing build:', err)
          }
        }
        
        completeBuild()
        
        alert(`✅ Stadionausbau fertig! ${buildInProgress.totalSeats} ${buildInProgress.seatType === 'standing' ? 'Steh' : buildInProgress.seatType === 'seated' ? 'Sitz' : 'VIP'}-Plätze sind jetzt verfügbar.`)
        setBuildInProgress(null)
        setTimeRemaining(0)
      }
    }, 100)

    return () => clearInterval(interval)
  }, [buildInProgress])

  // Format time remaining
  const formatTime = (ms) => {
    const seconds = Math.ceil(ms / 1000)
    const days = Math.floor(seconds / 86400)
    const hours = Math.floor((seconds % 86400) / 3600)
    const mins = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60

    if (days > 0) return `${days}d ${hours}h ${mins}m`
    if (hours > 0) return `${hours}h ${mins}m ${secs}s`
    if (mins > 0) return `${mins}m ${secs}s`
    return `${secs}s`
  }

  // Bestimme Stadion-Größe basierend auf Kapazität
  const getStadiumSize = () => {
    const capacity = summary.total
    if (capacity < 10000) return 'small'
    if (capacity < 30000) return 'medium'
    if (capacity < 70000) return 'large'
    return 'huge'
  }

  // Stadion-Bild-Pfad
  const getStadiumImage = () => {
    const size = getStadiumSize()
    return `/stadiums/${size}_stadium.png`
  }

  // Preisberechnung für Stadionausbau
  // Stehplätze: Basis
  // Sitzplätze: 3x teurer
  // VIP: 9x teurer
  const calculateBuildCost = (numSeats, type) => {
    const planningCost = 50000
    let baseCost = 0

    // Basis-Kosten für Stehplätze
    if (numSeats <= 500) {
      baseCost = numSeats * 2000
    } else if (numSeats <= 2000) {
      baseCost = 500 * 2000 + (numSeats - 500) * 1500
    } else if (numSeats <= 5000) {
      baseCost = 500 * 2000 + 1500 * 1500 + (numSeats - 2000) * 1250
    } else {
      baseCost = 500 * 2000 + 1500 * 1500 + 3000 * 1250 + (numSeats - 5000) * 1000
    }

    // Multiplikator basierend auf Platztyp
    let multiplier = 1
    if (type === 'seated') multiplier = 3
    if (type === 'vip') multiplier = 9

    return planningCost + (baseCost * multiplier)
  }

  const totalCost = calculateBuildCost(buildSeats, buildType)
  const totalDuration = calculateBuildDuration(buildSeats)
  const canAfford = balance >= totalCost

  // ...existing code...

  // Stadion ausbauen
  const handleBuild = async () => {
    if (!canAfford) {
      alert('Du hast nicht genug Geld!')
      return
    }

    try {
      // Starte Bau im Backend
      const authRaw = localStorage.getItem('fm_auth')
      const token = authRaw ? JSON.parse(authRaw).token : null
      const headers = {
        'Content-Type': 'application/json',
        ...(token && { 'X-Auth-Token': token })
      }

      const res = await fetch(`${API_BASE}/api/v2/stadium-build/start`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          teamId: team.id,
          totalSeats: buildSeats,
          seatType: buildType,
          cost: totalCost,
          durationSeconds: totalDuration
        })
      })

      if (res.ok) {
        const data = await res.json()
        setBuildInProgress({
          id: data.id,
          totalSeats: data.totalSeats,
          seatType: data.seatType,
          endTime: new Date(data.endTime).getTime()
        })

        // Kosten abziehen und Transaktion speichern
        addTransaction({
          amount: -totalCost,
          type: 'expense',
          desc: `Stadionausbau: ${buildSeats} ${buildType === 'standing' ? 'Steh' : buildType === 'seated' ? 'Sitz' : 'VIP'}-Plätze`,
          category: 'infrastructure'
        })

        setShowBuildModal(false)
        setBuildSeats(1000)
        setBuildType('standing')
      } else {
        alert('Fehler beim Starten des Baus!')
      }
    } catch (err) {
      console.error('Error starting build:', err)
      alert('Fehler beim Starten des Baus!')
    }
  }

  return (
    <div>
      <h3>Infrastruktur</h3>
      
      {/* Sub-Tab Navigation */}
      <div className="menu" style={{marginBottom: 12}}>
        <button 
          className={infraSubTab === 'stadium' ? 'active' : ''} 
          onClick={() => setInfraSubTab('stadium')}
        >
          🏟️ Stadion
        </button>
        <button 
          className={infraSubTab === 'training' ? 'active' : ''} 
          onClick={() => setInfraSubTab('training')}
        >
          ⚽ Trainingsplatz
        </button>
        <button 
          className={infraSubTab === 'fanshop' ? 'active' : ''} 
          onClick={() => setInfraSubTab('fanshop')}
        >
          🛍️ FanShop
        </button>
        <button 
          className={infraSubTab === 'gastro' ? 'active' : ''} 
          onClick={() => setInfraSubTab('gastro')}
        >
          🍔 Gastronomie
        </button>
      </div>

       {/* Stadium Tab */}
       {infraSubTab === 'stadium' && (
         <div>
           {/* Stadion-Anzeige */}
           <div style={{
             display: 'flex',
             flexDirection: 'column',
             alignItems: 'center',
             gap: '16px',
             marginBottom: '24px'
           }}>
             {/* Großes Stadion-Bild */}
             <div 
               style={{
                 width: '100%',
                 maxWidth: '400px',
                 height: '250px',
                 borderRadius: '12px',
                 backgroundImage: `url('${getStadiumImage()}')`,
                 backgroundSize: 'cover',
                 backgroundPosition: 'center',
                 border: '2px solid rgba(255,255,255,0.1)',
                 boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
                 position: 'relative',
                 overflow: 'hidden'
               }}
             >
               {/* Bau-Fortschritt Overlay */}
               {buildInProgress && (
                 <div style={{
                   position: 'absolute',
                   inset: 0,
                   background: 'rgba(0,0,0,0.6)',
                   display: 'flex',
                   flexDirection: 'column',
                   alignItems: 'center',
                   justifyContent: 'center',
                   color: '#fff',
                   textAlign: 'center'
                 }}>
                   <div style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '8px' }}>🏗️ Im Bau</div>
                   <div style={{ fontSize: '16px', marginBottom: '8px' }}>
                     {buildInProgress.totalSeats} {buildInProgress.seatType === 'standing' ? 'Steh' : buildInProgress.seatType === 'seated' ? 'Sitz' : 'VIP'}-Plätze
                   </div>
                   <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#4CAF50' }}>
                     {formatTime(timeRemaining)}
                   </div>
                 </div>
               )}
             </div>
             
             {/* Stadion-Info unter dem Bild */}
             <div style={{
               textAlign: 'center',
               width: '100%'
             }}>
               <div style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '8px' }}>
                 {summary.total.toLocaleString()} Plätze
               </div>
               <div style={{ fontSize: '14px', color: 'var(--muted)', marginBottom: '12px' }}>
                 {summary.seats.standing.toLocaleString()} Steh · {summary.seats.seated.toLocaleString()} Sitz · {summary.seats.vip.toLocaleString()} VIP
               </div>
               
               {/* Eintrittspreise */}
               <div style={{
                 background: 'rgba(255,255,255,0.05)',
                 padding: '12px',
                 borderRadius: '8px',
                 marginBottom: '12px'
               }}>
                 <div style={{ fontSize: '12px', color: 'var(--muted)', marginBottom: '8px' }}>Eintrittspreise (€)</div>
                 <div style={{
                   display: 'flex',
                   gap: '8px',
                   justifyContent: 'center',
                   flexWrap: 'wrap'
                 }}>
                   <input 
                     className="input" 
                     type="number" 
                     value={stadiumEntryPrice.standing} 
                     onChange={e=>setStadiumEntryPrice({...stadiumEntryPrice, standing: Number(e.target.value)})} 
                     style={{width: '70px', fontSize: '12px'}}
                     placeholder="Steh"
                   />
                   <input 
                     className="input" 
                     type="number" 
                     value={stadiumEntryPrice.seated} 
                     onChange={e=>setStadiumEntryPrice({...stadiumEntryPrice, seated: Number(e.target.value)})} 
                     style={{width: '70px', fontSize: '12px'}}
                     placeholder="Sitz"
                   />
                   <input 
                     className="input" 
                     type="number" 
                     value={stadiumEntryPrice.vip} 
                     onChange={e=>setStadiumEntryPrice({...stadiumEntryPrice, vip: Number(e.target.value)})} 
                     style={{width: '70px', fontSize: '12px'}}
                     placeholder="VIP"
                   />
                 </div>
               </div>
               
               {/* Fan-Freundschaft */}
               <div style={{ fontSize: '12px', color: 'var(--muted)', marginBottom: '12px' }}>
                 Fan-Freundschaft: {fanFriendship}%
               </div>

               {/* Ausbauen Button */}
               <button 
                 className="btn primary" 
                 onClick={() => setShowBuildModal(true)}
                 disabled={buildInProgress !== null}
                 style={{ width: '100%', opacity: buildInProgress ? 0.5 : 1 }}
               >
                 🏗️ Stadion ausbauen
               </button>
             </div>
           </div>
         </div>
       )}

      {/* Build Modal */}
      {showBuildModal && (
        <div className="modal-backdrop" onClick={() => setShowBuildModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h4>Stadion ausbauen</h4>
            
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '12px', color: 'var(--muted)' }}>
                Anzahl der neuen Plätze
              </label>
              <input 
                className="input" 
                type="number" 
                value={buildSeats}
                onChange={e => setBuildSeats(Math.max(1, Number(e.target.value)))}
                min="1"
                step="100"
                style={{ width: '100%' }}
              />
            </div>

            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', marginBottom: '6px', fontSize: '12px', color: 'var(--muted)' }}>
                Platztyp
              </label>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button 
                  onClick={() => setBuildType('standing')}
                  style={{
                    flex: 1,
                    padding: '8px',
                    background: buildType === 'standing' ? '#4CAF50' : 'rgba(255,255,255,0.1)',
                    border: buildType === 'standing' ? 'none' : '1px solid rgba(255,255,255,0.2)',
                    borderRadius: '6px',
                    color: '#fff',
                    cursor: 'pointer',
                    fontSize: '12px'
                  }}
                >
                  Stehplatz
                </button>
                <button 
                  onClick={() => setBuildType('seated')}
                  style={{
                    flex: 1,
                    padding: '8px',
                    background: buildType === 'seated' ? '#2196F3' : 'rgba(255,255,255,0.1)',
                    border: buildType === 'seated' ? 'none' : '1px solid rgba(255,255,255,0.2)',
                    borderRadius: '6px',
                    color: '#fff',
                    cursor: 'pointer',
                    fontSize: '12px'
                  }}
                >
                  Sitzplatz
                </button>
                <button 
                  onClick={() => setBuildType('vip')}
                  style={{
                    flex: 1,
                    padding: '8px',
                    background: buildType === 'vip' ? '#FFB800' : 'rgba(255,255,255,0.1)',
                    border: buildType === 'vip' ? 'none' : '1px solid rgba(255,255,255,0.2)',
                    borderRadius: '6px',
                    color: buildType === 'vip' ? '#000' : '#fff',
                    cursor: 'pointer',
                    fontSize: '12px'
                  }}
                >
                  VIP
                </button>
              </div>
            </div>

            {/* Baudauer anzeigen */}
            <div style={{
              background: 'rgba(255,255,255,0.05)',
              padding: '12px',
              borderRadius: '8px',
              marginBottom: '16px',
              fontSize: '12px'
            }}>
              <div style={{ marginBottom: '8px' }}>
                <strong>⏱️ Baudauer:</strong>
              </div>
              <div style={{ color: '#4CAF50', fontSize: '14px', fontWeight: 'bold' }}>
                {formatTime(totalDuration * 1000)}
              </div>
              <div style={{ color: 'var(--muted)', fontSize: '11px', marginTop: '4px' }}>
                (1.000 Plätze = 1 Tag)
              </div>
            </div>

            {/* Preisberechnung anzeigen */}
            <div style={{
              background: 'rgba(255,255,255,0.05)',
              padding: '12px',
              borderRadius: '8px',
              marginBottom: '16px',
              fontSize: '12px'
            }}>
              <div style={{ marginBottom: '8px' }}>
                <strong>Kostenberechnung:</strong>
              </div>
              <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                Planungskosten: €50.000
              </div>
              {buildSeats <= 500 && (
                <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                  {buildSeats} × €2.000 = €{(buildSeats * 2000).toLocaleString()}
                </div>
              )}
              {buildSeats > 500 && buildSeats <= 2000 && (
                <>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    500 × €2.000 = €1.000.000
                  </div>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    {buildSeats - 500} × €1.500 = €{((buildSeats - 500) * 1500).toLocaleString()}
                  </div>
                </>
              )}
              {buildSeats > 2000 && buildSeats <= 5000 && (
                <>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    500 × €2.000 = €1.000.000
                  </div>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    1.500 × €1.500 = €2.250.000
                  </div>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    {buildSeats - 2000} × €1.250 = €{((buildSeats - 2000) * 1250).toLocaleString()}
                  </div>
                </>
              )}
              {buildSeats > 5000 && (
                <>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    500 × €2.000 = €1.000.000
                  </div>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    1.500 × €1.500 = €2.250.000
                  </div>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    3.000 × €1.250 = €3.750.000
                  </div>
                  <div style={{ color: 'var(--muted)', marginBottom: '4px' }}>
                    {buildSeats - 5000} × €1.000 = €{((buildSeats - 5000) * 1000).toLocaleString()}
                  </div>
                </>
              )}
              <div style={{ 
                borderTop: '1px solid rgba(255,255,255,0.1)',
                paddingTop: '8px',
                marginTop: '8px',
                fontSize: '14px',
                fontWeight: 'bold'
              }}>
                Gesamtkosten: €{totalCost.toLocaleString()}
              </div>
            </div>

            {/* Budget Check */}
            <div style={{
              marginBottom: '16px',
              padding: '8px',
              borderRadius: '6px',
              background: canAfford ? 'rgba(76,175,80,0.1)' : 'rgba(244,67,54,0.1)',
              color: canAfford ? '#4CAF50' : '#F44336',
              fontSize: '12px'
            }}>
              Budget: €{balance.toLocaleString()} {canAfford ? '✓' : '✗'}
            </div>

            {/* Buttons */}
            <div style={{ display: 'flex', gap: '8px' }}>
              <button 
                className="btn primary"
                onClick={handleBuild}
                disabled={!canAfford}
                style={{
                  flex: 1,
                  opacity: canAfford ? 1 : 0.5,
                  cursor: canAfford ? 'pointer' : 'not-allowed'
                }}
              >
                🏗️ Bauen
              </button>
              <button 
                className="btn secondary"
                onClick={() => setShowBuildModal(false)}
                style={{ flex: 1 }}
              >
                Abbrechen
              </button>
            </div>
           </div>
         </div>
       )}

      {/* Training Tab */}
      {infraSubTab === 'training' && (
        <div className="card">
          <h4>⚽ Trainingsplatz</h4>
          <p className="muted">Derzeit auf Stufe 0 - Kein Ausbau verfügbar</p>
          <button className="btn primary" disabled>
            🔧 Trainingsplatz ausbauen (Bald verfügbar)
          </button>
        </div>
      )}

      {/* FanShop Tab */}
      {infraSubTab === 'fanshop' && (
        <div className="card">
          <h4>🛍️ FanShop</h4>
          <p className="muted">Derzeit auf Stufe 0 - Kein Ausbau verfügbar</p>
          <button className="btn primary" disabled>
            🔧 FanShop ausbauen (Bald verfügbar)
          </button>
        </div>
      )}

      {/* Gastro Tab */}
      {infraSubTab === 'gastro' && (
        <div className="card">
          <h4>🍔 Gastronomie</h4>
          <p className="muted">Derzeit auf Stufe 0 - Kein Ausbau verfügbar</p>
          <button className="btn primary" disabled>
            🔧 Gastronomie ausbauen (Bald verfügbar)
          </button>
        </div>
      )}
    </div>
  )
}