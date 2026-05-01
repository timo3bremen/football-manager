import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function Clock(){
  const [time, setTime] = useState(new Date())
  const { currentMatchday, setCurrentMatchday, season, setSeason } = useGame()
  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  useEffect(() => {
    const timer = setInterval(() => {
      setTime(new Date())
    }, 1000)
    
    return () => clearInterval(timer)
  }, [])

  // Lade aktuellen Spieltag und Saison bei Mount und aktualisiere periodisch
  useEffect(() => {
    const loadMatchday = () => {
      fetch(`${API_BASE}/api/v2/schedule/current-matchday`)
        .then(r => r.json())
        .then(data => {
          setCurrentMatchday(data.currentMatchday || 1)
          setSeason(data.currentSeason || 1)
        })
        .catch(e => console.error('Fehler beim Laden des Spieltags:', e))
    }

    loadMatchday()
    
    // Aktualisiere den Spieltag jede Minute (um Änderungen um 18:00 zu erfassen)
    const timer = setInterval(loadMatchday, 60000)
    
    return () => clearInterval(timer)
  }, [setCurrentMatchday, setSeason])

  const formatDate = (date) => {
    const options = { weekday: 'short', year: 'numeric', month: '2-digit', day: '2-digit' }
    return date.toLocaleDateString('de-DE', options)
  }

  const formatTime = (date) => {
    return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }

  return (
    <div className="clock">
      <div className="clock-time">{formatTime(time)}</div>
      <div className="clock-date">{formatDate(time)}</div>
      <div style={{ fontSize: '0.85em', marginTop: 4, opacity: 0.8 }}>
        Spieltag {currentMatchday}/22 | Saison {season}
      </div>
    </div>
  )
}
