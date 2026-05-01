import React, { useState, useEffect } from 'react'
import { API_BASE } from './api.config'

export default function PlayerDetailsModal({ playerId, onClose }) {
  const [playerStats, setPlayerStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (playerId) {
      loadPlayerStats()
    }
  }, [playerId])

  const loadPlayerStats = async () => {
    try {
      setLoading(true)
      const response = await fetch(`${API_BASE}/api/v2/players/${playerId}/stats`)
      if (response.ok) {
        const data = await response.json()
        setPlayerStats(data)
      } else {
        console.error('Failed to load player stats')
      }
    } catch (error) {
      console.error('Error loading player stats:', error)
    } finally {
      setLoading(false)
    }
  }

  if (!playerId) return null

  const formatCurrency = (value) => {
    if (value >= 1000000) {
      return `${(value / 1000000).toFixed(2)}M €`
    }
    if (value >= 1000) {
      return `${(value / 1000).toFixed(0)}K €`
    }
    return `${value} €`
  }

  const formatRating = (rating) => {
    if (!rating) return '-'
    return rating.toFixed(1)
  }

  const getResultColor = (result) => {
    if (result === 'W') return '#10b981' // green
    if (result === 'L') return '#ef4444' // red
    return '#f59e0b' // orange for draw
  }

  const getResultText = (result) => {
    if (result === 'W') return 'S' // Sieg
    if (result === 'L') return 'N' // Niederlage
    return 'U' // Unentschieden
  }

  return (
    <div className="modal-overlay" onClick={onClose} style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.7)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{
        backgroundColor: 'var(--card-bg)',
        borderRadius: 8,
        padding: 24,
        maxWidth: 800,
        width: '90%',
        maxHeight: '90vh',
        overflowY: 'auto',
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.5)'
      }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <div className="muted">Lade Spielerstatistiken...</div>
          </div>
        ) : playerStats ? (
          <>
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
              <div>
                <h2 style={{ margin: 0, marginBottom: 8 }}>{playerStats.name}</h2>
                <div style={{ fontSize: '0.9em', color: 'var(--muted)' }}>
                  {playerStats.position} • {playerStats.age} Jahre • {playerStats.country}
                </div>
              </div>
              <button onClick={onClose} style={{
                background: 'transparent',
                border: 'none',
                fontSize: '1.5em',
                cursor: 'pointer',
                color: 'var(--muted)',
                padding: 0,
                width: 32,
                height: 32
              }}>
                ✕
              </button>
            </div>

            {/* Grundlegende Informationen */}
            <div className="card" style={{ marginBottom: 16, padding: 16 }}>
              <h4 style={{ marginTop: 0, marginBottom: 12 }}>📊 Grunddaten</h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 16 }}>
                <div>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Rating</div>
                  <div style={{ fontSize: '1.3em', fontWeight: 'bold', color: '#fbbf24' }}>⭐ {playerStats.rating}</div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Potential</div>
                  <div style={{ fontSize: '1.3em', fontWeight: 'bold', color: '#8b5cf6' }}>🎯 {playerStats.overallPotential}</div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Fitness</div>
                  <div style={{ fontSize: '1.3em', fontWeight: 'bold', color: playerStats.fitness >= 80 ? '#10b981' : playerStats.fitness >= 50 ? '#f59e0b' : '#ef4444' }}>
                    💪 {playerStats.fitness}%
                  </div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Marktwert</div>
                  <div style={{ fontSize: '1.1em', fontWeight: 'bold', color: '#10b981' }}>💰 {formatCurrency(playerStats.marketValue)}</div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Gehalt</div>
                  <div style={{ fontSize: '1.1em', fontWeight: 'bold' }}>💵 {formatCurrency(playerStats.salary)}</div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Vertragslaufzeit</div>
                  <div style={{ fontSize: '1.1em', fontWeight: 'bold', color: playerStats.contractLength <= 1 ? '#ef4444' : '#10b981' }}>
                    📝 {playerStats.contractLength} {playerStats.contractLength === 1 ? 'Saison' : 'Saisons'}
                  </div>
                </div>
              </div>
            </div>

            {/* Saisonstatistiken */}
            <div className="card" style={{ marginBottom: 16, padding: 16 }}>
              <h4 style={{ marginTop: 0, marginBottom: 12 }}>📈 Saisonstatistiken</h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 16 }}>
                <div style={{ textAlign: 'center' }}>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Spiele</div>
                  <div style={{ fontSize: '1.5em', fontWeight: 'bold' }}>{playerStats.matchesPlayed}</div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Tore</div>
                  <div style={{ fontSize: '1.5em', fontWeight: 'bold', color: '#10b981' }}>⚽ {playerStats.totalGoals}</div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Vorlagen</div>
                  <div style={{ fontSize: '1.5em', fontWeight: 'bold', color: '#3b82f6' }}>🎯 {playerStats.totalAssists}</div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Ø Note</div>
                  <div style={{ fontSize: '1.5em', fontWeight: 'bold', color: '#fbbf24' }}>
                    {playerStats.averageRating ? formatRating(playerStats.averageRating) : '-'}
                  </div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Gelbe Karten</div>
                  <div style={{ fontSize: '1.5em', fontWeight: 'bold', color: '#f59e0b' }}>🟨 {playerStats.totalYellowCards}</div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div className="muted" style={{ fontSize: '0.85em' }}>Rote Karten</div>
                  <div style={{ fontSize: '1.5em', fontWeight: 'bold', color: '#ef4444' }}>🟥 {playerStats.totalRedCards}</div>
                </div>
              </div>
            </div>

            {/* Letzte Spiele */}
            <div className="card" style={{ padding: 16 }}>
              <h4 style={{ marginTop: 0, marginBottom: 12 }}>🎮 Letzte Spiele</h4>
              {playerStats.recentPerformances && playerStats.recentPerformances.length > 0 ? (
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9em' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--border)', textAlign: 'left' }}>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)' }}>Spieltag</th>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)' }}>Gegner</th>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)', textAlign: 'center' }}>Ergebnis</th>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)', textAlign: 'center' }}>Note</th>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)', textAlign: 'center' }}>⚽</th>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)', textAlign: 'center' }}>🎯</th>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)', textAlign: 'center' }}>🟨</th>
                        <th style={{ padding: '8px 4px', color: 'var(--muted)', textAlign: 'center' }}>🟥</th>
                      </tr>
                    </thead>
                    <tbody>
                      {playerStats.recentPerformances.map((perf, index) => (
                        <tr key={index} style={{ borderBottom: '1px solid var(--border)' }}>
                          <td style={{ padding: '8px 4px' }}>Spieltag {perf.matchday}</td>
                          <td style={{ padding: '8px 4px' }}>
                            {perf.isHomeMatch ? '🏠' : '✈️'} {perf.opponent}
                          </td>
                          <td style={{ padding: '8px 4px', textAlign: 'center' }}>
                            <span style={{
                              display: 'inline-block',
                              padding: '2px 8px',
                              borderRadius: 4,
                              backgroundColor: getResultColor(perf.result),
                              color: '#fff',
                              fontWeight: 'bold',
                              fontSize: '0.85em'
                            }}>
                              {getResultText(perf.result)}
                            </span>
                            <div style={{ fontSize: '0.8em', marginTop: 2, color: 'var(--muted)' }}>
                              {perf.homeGoals !== null ? `${perf.homeGoals}:${perf.awayGoals}` : '-'}
                            </div>
                          </td>
                          <td style={{ padding: '8px 4px', textAlign: 'center', fontWeight: 'bold', color: '#fbbf24' }}>
                            {formatRating(perf.rating)}
                          </td>
                          <td style={{ padding: '8px 4px', textAlign: 'center', fontWeight: 'bold', color: perf.goals > 0 ? '#10b981' : 'inherit' }}>
                            {perf.goals || '-'}
                          </td>
                          <td style={{ padding: '8px 4px', textAlign: 'center', fontWeight: 'bold', color: perf.assists > 0 ? '#3b82f6' : 'inherit' }}>
                            {perf.assists || '-'}
                          </td>
                          <td style={{ padding: '8px 4px', textAlign: 'center', color: perf.yellowCards > 0 ? '#f59e0b' : 'inherit' }}>
                            {perf.yellowCards || '-'}
                          </td>
                          <td style={{ padding: '8px 4px', textAlign: 'center', color: perf.redCards > 0 ? '#ef4444' : 'inherit' }}>
                            {perf.redCards || '-'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="muted" style={{ textAlign: 'center', padding: 20 }}>
                  Noch keine Spiele absolviert
                </div>
              )}
            </div>
          </>
        ) : (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <div className="muted">Spieler nicht gefunden</div>
          </div>
        )}
      </div>
    </div>
  )
}
