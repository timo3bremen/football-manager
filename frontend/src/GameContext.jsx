import React, { createContext, useContext, useState, useEffect } from 'react'

const GameContext = createContext(null)

export function useGame(){
  return useContext(GameContext)
}

function makePlayer(id, idx){
  const positions = ['GK','DEF','MID','FWD']
  const pos = positions[Math.floor(Math.random()*positions.length)]
  return {
    id: id,
    name: `Spieler ${idx}`,
    position: pos,
    rating: Math.floor(50 + Math.random()*40),
  }
}

export function GameProvider({children}){
  const [team, setTeam] = useState(null)
  const [roster, setRoster] = useState([])
  // lineup is a map: slotId -> playerId | null
  const [lineup, setLineup] = useState({})
  const [formationRows, setFormationRows] = useState([]) // array of rows (each row = array of slotIds)
  const [currentFormation, setCurrentFormation] = useState('4-4-2')
  const [stadiumLevel, setStadiumLevel] = useState(1)
  // stadium parts: array of 30 entries, each { built: boolean, type: 'standing'|'seated'|'vip' | null, offers: Array, acceptedOffer: object|null }
  const [stadiumParts, setStadiumParts] = useState([])
  const stadiumBaseCapacity = 1000 // default base capacity (standing)
  // entry prices per type (standing, seated, vip)
  const [stadiumEntryPrice, setStadiumEntryPrice] = useState({ standing: 5, seated: 10, vip: 20 })
  const [fanFriendship, setFanFriendship] = useState(100) // percent
  const [sponsors, setSponsors] = useState([])
  const [jersey, setJersey] = useState({ color: 'weiß' })
  // finances
  const [balance, setBalance] = useState(100000)
  const [transactions, setTransactions] = useState([]) // recent txns

  // debug toggle (set localStorage fm_debug = '1' to enable)
  const debugEnabled = (()=>{
    try{ return localStorage.getItem('fm_debug') === '1' }catch(e){ return false }
  })()
  // append to local log ringbuffer (max 2000 entries)
  function appendLogEntry(entry){
    try{
      const raw = localStorage.getItem('fm_logs') || '[]'
      const arr = JSON.parse(raw)
      arr.push(entry)
      const max = 2000
      if (arr.length > max) arr.splice(0, arr.length - max)
      localStorage.setItem('fm_logs', JSON.stringify(arr))
    }catch(e){ /* ignore */ }
  }

  function getLogs(){
    try{ return JSON.parse(localStorage.getItem('fm_logs') || '[]') }catch(e){ return [] }
  }

  function clearLogs(){ try{ localStorage.removeItem('fm_logs') }catch(e){} }

  function exportLogs(){
    try{
      const raw = localStorage.getItem('fm_logs') || '[]'
      const data = (()=>{
        try{ return JSON.stringify(JSON.parse(raw), null, 2) }catch(e){ return String(raw) }
      })()
      const filename = `fm_logs_${new Date().toISOString().replace(/[:.]/g,'-')}.log`
      const blob = new Blob([data], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    }catch(e){ console.error('exportLogs failed', e) }
  }

  function log(...args){
    const serializable = args.map(a=>{
      try{ return typeof a === 'string' ? a : JSON.parse(JSON.stringify(a)) }catch(e){ return String(a) }
    })
    const entry = { ts: Date.now(), tag: 'GameContext', data: serializable }
    // always store logs locally (only when debug enabled)
    if (debugEnabled) appendLogEntry(entry)
    if (debugEnabled) console.log('[GameContext]', ...args)
  }

  // expose quick helpers on window when debugging enabled
  try{
    if (typeof window !== 'undefined' && debugEnabled){
      window.fmExportLogs = exportLogs
      window.fmGetLogs = getLogs
      window.fmClearLogs = clearLogs
    }
  }catch(e){}

  function addTransaction({amount, type='income', desc=''}){
    const t = { id: `t-${Date.now()}`, amount, type, desc, date: Date.now() }
    setTransactions(prev => [t, ...prev])
    setBalance(b => b + amount)
    return t
  }

  function addSponsorObject(sponsor){
    setSponsors(prev => [...prev, sponsor])
  }

  function createTeam(name){
    const newTeam = { name, id: Date.now() }
    // generate a roster of 18 players
    const players = Array.from({length:18}).map((_,i)=> makePlayer(`p-${Date.now()}-${i}`, i+1))
    setTeam(newTeam)
    setRoster(players)
    // initialize default formation
    const defaultFormation = '4-4-2'
    const rows = buildFormationRows(defaultFormation)
    const slots = {}
    rows.flat().forEach(sid => slots[sid] = null)
    setLineup(slots)
    setFormationRows(rows)
    setCurrentFormation(defaultFormation)
    // initialize stadium parts (30 empty parts)
    const parts = Array.from({length:30}).map(()=>({ built:false, type:null, offers:null, acceptedOffer:null }))
    setStadiumParts(parts)
    setStadiumLevel(1)
    setSponsors([])
    setJersey({ color: 'weiß' })
    try{ localStorage.setItem('fm_currentTeam', JSON.stringify(newTeam)) }catch(e){}
    // also persist initial state locally so refresh immediately restores roster
    try{
      const initState = { roster: players, lineup: slots, formationRows: rows, currentFormation: defaultFormation, stadiumParts: parts, sponsors: [], balance, transactions, jersey, stadiumEntryPrice, fanFriendship }
      localStorage.setItem(localStateKey(newTeam.id), JSON.stringify(initState))
      log('createTeam: persisted initState', newTeam.id, initState)
    }catch(e){}
    return newTeam
  }

  // restore team from localStorage if present
  useEffect(()=>{
    (async ()=>{
      try{
        // if user is authenticated, prefer server-side team load
        const authRaw = localStorage.getItem('fm_auth')
        if (authRaw){
          try{
            const auth = JSON.parse(authRaw)
            if (auth && auth.teamId){
              const headers = {}
              if (auth.token) headers['X-Auth-Token'] = auth.token
              const res = await fetch(`http://localhost:8080/api/teams/${auth.teamId}`, { headers })
              if (res.ok){
                const teamObj = await res.json()
                setTeam(teamObj)
                try{ localStorage.setItem('fm_currentTeam', JSON.stringify(teamObj)) }catch(e){}
                return
              }
            }
          }catch(e){ /* ignore */ }
        }
        // fallback: use fm_currentTeam if present
        const raw = localStorage.getItem('fm_currentTeam')
        if (!raw) return
        const t = JSON.parse(raw)
        if (t && t.id) setTeam(t)
      }catch(e){}
    })()
  }, [])

  // persist/load state to backend per team id
  async function saveStateToServer(teamId, state){
    try{
      log('saveStateToServer: attempt', teamId)
      const authRaw = localStorage.getItem('fm_auth')
      const token = authRaw ? JSON.parse(authRaw).token : null
      const headers = { 'Content-Type':'application/json' }
      if (token) headers['X-Auth-Token'] = token
      await fetch(`http://localhost:8080/api/teams/${teamId}/state`, {
        method: 'POST', headers, body: JSON.stringify(state)
      })
      log('saveStateToServer: done', teamId)
    }catch(e){ console.error('saveState failed', e) }
  }

  // localStorage key helper: use a stable local key when no teamId is present
  function localStateKey(teamId){
    return `fm_state_${teamId ? teamId : 'local'}`
  }

  async function loadStateFromServer(teamId){
    try{
      log('loadStateFromServer: attempt', teamId)
      const authRaw = localStorage.getItem('fm_auth')
      const token = authRaw ? JSON.parse(authRaw).token : null
      const headers = {}
      if (token) headers['X-Auth-Token'] = token
      const res = await fetch(`http://localhost:8080/api/teams/${teamId}/state`, { headers })
      if (!res.ok){ log('loadStateFromServer: no state on server', teamId, res.status); return null }
      const text = await res.text()
      log('loadStateFromServer: got', teamId, text && text.length)
      try { return JSON.parse(text) } catch(e) { return text }
    }catch(e){ console.error('loadState failed', e); return null }
  }

  // auto-save whenever important state changes (debounced)
  useEffect(()=>{
    if (!team || !team.id) return
    const timer = setTimeout(()=>{
      const state = { lineup, stadiumParts, sponsors, balance, transactions, jersey, currentFormation, formationRows, stadiumEntryPrice, fanFriendship, roster }
      // save to server (if authenticated) and to localStorage as fallback
      log('autosave: saving state', team.id, { lineup })
      saveStateToServer(team.id, state)
      try{ localStorage.setItem(localStateKey(team.id), JSON.stringify(state)); log('autosave: wrote local state', localStateKey(team.id)) }catch(e){ log('autosave: local write failed', e) }
    }, 600)
    return () => clearTimeout(timer)
  }, [team && team.id, lineup, stadiumParts, sponsors, balance, transactions, jersey, currentFormation, stadiumEntryPrice, fanFriendship, roster])

  // when a team is set, first apply local cached state immediately, then try to fetch server state and override
  useEffect(()=>{
    if (!team || !team.id) return
    // apply local immediately; prefer team-specific key, fall back to local key and migrate
    try{
      const teamKey = localStateKey(team.id)
      let raw = localStorage.getItem(teamKey)
      if (!raw){
        // try legacy/local key
        raw = localStorage.getItem(localStateKey(null))
        if (raw){
          log('migrating local state into team key', teamKey)
          // migrate to team-specific key for future loads
          try{ localStorage.setItem(teamKey, raw); localStorage.removeItem(localStateKey(null)) }catch(e){ log('migration failed', e) }
        }
      }
      if (raw){
        log('loading local cached state for team', team.id)
        const parsed = JSON.parse(raw)
        if (parsed.roster) setRoster(parsed.roster)
        if (parsed.lineup) setLineup(parsed.lineup)
        if (parsed.currentFormation) setFormation(parsed.currentFormation)
        if (parsed.formationRows) setFormation(parsed.formationRows)
        if (parsed.stadiumParts) setStadiumParts(parsed.stadiumParts)
        if (parsed.sponsors) setSponsors(parsed.sponsors)
        if (typeof parsed.balance === 'number') setBalance(parsed.balance)
        if (parsed.transactions) setTransactions(parsed.transactions)
        if (parsed.jersey) setJersey(parsed.jersey)
        if (parsed.stadiumEntryPrice) setStadiumEntryPrice(parsed.stadiumEntryPrice)
        if (typeof parsed.fanFriendship === 'number') setFanFriendship(parsed.fanFriendship)
      } else {
        log('no local cached state for team', team.id)
      }
    }catch(e){ log('error applying local cached state', e) }

    // then fetch server state and override if present
    (async ()=>{
      const s = await loadStateFromServer(team.id)
      if (!s) return
      if (s.roster) setRoster(s.roster)
      if (s.lineup) setLineup(s.lineup)
      if (s.currentFormation) setFormation(s.currentFormation)
      else if (s.formationRows) setFormationRows(s.formationRows)
      if (s.stadiumParts) setStadiumParts(s.stadiumParts)
      if (s.sponsors) setSponsors(s.sponsors)
      if (typeof s.balance === 'number') setBalance(s.balance)
      if (s.transactions) setTransactions(s.transactions)
      if (s.jersey) setJersey(s.jersey)
      if (s.stadiumEntryPrice) setStadiumEntryPrice(s.stadiumEntryPrice)
      if (typeof s.fanFriendship === 'number') setFanFriendship(s.fanFriendship)
      // if after server load roster is still empty, initialize default roster/state
      setTimeout(()=>{
        if ((!s.roster || s.roster.length === 0) && roster.length === 0){
          // initialize a default roster and lineup for this existing team
          const players = Array.from({length:18}).map((_,i)=> makePlayer(`p-${team.id}-${Date.now()}-${i}`, i+1))
          setRoster(players)
          const defaultFormation = '4-4-2'
          const rows = buildFormationRows(defaultFormation)
          const slots = {}
          rows.flat().forEach(sid => slots[sid] = null)
          setLineup(slots)
          setFormationRows(rows)
          setCurrentFormation(defaultFormation)
          const parts = Array.from({length:30}).map(()=>({ built:false, type:null, offers:null, acceptedOffer:null }))
          setStadiumParts(parts)
          setSponsors([])
          setJersey({ color: 'weiß' })
          // save newly created initial state to server and local
          const initState = { roster: players, lineup: slots, formationRows: rows, currentFormation: defaultFormation, stadiumParts: parts, sponsors: [], balance, transactions, jersey, stadiumEntryPrice, fanFriendship }
          saveStateToServer(team.id, initState)
          try{ localStorage.setItem(localStateKey(team.id), JSON.stringify(initState)); log('initialized empty state for existing team', team.id) }catch(e){ log('failed to persist init state', e) }
        }
      }, 50)
    })()
  }, [team && team.id])

  // formation helper
  const formations = {
    '4-4-2': [ ['GK'], ['D1','D2','D3','D4'], ['M1','M2','M3','M4'], ['F1','F2'] ],
    '4-3-3': [ ['GK'], ['D1','D2','D3','D4'], ['M1','M2','M3'], ['F1','F2','F3'] ],
    '3-5-2': [ ['GK'], ['D1','D2','D3'], ['M1','M2','M3','M4','M5'], ['F1','F2'] ],
  }

  function buildFormationRows(name){
    return formations[name] || formations['4-4-2']
  }

  function setFormation(name){
    const rows = buildFormationRows(name)
    const slots = {}
    rows.flat().forEach(sid => slots[sid] = null)
    setLineup(slots)
    setFormationRows(rows)
    setCurrentFormation(name)
  }

  function assignPlayerToSlot(slotId, playerId){
    setLineup(prev => {
      // remove player from any other slot
      const next = {...prev}
      Object.keys(next).forEach(k => { if (next[k] === playerId) next[k] = null })
      // assign
      next[slotId] = playerId

      // persist immediately (local + server)
        try{
          const state = { lineup: next, stadiumParts, sponsors, balance, transactions, jersey, currentFormation, formationRows, stadiumEntryPrice, fanFriendship, roster }
          if (team && team.id) saveStateToServer(team.id, state)
          // persist under a stable key: if team exists use team-specific key, otherwise use fm_state_local
          localStorage.setItem(localStateKey(team && team.id), JSON.stringify(state))
          log('assignPlayerToSlot', { teamId: team && team.id, slotId, playerId, prev, next })
        }catch(e){ log('assignPlayerToSlot: persist failed', e) }

      return next
    })
  }

  function swapSlots(slotA, slotB){
    setLineup(prev => {
      const next = {...prev}
      const a = next[slotA]
      const b = next[slotB]
      next[slotA] = b
      next[slotB] = a
       try{
         const state = { lineup: next, stadiumParts, sponsors, balance, transactions, jersey, currentFormation, formationRows, stadiumEntryPrice, fanFriendship, roster }
         if (team && team.id) saveStateToServer(team.id, state)
         localStorage.setItem(localStateKey(team && team.id), JSON.stringify(state))
         log('swapSlots', { teamId: team && team.id, slotA, slotB, prev, next })
       }catch(e){ log('swapSlots: persist failed', e) }
      return next
    })
  }

  function removePlayerFromSlot(slotId){
    setLineup(prev => {
      const next = {...prev, [slotId]: null}
       try{
         const state = { lineup: next, stadiumParts, sponsors, balance, transactions, jersey, currentFormation, formationRows, stadiumEntryPrice, fanFriendship, roster }
         if (team && team.id) saveStateToServer(team.id, state)
         localStorage.setItem(localStateKey(team && team.id), JSON.stringify(state))
         log('removePlayerFromSlot', { teamId: team && team.id, slotId, prev, next })
       }catch(e){ log('removePlayerFromSlot: persist failed', e) }
      return next
    })
  }

  function upgradeStadium(){
    setStadiumLevel(l=>l+1)
  }

  // stadium part management
  function setStadiumPart(index, type){
    setStadiumParts(prev => {
      const next = [...(prev||[])]
      const existing = next[index] || { built:false, type:null, offers:null, acceptedOffer:null, pendingType:null }
      // if offers exist, keep them, else leave offers as is
      next[index] = { ...existing, built:true, type, pendingType: null, acceptedOffer: null }
      return next
    })
  }

  function setPendingType(index, type){
    setStadiumParts(prev => {
      const next = [...(prev||[])]
      const existing = next[index] || { built:false, type:null, offers:null, acceptedOffer:null, pendingType:null }
      next[index] = { ...existing, pendingType: type }
      return next
    })
  }

  function ensureOffersFresh(index){
    const parts = stadiumParts || []
    const item = parts[index]
    const now = Date.now()
    const age = item && item.offersCreatedAt ? (now - item.offersCreatedAt) : Infinity
    if (!item || !item.offers || age > 24*60*60*1000) {
      // regenerate offers (preserve pendingType)
      generateOffersForPart(index)
    }
  }

  function removeStadiumPart(index){
    setStadiumParts(prev => {
      const next = [...(prev||[])]
      next[index] = { built:false, type:null, offers:null, acceptedOffer:null }
      return next
    })
  }

  // Offers and capacity
  function generateOffersForPart(index){
    setStadiumParts(prev => {
      const next = [...(prev||[])]
      const existing = next[index] || { built:false, type:null, offers:null, acceptedOffer:null, pendingType:null }
      // always (re)generate offers when requested
      // determine base price depending on desired type (standing=1x, seated=2x, vip=4x)
      const desiredType = existing.pendingType || existing.type || 'standing'
      const multiplier = desiredType === 'standing' ? 1 : desiredType === 'seated' ? 2 : 4
      const base = Math.round((5000 + (index % 5) * 1000 + Math.floor(Math.random()*3000)) * multiplier)
      const ts = Date.now()
      const offers = Array.from({length:3}).map((_,i)=>({ id: `o-${ts}-${i}`, price: Math.round(base * (1 + Math.random()*0.6)), proposer: `Baupartner ${i+1}`, createdAt: ts, tolerancePercent: 5 + Math.floor(Math.random()*11) }))
      next[index] = { ...existing, offers, acceptedOffer: null, offersCreatedAt: ts }
      return next
    })
  }

  function acceptOffer(index, offerId){
    setStadiumParts(prev => {
      const next = [...(prev||[])]
      const item = next[index] || { built:false, type:null, offers:null, acceptedOffer:null, pendingType:null }
      if (!item.offers) return next
      const offer = item.offers.find(o=>o.id===offerId)
      if (!offer) return next
      // accept and build the part using pendingType if set
      const builtType = item.pendingType || item.type || 'standing'
      next[index] = { ...item, offers: item.offers.map(o => ({...o, status: o.id===offerId ? 'accepted' : o.status})), acceptedOffer: offer, built:true, type: builtType }
      return next
    })
  }

  function rejectOffer(index, offerId){
    setStadiumParts(prev => {
      const next = [...(prev||[])]
      const item = next[index] || { built:false, type:null, offers:null, acceptedOffer:null }
      if (!item.offers) return next
      const offers = item.offers.filter(o => o.id !== offerId)
      next[index] = { ...item, offers }
      return next
    })
  }

  // negotiateOffer now accepts a user counterPrice. Logic:
  // - each offer has tolerancePercent. If counterPrice is >= price*(1 - tolerance), it's within acceptable range.
  //   - then seller may accept immediately (50% chance) or make a counteroffer (price between counterPrice and originalPrice).
  // - if counterPrice < threshold, seller rejects and the offer is removed.
  function negotiateOffer(index, offerId, counterPrice){
    setStadiumParts(prev => {
      const next = [...(prev||[])]
      const item = next[index] || { built:false, type:null, offers:null, acceptedOffer:null }
      if (!item.offers) return next
      const offers = item.offers.map(o => {
        if (o.id !== offerId) return o
        const threshold = Math.round(o.price * (1 - (o.tolerancePercent || 10)/100))
        if (counterPrice < threshold){
          // too low -> reject (remove)
          return null
        }
        // within acceptable range
        const acceptNow = Math.random() > 0.5 // 50% chance to accept immediately
        if (acceptNow){
          return { ...o, counter: { price: counterPrice, accepted: true }, status: 'accepted' }
        }
        // otherwise make a counteroffer between counterPrice and original price
        const counterPriceFromSeller = Math.round(counterPrice + Math.random() * (o.price - counterPrice))
        return { ...o, counter: { price: counterPriceFromSeller, accepted: false }, status: 'counter' }
      }).filter(Boolean)
      const acceptedOffer = offers.find(o => o.status === 'accepted') || item.acceptedOffer
      if (acceptedOffer){
        next[index] = { ...item, offers, acceptedOffer, built:true, type: item.pendingType || item.type || 'standing' }
      } else {
        next[index] = { ...item, offers, acceptedOffer }
      }
      return next
    })
  }

  function getStadiumCapacity(){
    const parts = stadiumParts || []
    const builtStanding = parts.filter(p => p && p.built && p.type === 'standing').length
    const builtSeated = parts.filter(p => p && p.built && p.type === 'seated').length
    const builtVip = parts.filter(p => p && p.built && p.type === 'vip').length
    const standingSeats = stadiumBaseCapacity + builtStanding * 1000
    const seatedSeats = builtSeated * 1000
    const vipSeats = builtVip * 1000
    return standingSeats + seatedSeats + vipSeats
  }

  function getStadiumSummary(){
    const parts = stadiumParts || []
    const builtStanding = parts.filter(p => p && p.built && p.type === 'standing').length
    const builtSeated = parts.filter(p => p && p.built && p.type === 'seated').length
    const builtVip = parts.filter(p => p && p.built && p.type === 'vip').length
    const standingSeats = stadiumBaseCapacity + builtStanding * 1000
    const seatedSeats = builtSeated * 1000
    const vipSeats = builtVip * 1000
    const added = (builtStanding + builtSeated + builtVip) * 1000
    return {
      base: stadiumBaseCapacity,
      added,
      total: standingSeats + seatedSeats + vipSeats,
      seats: { standing: standingSeats, seated: seatedSeats, vip: vipSeats },
      parts: { standing: builtStanding, seated: builtSeated, vip: builtVip },
      entryPrice: stadiumEntryPrice,
      fanFriendship
    }
  }

  const value = {
    team, roster, lineup, formationRows, currentFormation, stadiumLevel, stadiumParts, stadiumBaseCapacity, sponsors, jersey,
    createTeam, setFormation, assignPlayerToSlot, swapSlots, removePlayerFromSlot, upgradeStadium, setSponsors, setJersey,
    setStadiumPart, removeStadiumPart, getStadiumCapacity, getStadiumSummary,
    // offers API
    generateOffersForPart, acceptOffer, rejectOffer, negotiateOffer,
    // pending type and freshness
    setPendingType, ensureOffersFresh,
    // stadium economics
    stadiumEntryPrice, setStadiumEntryPrice, fanFriendship, setFanFriendship,
    // finances
    balance, transactions, addTransaction, addSponsorObject,
    // allow setting team from auth flow
    setTeam,
    // logging helpers
    getLogs, exportLogs, clearLogs
  }

  return <GameContext.Provider value={value}>{children}</GameContext.Provider>
}

export default GameContext
