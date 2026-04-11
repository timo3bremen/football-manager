import React from 'react'
import { useGame } from './GameContext'

export default function Schedule(){
  const { team } = useGame()

  // placeholder competitions
  const competitions = [
    {id:1, name: 'Liga A'},
    {id:2, name: 'Pokal B'}
  ]

  return (
    <div>
      <h3>Spielplan & Tabellen</h3>
      <div className="card">
        <p>Team: <strong>{team ? team.name : '-'}</strong></p>
        <h4>Wettbewerbe</h4>
        <ul>
          {competitions.map(c => <li key={c.id}>{c.name} — Spielplan & Tabelle folgen</li>)}
        </ul>
      </div>
    </div>
  )
}
