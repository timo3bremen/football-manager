import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Home from './Home'
import TeamCreate from './TeamCreate'
import Settings from './Settings'
import StartGame from './StartGame'
import GameMain from './GameMain'
import { GameProvider } from './GameContext'
import Auth from './Auth'
import Admin from './Admin'
import Clock from './Clock'

export default function App(){
  return (
    <GameProvider>
      <BrowserRouter>
        <div style={{position: 'relative'}}>
          <div style={{position: 'fixed', top: 12, right: 12, zIndex: 1000}}>
            <Clock />
          </div>
          <Routes>
            <Route path="/" element={<Home/>} />
            <Route path="/team-create" element={<TeamCreate/>} />
            <Route path="/settings" element={<Settings/>} />
            <Route path="/start-game" element={<StartGame/>} />
            <Route path="/game" element={<GameMain/>} />
            <Route path="/join-team" element={<div style={{padding:20}}><h2>Team Beitreten</h2><p>Feature folgt.</p></div>} />
            <Route path="/auth" element={<Auth/>} />
            <Route path="/admin" element={<Admin/>} />
          </Routes>
        </div>
      </BrowserRouter>
    </GameProvider>
  )
}
