import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'

export default function AuctionSection() {
  const { team } = useGame()
  const [activeAuctions, setActiveAuctions] = useState([])
  const [selectedAuction, setSelectedAuction] = useState(null)
  const [bidAmount, setBidAmount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [showBidModal, setShowBidModal] = useState(false)
  const [showNegotiateModal, setShowNegotiateModal] = useState(false)
  const [negotiateAuction, setNegotiateAuction] = useState(null)
  const [negotiationSalary, setNegotiationSalary] = useState(0)
  const [negotiationContractLength, setNegotiationContractLength] = useState(1)
  const [auctionDetails, setAuctionDetails] = useState(null)

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  if (!team) {
    return (
      <div>
        <h3>Auktion - Bieten</h3>
        <p className="muted">Kein aktives Team</p>
      </div>
    )
  }

  // Lade aktive Auktionen
  useEffect(() => {
    loadActiveAuctions()
    const interval = setInterval(loadActiveAuctions, 30000) // Aktualisiere alle 30 Sekunden
    return () => clearInterval(interval)
  }, [])

  const loadActiveAuctions = () => {
    setLoading(true)
    fetch(`${API_BASE}/api/v2/auction/active`)
      .then(r => {
        console.log('[AuctionSection] Response status:', r.status)
        if (!r.ok) {
          console.error('[AuctionSection] Response not ok:', r.status, r.statusText)
          throw new Error('HTTP ' + r.status)
        }
        return r.json()
      })
      .then(data => {
        console.log('[AuctionSection] Data received:', data)
        if (Array.isArray(data)) {
          setActiveAuctions(data)
        } else {
          console.warn('[AuctionSection] Data is not an array:', data)
          setActiveAuctions([])
        }
        setLoading(false)
      })
      .catch(e => {
        console.error('Fehler beim Laden der Auktionen:', e)
        setActiveAuctions([])
        setLoading(false)
      })
  }

  const loadAuctionDetails = (auctionId) => {
    fetch(`${API_BASE}/api/v2/auction/${auctionId}/details?teamId=${team.id}`)
      .then(r => r.json())
      .then(data => {
        setAuctionDetails(data)
        setSelectedAuction(data)
      })
      .catch(e => console.error('Fehler beim Laden der Auktionsdetails:', e))
  }

  const handleSelectAuction = (auction) => {
    loadAuctionDetails(auction.id)
    setBidAmount(Math.max(auction.marketValue, (auction.highestBidAmount || 0) + 1000))
    setShowBidModal(true)
  }

  const placeBid = () => {
    if (bidAmount < selectedAuction.marketValue) {
      setMessage('Gebot unter Mindestpreis!')
      return
    }

    if (selectedAuction.highestBidAmount && bidAmount <= selectedAuction.highestBidAmount) {
      setMessage('Gebot muss höher als aktuelles Gebot sein!')
      return
    }

    fetch(`${API_BASE}/api/v2/auction/${selectedAuction.id}/bid`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        teamId: team.id,
        bidAmount: bidAmount
      })
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          setMessage('✅ Gebot erfolgreich abgegeben!')
          setShowBidModal(false)
          loadActiveAuctions()
          loadAuctionDetails(selectedAuction.id)
        } else {
          setMessage('❌ ' + data.message)
        }
      })
      .catch(e => setMessage('Fehler beim Abgeben des Gebots: ' + e.message))
  }

  const handleNegotiate = (auction) => {
    // Prüfe ob dieses Team gewonnen hat
    if (auction.winnerTeamId !== team.id) {
      setMessage('❌ Nur das gewinnende Team kann Verhandlungen führen!')
      return
    }

    if (auction.auctionStatus !== 'completed') {
      setMessage('❌ Auktion ist noch nicht abgeschlossen!')
      return
    }

    setNegotiateAuction(auction)
    setNegotiationSalary(auction.salary)
    setNegotiationContractLength(auction.contractLength)
    setShowNegotiateModal(true)
  }

  const completeNegotiation = () => {
    if (negotiationSalary < negotiateAuction.salary) {
      setMessage('❌ Spieler lehnt ab - Gehalt zu niedrig!')
      return
    }

    fetch(`${API_BASE}/api/v2/auction/${negotiateAuction.id}/complete-negotiation`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        teamId: team.id,
        newSalary: negotiationSalary,
        contractLength: negotiationContractLength
      })
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          setMessage('✅ Transfer abgeschlossen! ' + data.player + ' wechselt zu ' + team.name)
          setShowNegotiateModal(false)
          loadActiveAuctions()
        } else {
          setMessage('❌ ' + data.message)
        }
      })
      .catch(e => setMessage('Fehler beim Abschließen der Verhandlung: ' + e.message))
  }

  const formatCurrency = (value) => {
    if (value >= 1000000) return '€' + (value / 1000000).toFixed(1) + 'M'
    if (value >= 1000) return '€' + (value / 1000).toFixed(1) + 'K'
    return '€' + value
  }

  const formatTime = (instant) => {
    if (!instant) return 'N/A'
    const date = new Date(instant)
    return date.toLocaleString('de-DE')
  }

  const getRemainingTime = (endTime) => {
    if (!endTime) return 'Abgelaufen'
    const now = new Date()
    const end = new Date(endTime)
    const diff = end - now
    
    if (diff <= 0) return 'Abgelaufen'
    
    const hours = Math.floor(diff / 3600000)
    const minutes = Math.floor((diff % 3600000) / 60000)
    
    return `${hours}h ${minutes}m`
  }

  return (
    <div style={{ padding: '10px' }}>
      <h3>🏷️ Auktion - Bieten</h3>
      
      {message && (
        <div style={{
          padding: '10px',
          marginBottom: '10px',
          backgroundColor: message.includes('✅') ? '#2d5016' : '#5d2d2d',
          border: '1px solid ' + (message.includes('✅') ? '#4a7c0c' : '#8b4040'),
          color: message.includes('✅') ? '#90ee90' : '#ff6b6b',
          borderRadius: '4px'
        }}>
          {message}
        </div>
      )}

      {loading ? (
        <p>Lädt Auktionen...</p>
      ) : !Array.isArray(activeAuctions) ? (
        <p className="muted">Fehler beim Laden der Auktionen</p>
      ) : activeAuctions.length === 0 ? (
        <p className="muted">Keine aktiven Auktionen verfügbar</p>
      ) : (
        <div>
          <p style={{ color: '#aaa', fontSize: '12px' }}>
            Es gibt {activeAuctions.length} aktive Auktionen
          </p>
          
          <table style={{
            width: '100%',
            borderCollapse: 'collapse',
            marginTop: '10px',
            backgroundColor: '#1a1a1a',
            border: '1px solid #444'
          }}>
            <thead>
              <tr style={{ backgroundColor: '#222', borderBottom: '2px solid #444' }}>
                <th style={{ padding: '8px', textAlign: 'left', borderRight: '1px solid #444' }}>Spieler</th>
                <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Position</th>
                <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Rating</th>
                <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Alter</th>
                <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Mindestgebot</th>
                <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Höchstes Gebot</th>
                <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Verbleibend</th>
                <th style={{ padding: '8px', textAlign: 'center' }}>Aktion</th>
              </tr>
            </thead>
            <tbody>
              {activeAuctions.map(auction => (
                <tr key={auction.id} style={{ borderBottom: '1px solid #333' }}>
                  <td style={{ padding: '8px', borderRight: '1px solid #333' }}>
                    <strong>{auction.playerName}</strong>
                  </td>
                  <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333' }}>
                    {auction.position}
                  </td>
                  <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333' }}>
                    <span style={{ color: auction.rating >= 75 ? '#90ee90' : '#aaa' }}>
                      {auction.rating}
                    </span>
                  </td>
                  <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333' }}>
                    {auction.age}
                  </td>
                  <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333', color: '#90ee90' }}>
                    {formatCurrency(auction.marketValue)}
                  </td>
                  <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333', color: '#ffb347' }}>
                    {auction.highestBidAmount ? formatCurrency(auction.highestBidAmount) : '-'}
                  </td>
                  <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333', color: '#ff6b6b' }}>
                    <strong>{getRemainingTime(auction.auctionEndTime)}</strong>
                  </td>
                  <td style={{ padding: '8px', textAlign: 'center' }}>
                    <button
                      onClick={() => handleSelectAuction(auction)}
                      style={{
                        padding: '5px 10px',
                        backgroundColor: '#0066cc',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '3px',
                        cursor: 'pointer',
                        fontSize: '12px'
                      }}
                    >
                      Bieten
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Bid Modal */}
      {showBidModal && selectedAuction && (
        <div style={{
          position: 'fixed',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          backgroundColor: '#1a1a1a',
          border: '2px solid #666',
          borderRadius: '8px',
          padding: '20px',
          zIndex: 1000,
          maxWidth: '400px',
          width: '90%',
          maxHeight: '80vh',
          overflowY: 'auto'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
            <h4>{selectedAuction.playerName} - Gebot abgeben</h4>
            <button
              onClick={() => setShowBidModal(false)}
              style={{ backgroundColor: '#333', border: 'none', color: '#fff', cursor: 'pointer', padding: '5px 10px', borderRadius: '3px' }}
            >
              ✕
            </button>
          </div>

          <div style={{ marginBottom: '10px' }}>
            <p><strong>Spieler:</strong> {selectedAuction.playerName} ({selectedAuction.position})</p>
            <p><strong>Rating:</strong> {selectedAuction.rating}</p>
            <p><strong>Alter:</strong> {selectedAuction.age}</p>
            <hr style={{ borderColor: '#444' }} />
            <p style={{ backgroundColor: '#1a3a1a', padding: '8px', borderRadius: '4px', color: '#90ee90' }}>
              <strong>📋 Nach Gewinn automatisch:</strong><br/>
              💰 Gehalt: {formatCurrency(selectedAuction.salary)}/Saison<br/>
              📅 Vertrag: 3 Saisons
            </p>
            <hr style={{ borderColor: '#444' }} />
            <p style={{ color: '#90ee90' }}><strong>Mindestgebot:</strong> {formatCurrency(selectedAuction.marketValue)}</p>
            {selectedAuction.highestBidAmount && (
              <p style={{ color: '#ffb347' }}>
                <strong>Aktuelles Höchstgebot:</strong> {formatCurrency(selectedAuction.highestBidAmount)}
                {selectedAuction.highestBidderTeamId === team.id && (
                  <span style={{ color: '#90ee90' }}> (Ihr Gebot)</span>
                )}
              </p>
            )}
          </div>

          <div style={{ marginBottom: '15px' }}>
            <label style={{ display: 'block', marginBottom: '5px' }}>Gebotsbetrag:</label>
            <input
              type="number"
              value={bidAmount}
              onChange={(e) => setBidAmount(parseInt(e.target.value) || 0)}
              style={{
                width: '100%',
                padding: '8px',
                backgroundColor: '#222',
                border: '1px solid #444',
                color: '#fff',
                borderRadius: '3px',
                boxSizing: 'border-box'
              }}
            />
            <p style={{ fontSize: '12px', color: '#aaa', marginTop: '5px' }}>
              Gerundetes Gebot: {formatCurrency(bidAmount)}
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', justifyContent: 'space-between' }}>
            <button
              onClick={() => setShowBidModal(false)}
              style={{
                flex: 1,
                padding: '10px',
                backgroundColor: '#333',
                color: '#fff',
                border: 'none',
                borderRadius: '3px',
                cursor: 'pointer'
              }}
            >
              Abbrechen
            </button>
            <button
              onClick={placeBid}
              style={{
                flex: 1,
                padding: '10px',
                backgroundColor: '#0066cc',
                color: '#fff',
                border: 'none',
                borderRadius: '3px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              Gebot abgeben
            </button>
          </div>
        </div>
      )}

      {/* Overlay */}
      {showBidModal && (
        <div
          onClick={() => setShowBidModal(false)}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            zIndex: 999
          }}
        />
      )}
    </div>
  )
}
