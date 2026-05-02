import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'
import ScoutSection from './ScoutSection'
import AuctionSection from './AuctionSection'

export default function TransferMarket(){
  const { team } = useGame()
  const [tab, setTab] = useState('available') // 'available', 'myPlayers', 'myOffers', 'scout', 'auction', 'history'
  const [availablePlayers, setAvailablePlayers] = useState([])
  const [myPlayers, setMyPlayers] = useState([])
  const [myOutgoingOffers, setMyOutgoingOffers] = useState([]) // Meine Angebote für andere Spieler
  const [myIncomingOffers, setMyIncomingOffers] = useState([]) // Angebote für meine Spieler
  const [transferHistory, setTransferHistory] = useState([]) // Transfer-Historie
  const [loading, setLoading] = useState(false)
  const [searchFilter, setSearchFilter] = useState({ position: '', minRating: '', maxRating: '', onTransferList: false, showFreeAgentsOnly: false })
  const [selectedPlayer, setSelectedPlayer] = useState(null)
  const [showOfferModal, setShowOfferModal] = useState(false)
  const [offerPrice, setOfferPrice] = useState(0)
  const [playerForOffer, setPlayerForOffer] = useState(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [selectedTeamDetails, setSelectedTeamDetails] = useState(null)
  const [showTeamModal, setShowTeamModal] = useState(false)
  const [offerToAccept, setOfferToAccept] = useState(null)
  const [showAcceptModal, setShowAcceptModal] = useState(false)
  const [showNegotiateModal, setShowNegotiateModal] = useState(false)
  const [negotiateOffer, setNegotiateOffer] = useState(null)
  const [negotiationContractLength, setNegotiationContractLength] = useState(2)
  const [negotiationSalary, setNegotiationSalary] = useState(0)
  const PLAYERS_PER_PAGE = 50

  // API base: when frontend is served separately from backend (npm start with Vite),
  // set VITE_API_URL or window.__API_BASE__ to point to backend (e.g. http://localhost:8080)
 const API_BASE = 'http://192.168.178.21:8080'

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
       setCurrentPage(1)
     }
   }, [tab])

   // Lade meine Spieler
   useEffect(() => {
     if (tab === 'myPlayers') {
       loadMyPlayers()
     }
   }, [tab])

   // Lade meine Angebote
   useEffect(() => {
     if (tab === 'myOffers') {
       loadMyOffers()
     }
   }, [tab])

   // Lade Transfer-Historie
   useEffect(() => {
     if (tab === 'history') {
       loadTransferHistory()
     }
   }, [tab])

   const loadAvailablePlayers = () => {
     setLoading(true)
     fetch(`${API_BASE}/api/v2/transfer-market/available?teamId=${team.id}`)
       .then(r => r.json())
       .then(data => {
         // Sortiere nach Marktwert absteigend
         const sorted = data.sort((a, b) => (b.marketValue || 0) - (a.marketValue || 0))
         setAvailablePlayers(sorted)
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler beim Laden der Spieler:', e)
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

   const loadMyOffers = () => {
     setLoading(true)
     // Lade meine abgegebenen Angebote, eingehende Angebote UND Free-Agent-Angebote
     Promise.all([
       fetch(`${API_BASE}/api/v2/transfer-market/my-offers/outgoing?teamId=${team.id}`).then(r => r.json()),
       fetch(`${API_BASE}/api/v2/transfer-market/my-offers/incoming?teamId=${team.id}`).then(r => r.json()),
       fetch(`${API_BASE}/api/v2/players/team/${team.id}/free-agent-offers`).then(r => r.json())
     ])
       .then(([outgoing, incoming, freeAgentOffers]) => {
         // Kombiniere reguläre Angebote mit Free-Agent-Angeboten
         const combinedOutgoing = [...(outgoing || []), ...(freeAgentOffers || [])]
         setMyOutgoingOffers(combinedOutgoing)
         setMyIncomingOffers(incoming || [])
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler beim Laden der Angebote:', e)
         setLoading(false)
       })
   }

   const loadTransferHistory = () => {
     setLoading(true)
     fetch(`${API_BASE}/api/v2/auction/transfer-history`)
       .then(r => r.json())
       .then(data => {
         setTransferHistory(data || [])
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler beim Laden der Transfer-Historie:', e)
         setLoading(false)
       })
   }

   const handleSearch = () => {
     setLoading(true)
     const params = new URLSearchParams()
     params.append('teamId', team.id)
     if (searchFilter.position) params.append('position', searchFilter.position)
     if (searchFilter.minRating) params.append('minRating', searchFilter.minRating)
     if (searchFilter.maxRating) params.append('maxRating', searchFilter.maxRating)
     if (searchFilter.onTransferList) params.append('onTransferList', 'true')
     if (searchFilter.showFreeAgentsOnly) params.append('freeAgentsOnly', 'true')
     
     fetch(`${API_BASE}/api/v2/transfer-market/available?${params}`)
       .then(r => r.json())
       .then(data => {
         // Sortiere nach Marktwert absteigend
         const sorted = data.sort((a, b) => (b.marketValue || 0) - (a.marketValue || 0))
         setAvailablePlayers(sorted)
         setCurrentPage(1)
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler bei der Suche:', e)
         setLoading(false)
       })
   }

   const makeOffer = (playerId) => {
     const player = availablePlayers.find(p => p.id === playerId)
     if (!player) return
     
     setPlayerForOffer(player)
     // Für freie Spieler: Gehalt statt Ablöse
     if (player.isFreeAgent) {
       setOfferPrice(player.salary || 50000) // Standard-Gehalt
       setNegotiationContractLength(2) // Standard-Vertragslaufzeit
     } else {
       setOfferPrice(player.marketValue || 0)
     }
     setShowOfferModal(true)
   }

   const submitOffer = () => {
     if (!playerForOffer) return
     
     // Für freie Spieler: Angebot ohne Ablöse
     if (playerForOffer.isFreeAgent) {
       setLoading(true)
       fetch(`${API_BASE}/api/v2/players/${playerForOffer.id}/offer-free-agent`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify({
           teamId: team.id,
           salary: offerPrice,
           contractLength: negotiationContractLength
         })
       })
         .then(r => r.json())
         .then(data => {
           setShowOfferModal(false)
           setPlayerForOffer(null)
           setOfferPrice(0)
           if (data.success) {
             alert(data.message)
             // Aktualisiere nur den Status des Spielers in der aktuellen Liste
             setAvailablePlayers(availablePlayers.map(p => 
               p.id === playerForOffer.id ? { ...p, hasOffer: true, offerStatus: 'pending' } : p
             ))
           } else {
             alert('Fehler: ' + data.message)
           }
           setLoading(false)
         })
         .catch(e => {
           console.error('Fehler:', e)
           alert('Fehler beim Abgeben des Angebots')
           setLoading(false)
         })
       return
     }
     
     // Normales Angebot mit Ablöse
     setLoading(true)
     fetch(`${API_BASE}/api/v2/transfer-market/offer`, {
       method: 'POST',
       headers: { 'Content-Type': 'application/json' },
       body: JSON.stringify({
         playerId: playerForOffer.id,
         buyingTeamId: team.id,
         offerPrice: offerPrice
       })
     })
       .then(r => {
         if (r.ok) {
           setShowOfferModal(false)
           setPlayerForOffer(null)
           setOfferPrice(0)
           loadAvailablePlayers()
           if (myOutgoingOffers.length > 0 || myIncomingOffers.length > 0) {
             loadMyOffers()
           }
         } else {
           alert('Fehler beim Abgeben des Angebots')
         }
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler:', e)
         setLoading(false)
       })
   }

   const listPlayerForSale = (player) => {
     setLoading(true)
     fetch(`${API_BASE}/api/v2/transfer-market/list/${player.id}?teamId=${team.id}`, { method: 'POST' })
       .then(r => {
         if (r.ok) {
           loadMyPlayers()
         } else {
           alert('Fehler beim Hinzufügen zur Transferliste')
         }
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler:', e)
         alert('Fehler beim Hinzufügen zur Transferliste')
         setLoading(false)
       })
   }

   const removePlayerFromSale = (playerId) => {
     setLoading(true)
     fetch(`${API_BASE}/api/v2/transfer-market/list/${playerId}?teamId=${team.id}`, { method: 'DELETE' })
       .then(r => {
         if (r.ok) {
           loadMyPlayers()
           loadAvailablePlayers()
         } else {
           alert('Fehler beim Entfernen des Spielers')
         }
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler:', e)
         setLoading(false)
       })
   }

   const openTeamDetails = (teamId) => {
     setLoading(true)
     fetch(`${API_BASE}/api/teams/${teamId}/details`)
       .then(r => r.json())
       .then(data => {
         setSelectedTeamDetails(data)
         setShowTeamModal(true)
         setLoading(false)
       })
       .catch(e => {
         console.error('Fehler beim Laden der Team-Details:', e)
         setLoading(false)
       })
   }

    const acceptIncomingOffer = (offerInfo) => {
      // Kombiniere das Angebot mit dem Spieler-Namen und allen Infos
      const fullOffer = {
        ...offerInfo,
        name: offerInfo.playerName || 'Unbekannter Spieler'
      }
      setOfferToAccept(fullOffer)
      setShowAcceptModal(true)
    }

    const handleNegotiate = (offerInfo) => {
      // Öffne Gehaltsverhandlungs-Modal
      console.log("handleNegotiate called with offerInfo:", offerInfo)
      console.log("Current team ID:", team.id)
      setNegotiateOffer(offerInfo)
      // Setze Standard-Werte: 2 Saisons und aktuelles Gehalt des Spielers
      setNegotiationContractLength(2)
      setNegotiationSalary(offerInfo.currentSalary || 0)
      setShowNegotiateModal(true)
    }

    const confirmNegotiation = () => {
      if (!negotiateOffer || negotiationSalary < 0) return
      
      setLoading(true)
      
      // Spieler akzeptiert, wenn das Gehalt gleich oder höher als aktuelles Gehalt ist
      const playerAccepts = negotiationSalary >= (negotiateOffer.currentSalary || 0)
      
      if (playerAccepts) {
        // Sende Gehaltsverhandlung zum Backend
        const requestBody = {
          offerId: negotiateOffer.id,
          teamId: team.id,
          newSalary: negotiationSalary,
          contractLength: negotiationContractLength
        }
        
        console.log("Sending negotiation request:", requestBody)
        
        fetch(`${API_BASE}/api/v2/transfer-market/complete-negotiation`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        })
          .then(r => {
            console.log("Response status:", r.status)
            if (r.ok) {
              return r.json()
            }
            return r.json().then(data => {
              throw new Error(data.message || 'Fehler beim Abschließen der Verhandlung')
            })
          })
          .then(data => {
            console.log("Success response:", data)
            if (data.success) {
              setShowNegotiateModal(false)
              loadMyOffers() // Reload offers
              loadMyPlayers() // Reload squad
              window.dispatchEvent(new Event('teamUpdated')) // Trigger team update (budget, etc)
            } else {
              alert('Fehler: ' + data.message)
            }
            setLoading(false)
          })
          .catch(e => {
            console.error('Fehler:', e)
            alert('Fehler beim Abschließen der Verhandlung: ' + e.message)
            setLoading(false)
          })
      } else {
        // Spieler lehnt ab
        alert(`✗ ${negotiateOffer.playerName} hat abgelehnt - Gehalt zu niedrig!`)
        setShowNegotiateModal(false)
        setLoading(false)
      }
    }

    const handleRejectOffer = (offerId) => {
      if (!offerId) return
      
      setLoading(true)
      fetch(`${API_BASE}/api/v2/transfer-market/reject-offer/${offerId}`, {
        method: 'DELETE'
      })
        .then(r => {
          if (r.ok) {
            return r.json()
          }
          throw new Error('Fehler beim Ablehnen des Angebots')
        })
        .then(data => {
          if (data.success) {
            loadMyOffers() // Reload offers
          } else {
            alert('Fehler: ' + data.message)
          }
          setLoading(false)
        })
        .catch(e => {
          console.error('Fehler:', e)
          alert('Fehler beim Löschen des Angebots')
          setLoading(false)
        })
    }

    const confirmAcceptOffer = () => {
      if (!offerToAccept) return
      
      setLoading(true)
      fetch(`${API_BASE}/api/v2/transfer-market/accept-offer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          offerId: offerToAccept.id,
          teamId: team.id
        })
      })
        .then(r => {
          if (r.ok) {
            return r.json()
          }
          throw new Error('Fehler beim Annehmen des Angebots')
        })
        .then(data => {
          if (data.success) {
            setShowAcceptModal(false)
            setOfferToAccept(null)
            loadMyOffers()
            loadMyPlayers()
            // Aktualisiere den Team-Kontext, um Budget und Aufstellung zu refreshen
            window.dispatchEvent(new Event('teamUpdated'))
          } else {
            alert('Fehler: ' + data.message)
          }
          setLoading(false)
        })
        .catch(e => {
          console.error('Fehler:', e)
          alert('Fehler beim Annehmen des Angebots')
          setLoading(false)
        })
    }

   const formatValue = (value) => {
     if (value >= 1000000) {
       return (value / 1000000).toFixed(1) + 'M €'
     } else if (value >= 1000) {
       return (value / 1000).toFixed(1) + 'K €'
     }
     return value + ' €'
   }

    const PlayerCard = ({ player, showOfferBtn = false, showSellBtn = false, showRemoveFromSaleBtn = false, showAcceptBtn = false, onOffer, onSell, onRemoveFromSale, onAccept, onTeamClick, offerInfo = null, hideOfferInfo = false, hideTeamInfo = false, onNegotiate = null, onRejectOffer = null, isOutgoingOffer = false }) => (
      <div className="card" style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'start', 
        marginBottom: 8,
        border: player.isFreeAgent ? '2px solid #10b981' : undefined,
        background: player.isFreeAgent ? 'rgba(16, 185, 129, 0.05)' : undefined
      }}>
        <div>
          <strong>{player.name}</strong>
          <div className="muted">Position: {player.position} · Land: {player.country}</div>
          {!hideTeamInfo && player.isFreeAgent && (
            <div style={{ color: '#10b981', fontWeight: 'bold', marginTop: 4, fontSize: '0.9em' }}>
              ⭐ FREIER SPIELER - Keine Ablöse!
              {player.hasOffer && (
                <span style={{ 
                  marginLeft: 8, 
                  padding: '2px 8px', 
                  background: player.offerStatus === 'outbid' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(59, 130, 246, 0.2)',
                  border: '1px solid ' + (player.offerStatus === 'outbid' ? '#ef4444' : '#3b82f6'),
                  borderRadius: '4px',
                  fontSize: '0.85em',
                  color: player.offerStatus === 'outbid' ? '#fca5a5' : '#93c5fd'
                }}>
                  {player.offerStatus === 'outbid' ? '❌ Überboten' : '✅ Angebot abgegeben'}
                </span>
              )}
            </div>
          )}
          {!hideTeamInfo && !player.isFreeAgent && player.teamId ? (
            <div className="muted">
              Team: <span 
                style={{ cursor: 'pointer', textDecoration: 'underline', color: '#6366f1' }}
                onClick={() => onTeamClick && onTeamClick(player.teamId)}
              >
                {player.teamName || `Team ${player.teamId}`}
              </span>
              {player.onTransferList && <span style={{ marginLeft: 8, color: '#f59e0b', fontWeight: 'bold' }}>🏪 Auf Transferliste</span>}
            </div>
          ) : (
            !hideTeamInfo && !player.isFreeAgent && <div className="muted" style={{ color: '#f59e0b', fontWeight: 'bold' }}>🏪 Auf Transferliste</div>
          )}
          <div className="muted">Alter: {player.age} · Rating: {player.rating} · Potenzial: {player.potential}</div>
          {!player.isFreeAgent && (
            <div className="muted">
              Gehalt: {formatValue(player.salary)}/Spieltag · Marktwert: {formatValue(player.marketValue)}
            </div>
          )}
          {player.isFreeAgent && (
            <div className="muted" style={{ color: '#9ca3af' }}>
              Sucht neuen Verein - Entscheidung in 2 Spieltagen
            </div>
          )}
          {player.contractEndDate && (
            <div className="muted">Vertrag bis: {new Date(player.contractEndDate).toLocaleDateString()}</div>
          )}
          {offerInfo && !hideOfferInfo && (
            <div style={{ 
              marginTop: 6, 
              padding: 8, 
              background: offerInfo.isFreeAgent ? 'rgba(16, 185, 129, 0.1)' : 'rgba(100, 150, 255, 0.1)', 
              borderRadius: 4,
              border: offerInfo.isFreeAgent ? '1px solid #10b981' : 'none'
            }}>
              {offerInfo.isFreeAgent ? (
                <>
                  <div className="muted">💰 Gehalt-Angebot: {formatValue(offerInfo.offerPrice)}/Spieltag</div>
                  <div className="muted">📅 Vertragslaufzeit: {offerInfo.contractLength} Saisons</div>
                  <div className="muted">
                    Status: <strong style={{
                      color: offerInfo.status === 'pending' ? '#3b82f6' : 
                             offerInfo.status === 'outbid' ? '#ef4444' : '#10b981'
                    }}>
                      {offerInfo.status === 'pending' ? '⏳ Wartet auf Entscheidung' : 
                       offerInfo.status === 'outbid' ? '❌ Überboten' : 
                       offerInfo.status === 'accepted' ? '✓ Angenommen' : offerInfo.status}
                    </strong>
                  </div>
                </>
              ) : (
                <>
                  <div className="muted">💰 Angebot: {formatValue(offerInfo.offerPrice)}</div>
                  <div className="muted">
                    📤 Von: <span 
                      style={{ cursor: 'pointer', textDecoration: 'underline', color: '#6366f1', fontWeight: 'bold' }}
                      onClick={() => onTeamClick && offerInfo.buyingTeamId && onTeamClick(offerInfo.buyingTeamId)}
                    >
                      {offerInfo.fromTeamName || ('Team ' + offerInfo.buyingTeamId) || '...'}
                    </span>
                  </div>
                  {offerInfo.status && (
                    <div className="muted">
                      Status: <strong style={{
                        color: offerInfo.status === 'accepted' ? '#10b981' : 
                               offerInfo.status === 'rejected' ? '#ef4444' : '#f59e0b'
                      }}>
                        {offerInfo.status === 'accepted' ? '✓ Angenommen' : 
                         offerInfo.status === 'rejected' ? '✗ Abgelehnt' : 'Ausstehend'}
                      </strong>
                    </div>
                  )}
                </>
              )}
            </div>
           )}
          </div>
        <div style={{ display: 'flex', gap: 6, flexDirection: 'column' }}>
          {showOfferBtn && (
            <button className="btn primary" onClick={() => onOffer(player.id)}>Angebot machen</button>
          )}
          {showSellBtn && (
            <button className="btn secondary" onClick={() => onSell(player)}>Zum Verkauf anbieten</button>
          )}
          {showRemoveFromSaleBtn && (
            <button className="btn warning" onClick={() => onRemoveFromSale(player.id)}>Von der Transferliste nehmen</button>
          )}
          
          {/* Für eingehende Angebote: Annehmen und Ablehnen Button */}
          {showAcceptBtn && offerInfo?.status === 'pending' && (
            <button className="btn success" onClick={() => onAccept(offerInfo)}>✓ Annehmen</button>
          )}
          {showAcceptBtn && offerInfo?.status === 'pending' && onRejectOffer && (
            <button className="btn secondary" onClick={() => onRejectOffer(offerInfo.id)} style={{ background: '#ef4444' }}>✕ Ablehnen</button>
          )}
          
          {/* Für ausgehende Angebote (von mir): Nur bei accepted/rejected Actions */}
          {isOutgoingOffer && offerInfo?.status === 'accepted' && onNegotiate && (
            <button className="btn info" onClick={() => onNegotiate(offerInfo)} style={{ background: '#8b5cf6' }}>💬 Gehaltsverhandlung</button>
          )}
          {isOutgoingOffer && offerInfo?.status === 'rejected' && onRejectOffer && (
            <button className="btn secondary" onClick={() => onRejectOffer(offerInfo.id)} style={{ background: '#ef4444' }}>🗑️ Löschen</button>
          )}
          {isOutgoingOffer && offerInfo?.status === 'pending' && (
            <span style={{ color: '#f59e0b', fontWeight: 'bold', padding: '8px', fontSize: '0.9em' }}>⏳ Wartet auf Antwort</span>
          )}
        </div>
      </div>
    )

   // Pagination
   const totalPages = Math.ceil(availablePlayers.length / PLAYERS_PER_PAGE)
   const startIdx = (currentPage - 1) * PLAYERS_PER_PAGE
   const paginatedPlayers = availablePlayers.slice(startIdx, startIdx + PLAYERS_PER_PAGE)

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
           <button className={tab === 'myOffers' ? 'active' : ''} onClick={() => setTab('myOffers')}>
             Meine Angebote
           </button>
           <button className={tab === 'auction' ? 'active' : ''} onClick={() => setTab('auction')}>
             🏷️ Bieten
           </button>
           <button className={tab === 'scout' ? 'active' : ''} onClick={() => setTab('scout')}>
             Scout
           </button>
           <button className={tab === 'history' ? 'active' : ''} onClick={() => setTab('history')}>
             📜 Wechselhistorie
           </button>
         </div>

        <div className="panel">
           {tab === 'available' && (
             <div>
               <h4>Verfügbare Spieler (sortiert nach Marktwert)</h4>
               
                <div className="card" style={{ marginBottom: 12, padding: 12 }}>
                  <h5>Spieler suchen und filtern</h5>
                   <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                     <select
                       value={searchFilter.position}
                       onChange={(e) => setSearchFilter({ ...searchFilter, position: e.target.value })}
                       style={{ 
                         padding: '6px 8px', 
                         borderRadius: '4px', 
                         border: '1px solid rgba(255,255,255,0.1)',
                         background: 'rgba(0,0,0,0.3)',
                         color: '#fff',
                         cursor: 'pointer'
                       }}
                     >
                       <option value="">Alle Positionen</option>
                       <option value="GK">GK (Torwart)</option>
                       <option value="DEF">DEF (Abwehr)</option>
                       <option value="MID">MID (Mittelfeld)</option>
                       <option value="FWD">FWD (Angriff)</option>
                     </select>
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
                     <label style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 8px', cursor: 'pointer' }}>
                       <input
                         type="checkbox"
                         checked={searchFilter.onTransferList}
                         onChange={(e) => setSearchFilter({ ...searchFilter, onTransferList: e.target.checked })}
                       />
                       <span>Nur Transferliste</span>
                     </label>
                     <label style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 8px', cursor: 'pointer' }}>
                       <input
                         type="checkbox"
                         checked={searchFilter.showFreeAgentsOnly}
                         onChange={(e) => setSearchFilter({ ...searchFilter, showFreeAgentsOnly: e.target.checked })}
                       />
                       <span>⭐ Nur freie Spieler</span>
                     </label>
                     <button className="btn primary" onClick={handleSearch}>Suchen</button>
                   </div>
                </div>

                {loading ? (
                  <p className="muted">Lädt...</p>
                ) : availablePlayers.length ? (
                  paginatedPlayers.map((player) => (
                    <PlayerCard
                      key={player.id}
                      player={player}
                      showOfferBtn
                      onOffer={makeOffer}
                      onTeamClick={openTeamDetails}
                    />
                  ))
                ) : (
                  <p className="muted">Keine Spieler verfügbar</p>
                )}

               {/* Pagination Controls */}
               {totalPages > 1 && (
                 <div className="pagination" style={{ marginTop: 16 }}>
                   <button 
                     className="btn" 
                     onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                     disabled={currentPage === 1}
                   >
                     &lt; Vorherige
                   </button>
                   <span style={{ margin: '0 12px' }}>
                     Seite {currentPage} von {totalPages}
                   </span>
                   <button 
                     className="btn" 
                     onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                     disabled={currentPage === totalPages}
                   >
                     Nächste &gt;
                   </button>
                 </div>
               )}
             </div>
           )}

            {tab === 'myPlayers' && (
              <div>
                <h4>Meine Spieler</h4>
                
                {loading ? (
                  <p className="muted">Lädt...</p>
                 ) : myPlayers.length ? (
                   myPlayers.map((player) => {
                     // Prüfe ob Spieler auf Transferliste ist (neues onTransferList Flag)
                     const isOnTransferList = player.onTransferList === true
                     return (
                       <PlayerCard
                         key={player.id}
                         player={player}
                         showSellBtn={!isOnTransferList}
                         showRemoveFromSaleBtn={isOnTransferList}
                         onSell={listPlayerForSale}
                         onRemoveFromSale={removePlayerFromSale}
                         onTeamClick={openTeamDetails}
                       />
                     )
                   })
                 ) : (
                  <p className="muted">Keine Spieler im Team</p>
                )}
              </div>
            )}

           {tab === 'myOffers' && (
             <div>
               <h4>Meine Angebote</h4>
               
               {loading ? (
                 <p className="muted">Lädt...</p>
               ) : (
                 <>
                      <h5 style={{ marginTop: 16, marginBottom: 12 }}>📤 Von mir abgegebene Angebote</h5>
                       {myOutgoingOffers.length ? (
                         myOutgoingOffers.map((offer) => {
                           const isFreeAgentOffer = offer.isFreeAgent === true
                           return (
                             <PlayerCard
                               key={offer.id}
                               player={offer}
                               hideTeamInfo={true}
                               onTeamClick={openTeamDetails}
                               onNegotiate={!isFreeAgentOffer ? handleNegotiate : null}
                               onRejectOffer={handleRejectOffer}
                               isOutgoingOffer={!isFreeAgentOffer}
                               offerInfo={{
                                 offerPrice: isFreeAgentOffer ? offer.offerSalary : offer.offerPrice,
                                 fromTeamName: team.name,
                                 buyingTeamId: offer.buyingTeamId,
                                 status: offer.status,
                                 playerName: offer.name || offer.playerName,
                                 currentSalary: offer.salary,
                                 id: offer.id,
                                 isFreeAgent: isFreeAgentOffer,
                                 contractLength: offer.contractLength
                               }}
                             />
                           )
                         })
                       ) : (
                         <p className="muted">Keine abgegebenen Angebote</p>
                       )}
                    
                     <h5 style={{ marginTop: 20, marginBottom: 12 }}>📥 Eingehende Angebote für meine Spieler</h5>
                     {myIncomingOffers.length ? (
                       myIncomingOffers.map((offer) => (
                         <PlayerCard
                           key={offer.id}
                           player={offer}
                           hideTeamInfo={true}
                           showAcceptBtn={offer.status === 'pending'}
                           onAccept={acceptIncomingOffer}
                           onTeamClick={openTeamDetails}
                           onRejectOffer={handleRejectOffer}
                           offerInfo={{
                             offerPrice: offer.offerPrice,
                             fromTeamName: offer.buyingTeamName,
                             buyingTeamId: offer.buyingTeamId,
                             status: offer.status,
                             id: offer.id,
                             playerName: offer.name,
                             currentSalary: offer.salary
                           }}
                           hideOfferInfo={false}
                         />
                       ))
                     ) : (
                       <p className="muted">Keine eingehenden Angebote</p>
                     )}
                 </>
                )}
              </div>
            )}

           {tab === 'auction' && (
             <AuctionSection />
           )}

           {tab === 'scout' && (
             <ScoutSection />
           )}

           {tab === 'history' && (
             <div>
               <h4>📜 Wechselhistorie</h4>
               
               {loading ? (
                 <p className="muted">Lädt...</p>
               ) : transferHistory.length === 0 ? (
                 <p className="muted">Noch keine Transfers über Auktionen durchgeführt</p>
               ) : (
                 <div style={{ overflowX: 'auto' }}>
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
                         <th style={{ padding: '8px', textAlign: 'left', borderRight: '1px solid #444' }}>Von</th>
                         <th style={{ padding: '8px', textAlign: 'left', borderRight: '1px solid #444' }}>Zu</th>
                         <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Preis</th>
                         <th style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #444' }}>Spieltag</th>
                         <th style={{ padding: '8px', textAlign: 'center' }}>Saison</th>
                       </tr>
                     </thead>
                     <tbody>
                       {transferHistory.map((transfer, idx) => (
                         <tr key={idx} style={{ borderBottom: '1px solid #333' }}>
                           <td style={{ padding: '8px', borderRight: '1px solid #333' }}>
                             <strong>{transfer.playerName}</strong>
                           </td>
                           <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333' }}>
                             {transfer.position}
                           </td>
                           <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333' }}>
                             <span style={{ color: transfer.rating >= 75 ? '#90ee90' : '#aaa' }}>
                               {transfer.rating}
                             </span>
                           </td>
                           <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333' }}>
                             {transfer.age}
                           </td>
                           <td style={{ padding: '8px', borderRight: '1px solid #333' }}>
                             {transfer.fromTeamName || 'Transfermarkt'}
                           </td>
                           <td style={{ padding: '8px', borderRight: '1px solid #333' }}>
                             <strong>{transfer.toTeamName}</strong>
                           </td>
                           <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333', color: '#90ee90' }}>
                             {formatValue(transfer.transferPrice)}
                           </td>
                           <td style={{ padding: '8px', textAlign: 'center', borderRight: '1px solid #333' }}>
                             Tag {transfer.matchday}
                           </td>
                           <td style={{ padding: '8px', textAlign: 'center' }}>
                             Saison {transfer.season}
                           </td>
                         </tr>
                       ))}
                     </tbody>
                   </table>
                 </div>
               )}
             </div>
           )}
         </div>
       </div>

       {showOfferModal && (
         <div className="modal-backdrop" onClick={() => setShowOfferModal(false)}>
           <div className="modal" onClick={e => e.stopPropagation()}>
             <h4>{playerForOffer?.isFreeAgent ? 'Angebot für freien Spieler' : 'Angebot für Spieler abgeben'}</h4>
             {playerForOffer && (
               <div>
                 <p><strong>{playerForOffer.name}</strong> ({playerForOffer.position})</p>
                 {playerForOffer.isFreeAgent ? (
                   <>
                     <div style={{ 
                       padding: '12px', 
                       background: 'rgba(16, 185, 129, 0.1)', 
                       borderRadius: '6px', 
                       marginBottom: 12,
                       border: '1px solid #10b981'
                     }}>
                       <div style={{ color: '#10b981', fontWeight: 'bold', marginBottom: 4 }}>
                         ⭐ FREIER SPIELER
                       </div>
                       <div className="muted" style={{ fontSize: '0.9em' }}>
                         Keine Ablösesumme erforderlich! Nur Gehalt verhandeln.
                       </div>
                     </div>
                     
                     <label style={{ display: 'block', marginBottom: 8 }}>
                       Gehalt pro Spieltag:
                       <input
                         type="text"
                         value={offerPrice.toLocaleString('de-DE')}
                         onChange={(e) => {
                           const value = e.target.value.replace(/\./g, '').replace(/,/g, '')
                           const numValue = parseInt(value) || 0
                           setOfferPrice(numValue)
                         }}
                         onFocus={(e) => {
                           if (offerPrice === 0) {
                             e.target.select()
                           }
                         }}
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
                     
                     <div className="muted" style={{ marginBottom: 12 }}>Dein Angebot: <strong>{formatValue(offerPrice)}</strong></div>

                     <label style={{ display: 'block', marginBottom: 16 }}>
                       Vertragslaufzeit:
                       <div style={{ display: 'flex', gap: '8px', marginTop: 8 }}>
                         {[2, 3, 4, 5].map(length => (
                           <button
                             key={length}
                             onClick={() => setNegotiationContractLength(length)}
                             style={{
                               flex: 1,
                               padding: '8px',
                               borderRadius: '4px',
                               border: negotiationContractLength === length ? '2px solid #3b82f6' : '1px solid rgba(255,255,255,0.2)',
                               background: negotiationContractLength === length ? 'rgba(59, 130, 246, 0.2)' : 'rgba(0,0,0,0.3)',
                               color: '#fff',
                               cursor: 'pointer',
                               fontWeight: negotiationContractLength === length ? 'bold' : 'normal'
                             }}
                           >
                             {length}
                           </button>
                         ))}
                       </div>
                     </label>
                   </>
                 ) : (
                   <>
                     <div className="muted" style={{ marginBottom: 12 }}>Marktwert: {formatValue(playerForOffer.marketValue)}</div>
                     
                     <label style={{ display: 'block', marginBottom: 8 }}>
                       Angebotspreis:
                       <input
                         type="text"
                         value={offerPrice.toLocaleString('de-DE')}
                         onChange={(e) => {
                           const value = e.target.value.replace(/\./g, '').replace(/,/g, '')
                           const numValue = parseInt(value) || 0
                           setOfferPrice(numValue)
                         }}
                         onFocus={(e) => {
                           if (offerPrice === 0) {
                             e.target.select()
                           }
                         }}
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
                     
                     <div className="muted" style={{ marginBottom: 12 }}>Dein Angebot: <strong>{formatValue(offerPrice)}</strong></div>
                   </>
                 )}

                 <div style={{ display: 'flex', gap: 8 }}>
                   <button className="btn primary" onClick={submitOffer} disabled={loading}>
                     {loading ? 'Wird gesendet...' : (playerForOffer?.isFreeAgent ? 'Angebot absenden' : 'Angebot absenden')}
                   </button>
                   <button className="btn secondary" onClick={() => setShowOfferModal(false)}>Abbrechen</button>
                 </div>
               </div>
             )}
           </div>
         </div>
       )}

       {/* Team Details Modal */}
       {showTeamModal && selectedTeamDetails && (
         <div style={{
           position: 'fixed',
           top: 0, left: 0, right: 0, bottom: 0,
           background: 'rgba(0,0,0,0.7)',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           zIndex: 1000
         }} onClick={() => setShowTeamModal(false)}>
           <div style={{
             background: '#1a1a1a',
             border: '1px solid rgba(255,255,255,0.2)',
             borderRadius: '8px',
             padding: '20px',
             maxWidth: '700px',
             width: '90%',
             maxHeight: '90vh',
             overflowY: 'auto'
           }} onClick={(e) => e.stopPropagation()}>
             <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 16 }}>
               <div>
                 <h3 style={{ margin: 0, marginBottom: 8 }}>{selectedTeamDetails.teamName}</h3>
                 <div className="muted" style={{ fontSize: '0.9em', lineHeight: '1.4' }}>
                   <div>💪 Teamstärke: <strong>{selectedTeamDetails.teamStrength}</strong></div>
                   <div>🏟️ Stadion: <strong>{selectedTeamDetails.stadiumCapacity?.toLocaleString() || 'Unbekannt'} Plätze</strong></div>
                   {selectedTeamDetails.country && <div>🌍 Land: <strong>{selectedTeamDetails.country}</strong></div>}
                   {selectedTeamDetails.leagueName && <div>🏆 Liga: <strong>{selectedTeamDetails.leagueName}</strong></div>}
                 </div>
               </div>
               <button
                 onClick={() => setShowTeamModal(false)}
                 style={{
                   background: 'rgba(255,255,255,0.1)',
                   border: 'none',
                   color: '#fff',
                   fontSize: '20px',
                   cursor: 'pointer',
                   padding: '0 8px',
                   minWidth: '32px'
                 }}
               >
                 ✕
               </button>
             </div>

             {/* Aufstellung Tab */}
             <div style={{ marginBottom: '24px' }}>
               <h4 style={{ marginBottom: '12px' }}>Aufstellung (Spieltag)</h4>
              {selectedTeamDetails.lineup && selectedTeamDetails.lineup.length > 0 ? (
                <div>
                  {selectedTeamDetails.lineup.map((player) => (
                    <div key={player.playerId} className="card" style={{ marginBottom: 8, padding: '10px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <strong>{player.playerName}</strong>
                          <div className="muted" style={{ fontSize: '0.85em' }}>
                            {player.position} • {player.age || '?'} Jahre
                          </div>
                        </div>
                        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                          <div style={{ textAlign: 'right' }}>
                            <strong>{player.rating}</strong>
                            <div className="muted" style={{ fontSize: '0.85em' }}>Rating</div>
                          </div>
                          <div style={{ textAlign: 'right' }}>
                            <strong style={{ color: player.fitness >= 80 ? '#4CAF50' : player.fitness >= 50 ? '#FFC107' : '#F44336' }}>
                              {player.fitness || '?'}
                            </strong>
                            <div className="muted" style={{ fontSize: '0.85em' }}>Fitness</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                ) : (
                  <p className="muted">Keine Aufstellung verfügbar</p>
                )}
             </div>

             {/* Alle Spieler im Kader */}
             <div>
               <h4 style={{ marginBottom: '12px' }}>Kader ({selectedTeamDetails.allPlayers?.length || 0} Spieler)</h4>
               {selectedTeamDetails.allPlayers && selectedTeamDetails.allPlayers.length > 0 ? (
                 <div style={{ overflowX: 'auto' }}>
                   <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9em' }}>
                      <thead>
                        <tr style={{ borderBottom: '2px solid rgba(255,255,255,0.2)' }}>
                          <th style={{ padding: '8px', textAlign: 'left' }}>Name</th>
                          <th style={{ padding: '8px', textAlign: 'center' }}>Pos</th>
                          <th style={{ padding: '8px', textAlign: 'center' }}>Rating</th>
                          <th style={{ padding: '8px', textAlign: 'center' }}>Fitness</th>
                          <th style={{ padding: '8px', textAlign: 'center' }}>Alter</th>
                          <th style={{ padding: '8px', textAlign: 'center' }}>Land</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedTeamDetails.allPlayers.map((player) => (
                          <tr key={player.playerId} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                            <td style={{ padding: '8px' }}>{player.playerName}</td>
                            <td style={{ padding: '8px', textAlign: 'center', color: '#999', fontSize: '0.9em' }}>{player.position}</td>
                            <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold' }}>{player.rating}</td>
                            <td style={{ padding: '8px', textAlign: 'center', fontWeight: 'bold', color: player.fitness >= 80 ? '#4CAF50' : player.fitness >= 50 ? '#FFC107' : '#F44336' }}>
                              {player.fitness || '?'}
                            </td>
                            <td style={{ padding: '8px', textAlign: 'center', color: '#999' }}>{player.age}</td>
                            <td style={{ padding: '8px', textAlign: 'center', color: '#999', fontSize: '0.85em' }}>{player.country}</td>
                          </tr>
                        ))}
                     </tbody>
                   </table>
                 </div>
               ) : (
                 <p className="muted">Kein Kader verfügbar</p>
               )}
             </div>
           </div>
         </div>
       )}

       {/* Accept Offer Modal */}
       {showAcceptModal && offerToAccept && (
          <div className="modal-backdrop" onClick={() => setShowAcceptModal(false)}>
            <div className="modal" onClick={e => e.stopPropagation()}>
              <h4>Angebot annehmen?</h4>
              <div>
                <p><strong>{offerToAccept.name}</strong></p>
                <div className="muted" style={{ marginBottom: 12 }}>
                  💰 Angebotspreis: <strong>{formatValue(offerToAccept.offerPrice)}</strong>
                </div>
                <div className="muted" style={{ marginBottom: 12 }}>
                  📤 Angebot von: <span 
                    style={{ cursor: 'pointer', textDecoration: 'underline', color: '#6366f1', fontWeight: 'bold' }}
                    onClick={() => {
                      openTeamDetails(offerToAccept.buyingTeamId)
                      setShowAcceptModal(false)
                    }}
                  >
                    {offerToAccept.fromTeamName || 'Team ' + offerToAccept.buyingTeamId}
                  </span>
                </div>
                
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn primary" onClick={confirmAcceptOffer} disabled={loading}>
                    {loading ? 'Wird verarbeitet...' : '✓ Angebot annehmen'}
                  </button>
                  <button className="btn secondary" onClick={() => setShowAcceptModal(false)}>Abbrechen</button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Negotiation Modal */}
        {showNegotiateModal && negotiateOffer && (
          <div className="modal-backdrop" onClick={() => setShowNegotiateModal(false)}>
            <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
              <h4>💬 Gehaltsverhandlung</h4>
              <div>
                <p><strong>{negotiateOffer.playerName}</strong></p>
                <div className="muted" style={{ marginBottom: 16 }}>
                  Aktuelles Gehalt: {formatValue(negotiateOffer.currentSalary || 0)} pro Spieltag
                </div>

                <div style={{ marginBottom: 16 }}>
                  <label style={{ display: 'block', marginBottom: 8 }}>
                    <strong>Vertragslaufzeit (Saisons)</strong>
                    <input
                      type="number"
                      min="1"
                      max="5"
                      value={negotiationContractLength}
                      onChange={(e) => setNegotiationContractLength(Math.min(5, Math.max(1, parseInt(e.target.value) || 1)))}
                      style={{
                        width: '100%',
                        padding: '8px',
                        marginTop: 4,
                        borderRadius: '4px',
                        border: '1px solid rgba(255,255,255,0.1)',
                        color: '#fff',
                        background: 'rgba(0,0,0,0.3)',
                        fontSize: '14px'
                      }}
                    />
                    <div className="muted" style={{ fontSize: '0.85em', marginTop: 4 }}>
                      Maximum 5 Saisons
                    </div>
                  </label>
                </div>

                <div style={{ marginBottom: 16 }}>
                  <label style={{ display: 'block', marginBottom: 8 }}>
                    <strong>Gehalt pro Spieltag</strong>
                    <input
                      type="text"
                      value={negotiationSalary.toLocaleString('de-DE')}
                      onChange={(e) => {
                        const value = e.target.value.replace(/\./g, '').replace(/,/g, '')
                        const numValue = parseInt(value) || 0
                        setNegotiationSalary(numValue)
                      }}
                      style={{
                        width: '100%',
                        padding: '8px',
                        marginTop: 4,
                        borderRadius: '4px',
                        border: '1px solid rgba(255,255,255,0.1)',
                        color: '#fff',
                        background: 'rgba(0,0,0,0.3)',
                        fontSize: '14px'
                      }}
                    />
                    <div className="muted" style={{ fontSize: '0.85em', marginTop: 4 }}>
                      {negotiationSalary >= (negotiateOffer.currentSalary || 0) ? (
                        <span style={{ color: '#10b981' }}>✓ Spieler wird akzeptieren</span>
                      ) : (
                        <span style={{ color: '#ef4444' }}>✗ Zu niedrig - Spieler wird ablehnen</span>
                      )}
                    </div>
                  </label>
                </div>

                <div style={{ background: 'rgba(100, 150, 255, 0.1)', padding: 12, borderRadius: 4, marginBottom: 16 }}>
                  <div className="muted" style={{ marginBottom: 8 }}>
                    <strong>Vertragsdetails:</strong>
                  </div>
                  <div className="muted">
                    💼 Laufzeit: <strong>{negotiationContractLength} Saisons</strong>
                  </div>
                  <div className="muted">
                    💰 Gehalt pro Saison: <strong>{formatValue(negotiationSalary)}</strong>
                  </div>
                  <div className="muted">
                    📊 Gesamtkosten: <strong>{formatValue(negotiationSalary * negotiationContractLength)}</strong>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 8 }}>
                  <button 
                    className="btn primary" 
                    onClick={confirmNegotiation} 
                    disabled={loading || negotiationSalary < 0}
                    style={{ flex: 1 }}
                  >
                    {loading ? 'Wird verarbeitet...' : '✓ Verhandlung abschließen'}
                  </button>
                  <button 
                    className="btn secondary" 
                    onClick={() => setShowNegotiateModal(false)}
                    style={{ flex: 1 }}
                  >
                    Abbrechen
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
     </div>
   )
}
