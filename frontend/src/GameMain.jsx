import React, { useState, useEffect } from 'react'
import TeamPage from './TeamPage'
import Infrastructure from './Infrastructure'
import Club from './Club'
import Schedule from './Schedule'
import Options from './Options'
import TransferMarket from './TransferMarket'
import History from './History'
import LiveMatchSimulation from './LiveMatchSimulation'
import Inbox from './Inbox'
import { useGame } from './GameContext'

export default function GameMain(){
  const [tab, setTab] = useState('team')
  const { team, token, unreadCount, setUnreadCount } = useGame()

  // Lade ungelesene Nachrichten-Anzahl beim Laden des Teams
  useEffect(() => {
    if (team?.id) {
      loadUnreadCount()
      // Lade regelmäßig neu (z.B. alle 30 Sekunden)
      const interval = setInterval(loadUnreadCount, 30000)
      return () => clearInterval(interval)
    }
  }, [team?.id])

  // Lade ungelesene Nachrichten sofort nach Spieltagswechsel
  useEffect(() => {
    const handleMatchdayChange = () => {
      loadUnreadCount()
    }
    
    window.addEventListener('matchdayChanged', handleMatchdayChange)
    return () => window.removeEventListener('matchdayChanged', handleMatchdayChange)
  }, [team?.id])

  const loadUnreadCount = async () => {
    try {
      const response = await fetch(`http://192.168.178.21:8080/api/v2/messages/${team.id}/unread/count`)
      if (response.ok) {
        const count = await response.json()
        setUnreadCount(count)
      }
    } catch (error) {
      console.error('Fehler beim Laden der ungelesenen Nachrichten-Anzahl:', error)
    }
  }

  if (!team) {
    return (
      <div className="app-container">
        <div className="card">
          <h2>Kein aktives Team</h2>
          <p>Erstelle zuerst ein Team unter <strong>Spiel Starten</strong>.</p>
        </div>
      </div>
    )
  }

  const menuBtn = (key, label) => (
    <button key={key} onClick={() => setTab(key)} className={tab===key? 'active':''}>{label}</button>
  )

  return (
    <div className="app-container">
      <div className="header">
        <div className="brand">
          <div className="logo">GM</div>
          <div>
            <h1 className="title">Spiel: {team.name}</h1>
            <div className="subtitle">Verwalte dein Team</div>
          </div>
        </div>
      </div>

       <div className="card">
         <div className="menu">
            <button className={tab==='team'? 'active':''} onClick={()=>setTab('team')}>👥 Team</button>
            <button className={tab==='inbox'? 'active':''} onClick={()=>setTab('inbox')}>
              📬 Postfach
              {unreadCount > 0 && <span style={{marginLeft: '6px', background: '#ef4444', color: 'white', borderRadius: '50%', width: '20px', height: '20px', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: '11px', fontWeight: 'bold'}}>{unreadCount}</span>}
            </button>
            <button className={tab==='infrastructure'? 'active':''} onClick={()=>setTab('infrastructure')}>🏗️ Infrastruktur</button>
            <button className={tab==='club'? 'active':''} onClick={()=>setTab('club')}>🏢 Verein</button>
            <button className={tab==='schedule'? 'active':''} onClick={()=>setTab('schedule')}>📅 Spielplan</button>
            <button className={tab==='live-simulation'? 'active':''} onClick={()=>setTab('live-simulation')}>🔴 Live</button>
            <button className={tab==='transfer-market'? 'active':''} onClick={()=>setTab('transfer-market')}>💱 Transfermarkt</button>
            <button className={tab==='history'? 'active':''} onClick={()=>setTab('history')}>📖 Geschichte</button>
            <button className={tab==='options'? 'active':''} onClick={()=>setTab('options')}>Optionen</button>
          </div>

         <div className="panel">
           {tab === 'team' && <TeamPage />}
           {tab === 'inbox' && <Inbox />}
           {tab === 'infrastructure' && <Infrastructure />}
           {tab === 'club' && <Club />}
           {tab === 'schedule' && <Schedule />}
           {tab === 'live-simulation' && <LiveMatchSimulation token={token} teamId={team?.id} />}
           {tab === 'transfer-market' && <TransferMarket />}
           {tab === 'history' && <History />}
           {tab === 'options' && <Options />}
         </div>
      </div>
    </div>
  )
}

