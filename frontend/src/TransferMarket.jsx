import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function TransferMarket(){
  const { team } = useGame()
  const [tab, setTab] = useState('available') // 'available', 'myPlayers', 'scout'
  const [availablePlayers, setAvailablePlayers] = useState([])
  const [myPlayers, setMyPlayers] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchFilter, setSearchFilter] = useState({ position: '', minRating: '', maxRating: '' })
  const [selectedPlayer, setSelectedPlayer] = useState(null)
  const [showPriceModal, setShowPriceModal] = useState(false)
  const [priceInput, setPriceInput] = useState(0)
  const [playerToList, setPlayerToList] = useState(null)

  // API base: when frontend is served separately from backend (npm start with Vite),
  // set VITE_API_URL or window.__API_BASE__ to point to backend (e.g. http://localhost:8080)
  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  if (!team) {
    return (
      <div>
        <h3>Transfermarkt</h3>
        <p className="muted">Kein aktives Team</p>
      </div>
    )
  }

  // Lade verfügbare Spieler
  useEffect(() => {
    if (tab === 'available') {
      loadAvailablePlayers()
    }
  }, [tab])

  // Lade meine Spieler
  useEffect(() => {
    if (tab === 'myPlayers') {
      loadMyPlayers()
    }
  }, [tab])

  const loadAvailablePlayers = () => {
    setLoading(true)
    fetch(`${API_BASE}/api/v2/transfer-market/available`)
      .then(r => r.json())
      .then(data => {
        setAvailablePlayers(data)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der verfügbaren Spieler:', e)
        setLoading(false)
      })
  }

  const loadMyPlayers = () => {
    setLoading(true)
    fetch(`${API_BASE}/api/v2/players/team/${team.id}`)
      .then(r => r.json())
      .then(data => {
        setMyPlayers(data)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Spieler:', e)
        setLoading(false)
      })
  }

  const handleSearch = () => {
    setLoading(true)
    const params = new URLSearchParams()
    if (searchFilter.position) params.append('position', searchFilter.position)
    if (searchFilter.minRating) params.append('minRating', searchFilter.minRating)
    if (searchFilter.maxRating) params.append('maxRating', searchFilter.maxRating)
    
    fetch(`${API_BASE}/api/v2/transfer-market/search?${params}`)
      .then(r => r.json())
      .then(data => {
        setAvailablePlayers(data)
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler bei der Suche:', e)
        setLoading(false)
      })
  }

  const buyPlayer = (playerId) => {
    fetch(`${API_BASE}/api/v2/transfer-market/buy/${playerId}?teamId=${team.id}`, { method: 'POST' })
      .then(r => {
        if (r.ok) {
          alert('Spieler erfolgreich gekauft!')
          loadAvailablePlayers()
        } else {
          alert('Fehler beim Kauf des Spielers')
        }
      })
      .catch(e => console.error('Fehler:', e))
  }

  const openPriceModal = (player) => {
    setPlayerToList(player)
    setPriceInput(player.marketValue || 0)
    setShowPriceModal(true)
  }

  const listPlayerForSale = () => {
    if (!playerToList) return
    
    // Note: price is currently not sent to backend (backend stores sale by teamId only).
    // If you want to persist listing price, extend the API to accept a price parameter.
    fetch(`${API_BASE}/api/v2/transfer-market/list/${playerToList.id}?teamId=${team.id}`, { method: 'POST' })
      .then(r => {
        if (r.ok) {
          alert(`Spieler erfolgreich zum Verkauf angeboten für ${formatValue(priceInput)}!`)
          setShowPriceModal(false)
          setPlayerToList(null)
          setPriceInput(0)
          loadMyPlayers()
        } else {
          alert('Fehler beim Angebot des Spielers')
        }
      })
      .catch(e => console.error('Fehler:', e))
  }

  const formatValue = (value) => {
    if (value >= 1000000) {
      return (value / 1000000).toFixed(1) + 'M €'
    } else if (value >= 1000) {
      return (value / 1000).toFixed(1) + 'K €'
    }
    return value + ' €'
  }

  const PlayerCard = ({ player, showBuyBtn = false, showSellBtn = false, onBuy, onSell }) => (
    <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 8 }}>
      <div>
        <strong>{player.name}</strong>
        <div className="muted">Position: {player.position} · Land: {player.country}</div>
        <div className="muted">Alter: {player.age} · Rating: {player.rating} · Potenzial: {player.potential}</div>
        <div className="muted">Gehalt: {formatValue(player.salary)} · Marktwert: {formatValue(player.marketValue)}</div>
        {player.contractEndDate && (
          <div className="muted">Vertrag bis: {new Date(player.contractEndDate).toLocaleDateString()}</div>
        )}
      </div>
      <div style={{ display: 'flex', gap: 6, flexDirection: 'column' }}>
        {showBuyBtn && (
          <button className="btn primary" onClick={() => onBuy(player.id)}>Kaufen</button>
        )}
        {showSellBtn && (
          <button className="btn secondary" onClick={() => onSell(player)}>Auf Transfermarkt</button>
        )}
      </div>
    </div>
  )

  return (
    <div>
      <h3>Transfermarkt</h3>

      <div className="card">
        <div className="menu" style={{ marginBottom: 12 }}>
          <button className={tab === 'available' ? 'active' : ''} onClick={() => setTab('available')}>
            Verfügbar
          </button>
          <button className={tab === 'myPlayers' ? 'active' : ''} onClick={() => setTab('myPlayers')}>
            Meine Spieler
          </button>
          <button className={tab === 'scout' ? 'active' : ''} onClick={() => setTab('scout')}>
            Scout
          </button>
        </div>

        <div className="panel">
          {tab === 'available' && (
            <div>
              <h4>Verfügbare Spieler</h4>
              
              <div className="card" style={{ marginBottom: 12, padding: 12 }}>
                <h5>Spieler suchen</h5>
                <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
                  <input
                    type="text"
                    placeholder="Position (GK, DEF, MID, FWD)"
                    value={searchFilter.position}
                    onChange={(e) => setSearchFilter({ ...searchFilter, position: e.target.value })}
                    style={{ padding: '6px 8px', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.1)' }}
                  />
                  <input
                    type="number"
                    placeholder="Min Rating"
                    value={searchFilter.minRating}
                    onChange={(e) => setSearchFilter({ ...searchFilter, minRating: e.target.value })}
                    style={{ padding: '6px 8px', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.1)' }}
                  />
                  <input
                    type="number"
                    placeholder="Max Rating"
                    value={searchFilter.maxRating}
                    onChange={(e) => setSearchFilter({ ...searchFilter, maxRating: e.target.value })}
                    style={{ padding: '6px 8px', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.1)' }}
                  />
                  <button className="btn primary" onClick={handleSearch}>Suchen</button>
                </div>
              </div>

              {loading ? (
                <p className="muted">Lädt...</p>
              ) : availablePlayers.length ? (
                availablePlayers.map((player) => (
                  <PlayerCard
                    key={player.id}
                    player={player}
                    showBuyBtn
                    onBuy={buyPlayer}
                  />
                ))
              ) : (
                <p className="muted">Keine Spieler verfügbar</p>
              )}
            </div>
          )}

          {tab === 'myPlayers' && (
            <div>
              <h4>Meine Spieler</h4>
              {loading ? (
                <p className="muted">Lädt...</p>
              ) : myPlayers.length ? (
                myPlayers.map((player) => (
                  <PlayerCard
                    key={player.id}
                    player={player}
                    showSellBtn
                    onSell={openPriceModal}
                  />
                ))
              ) : (
                <p className="muted">Keine Spieler im Team</p>
              )}
            </div>
          )}

          {tab === 'scout' && (
            <div>
              <h4>Scout / Talentsuche</h4>
              <p className="muted">Hier kannst du nach neuen Talenten suchen und sie entwickeln.</p>
              <p className="muted" style={{ fontSize: '0.9em', marginTop: 8 }}>
                Funktion folgt noch...
              </p>
            </div>
          )}
        </div>
      </div>

      {showPriceModal && (
        <div className="modal-backdrop" onClick={() => setShowPriceModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h4>Spieler zum Verkauf anbieten</h4>
            {playerToList && (
              <div>
                <p><strong>{playerToList.name}</strong> ({playerToList.position})</p>
                <div className="muted" style={{ marginBottom: 12 }}>Aktueller Marktwert: {formatValue(playerToList.marketValue)}</div>
                
                <label style={{ display: 'block', marginBottom: 8 }}>
                  Angebotspreis:
                  <input
                    type="number"
                    value={priceInput}
                    onChange={(e) => setPriceInput(parseInt(e.target.value) || 0)}
                    style={{ 
                      width: '100%', 
                      padding: '8px', 
                      marginTop: 4, 
                      borderRadius: '4px', 
                      border: '1px solid rgba(255,255,255,0.1)',
                      color: '#fff',
                      background: 'rgba(0,0,0,0.3)'
                    }}
                  />
                </label>
                
                <div className="muted" style={{ marginBottom: 12 }}>Wird angeboten für: <strong>{formatValue(priceInput)}</strong></div>

                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn primary" onClick={listPlayerForSale}>Auf Transfermarkt stellen</button>
                  <button className="btn secondary" onClick={() => setShowPriceModal(false)}>Abbrechen</button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
