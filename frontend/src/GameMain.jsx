import React, { useState } from 'react'
import TeamPage from './TeamPage'
import Infrastructure from './Infrastructure'
import Club from './Club'
import Schedule from './Schedule'
import Options from './Options'
import TransferMarket from './TransferMarket'
import { useGame } from './GameContext'

export default function GameMain(){
  const [tab, setTab] = useState('team')
  const { team } = useGame()

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
          <button className={tab==='team'? 'active':''} onClick={()=>setTab('team')}>Team</button>
          <button className={tab==='infrastructure'? 'active':''} onClick={()=>setTab('infrastructure')}>Infrastruktur</button>
          <button className={tab==='club'? 'active':''} onClick={()=>setTab('club')}>Verein</button>
          <button className={tab==='schedule'? 'active':''} onClick={()=>setTab('schedule')}>Spielplan</button>
          <button className={tab==='transfer-market'? 'active':''} onClick={()=>setTab('transfer-market')}>Transfermarkt</button>
          <button className={tab==='options'? 'active':''} onClick={()=>setTab('options')}>Optionen</button>
        </div>

        <div className="panel">
          {tab === 'team' && <TeamPage />}
          {tab === 'infrastructure' && <Infrastructure />}
          {tab === 'club' && <Club />}
          {tab === 'schedule' && <Schedule />}
          {tab === 'transfer-market' && <TransferMarket />}
          {tab === 'options' && <Options />}
        </div>
      </div>
    </div>
  )
}
