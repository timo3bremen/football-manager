import React, { createContext, useContext, useState, useEffect } from 'react'

const GameContext = createContext(null)

// API URL configuration - can be changed for network access
const API_BASE = 'http://192.168.178.21:8080'

export function useGame(){
  return useContext(GameContext)
}

export function GameProvider({children}){
  const [team, setTeam] = useState(null)
  const [token, setToken] = useState(null)
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
  const [stadiumEntryPrice, setStadiumEntryPrice] = useState({ standing: 20, seated: 40, vip: 80 })
  const [fanFriendship, setFanFriendship] = useState(100) // percent
  const [sponsors, setSponsors] = useState([])
  const [jersey, setJersey] = useState({ color: 'weiß' })
  // finances
  const [balance, setBalance] = useState(100000)
  const [transactions, setTransactions] = useState([]) // recent txns
  // league and matchday
  const [currentMatchday, setCurrentMatchday] = useState(1)
  const [gameDay, setGameDay] = useState(0) // Inkrementiert bei jedem Spieltag-Wechsel
  const [season, setSeason] = useState(1)
  const [userLeagueId, setUserLeagueId] = useState(null)
  const [userLeagueLabel, setUserLeagueLabel] = useState('Meine Liga')

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
    // always store logs locally (persistent ringbuffer)
    appendLogEntry(entry)
    // print to console only when debug enabled
    if (debugEnabled) console.log('[GameContext]', ...args)
  }

  // expose quick helpers on window for convenience
  try{
    if (typeof window !== 'undefined'){
      window.fmExportLogs = exportLogs
      window.fmGetLogs = getLogs
      window.fmClearLogs = clearLogs
    }
  }catch(e){}

  function addTransaction({amount, type='income', desc='', category='other'}){
    const t = { id: `t-${Date.now()}`, amount, type, desc, category, date: Date.now() }
    setTransactions(prev => [t, ...prev])
    setBalance(b => b + amount)
    
    // Speichere zur DB
    if (team && team.id) {
      const authRaw = localStorage.getItem('fm_auth')
      const token = authRaw ? JSON.parse(authRaw).token : null
      const headers = {
        'Content-Type': 'application/json',
        ...(token && { 'X-Auth-Token': token })
      }
      
      fetch(`${API_BASE}/api/v2/transactions`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          teamId: team.id,
          amount,
          type,
          description: desc,
          category
        })
      }).catch(err => console.error('Error saving transaction:', err))
    }
    
    return t
  }

  function addSponsorObject(sponsor){
    setSponsors(prev => [...prev, sponsor])
  }

  function createTeam(name){
    const newTeam = { name, id: Date.now() }
    // Note: Don't generate roster here - it will be loaded from server
    setTeam(newTeam)
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
      const initState = { roster: [], lineup: slots, formationRows: rows, currentFormation: defaultFormation, stadiumParts: parts, sponsors: [], balance, transactions, jersey, stadiumEntryPrice, fanFriendship }
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
             if (auth && auth.token) {
               setToken(auth.token)
             }
              if (auth && auth.teamId){
                const headers = {}
                if (auth.token) headers['X-Auth-Token'] = auth.token
                const res = await fetch(`${API_BASE}/api/teams/${auth.teamId}`, { headers })
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

    // wenn Team sich ändert, lade die aktive Formation
    useEffect(()=>{
      if (!team || !team.id) return
      
      // Wenn das Team eine activeFormation hat, nutze diese
      if (team.activeFormation) {
        setFormation(team.activeFormation)
      } else {
        // Ansonsten nutze Standard 4-4-2
        setFormation('4-4-2')
      }
    }, [team && team.id])

    // Load transactions from DB when team changes
    useEffect(()=>{
      if (!team || !team.id) return
      
      (async ()=>{
        try {
          const authRaw = localStorage.getItem('fm_auth')
          const token = authRaw ? JSON.parse(authRaw).token : null
          const headers = {}
          if (token) headers['X-Auth-Token'] = token
          
          const res = await fetch(`${API_BASE}/api/v2/transactions/team/${team.id}`, { headers })
          if (res.ok) {
            const transactions = await res.json()
            setTransactions(transactions)
            log('Loaded transactions from DB:', transactions.length)
          }
        } catch (err) {
          console.error('Error loading transactions:', err)
        }
      })()
    }, [team && team.id])

    // if no team is set, try to restore a local unsaved state (fm_state_local)
  useEffect(()=>{
    try{
      const raw = localStorage.getItem(localStateKey(null))
      if (!raw) return
      const parsed = JSON.parse(raw)
      log('restoring local unsaved state on mount', parsed && Object.keys(parsed))
      if (parsed.roster) setRoster(parsed.roster)
      if (parsed.lineup) setLineup(parsed.lineup)
      if (parsed.currentFormation) setFormation(parsed.currentFormation)
      if (parsed.formationRows) setFormationRows(parsed.formationRows)
      if (parsed.stadiumParts) setStadiumParts(parsed.stadiumParts)
      if (parsed.sponsors) setSponsors(parsed.sponsors)
      if (typeof parsed.balance === 'number') setBalance(parsed.balance)
      if (parsed.transactions) setTransactions(parsed.transactions)
      if (parsed.jersey) setJersey(parsed.jersey)
      if (parsed.stadiumEntryPrice) setStadiumEntryPrice(parsed.stadiumEntryPrice)
      if (typeof parsed.fanFriendship === 'number') setFanFriendship(parsed.fanFriendship)
    }catch(e){ log('restore local unsaved state failed', e) }
  }, [])

  // persist lineup to backend
  async function saveLineupToDB(teamId, state){
    try{
      log('saveLineupToDB: attempt', teamId)
      
      if (state.lineup && state.currentFormation) {
        try {
          const lineupPayload = {
            currentFormation: state.currentFormation,
            lineup: state.lineup,
            formationRows: state.formationRows
          };
          
          const authRaw = localStorage.getItem('fm_auth')
          const token = authRaw ? JSON.parse(authRaw).token : null
          const headers = { 'Content-Type':'application/json' }
          if (token) headers['X-Auth-Token'] = token
          
           const lineupResponse = await fetch(`${API_BASE}/api/lineups/${teamId}/save`, {
             method: 'POST',
             headers,
             body: JSON.stringify(lineupPayload)
           });
          
          if (lineupResponse.ok) {
            const result = await lineupResponse.json();
            log('Lineup saved to DB:', result);
          } else {
            console.error('Failed to save lineup:', await lineupResponse.text());
          }
        } catch (lineupError) {
          console.error('Error saving lineup:', lineupError);
        }
      }
      
      log('saveLineupToDB: done', teamId)
    }catch(e){ console.error('saveLineupToDB failed', e) }
  }

  // localStorage key helper: use a stable local key when no teamId is present
  function localStateKey(teamId){
    return `fm_state_${teamId ? teamId : 'local'}`
  }

  async function loadLineupFromDB(teamId, formationId){
    try{
      log('loadLineupFromDB: attempt', teamId, formationId)
      const authRaw = localStorage.getItem('fm_auth')
      const token = authRaw ? JSON.parse(authRaw).token : null
      const headers = {}
      if (token) headers['X-Auth-Token'] = token
      
      // Lade die Lineup von der DB - das ist die Source of Truth
       // WICHTIG: formationId ist erforderlich!
       try {
         const lineupRes = await fetch(`${API_BASE}/api/lineups/${teamId}/${formationId}/map`, { headers })
        if (lineupRes.ok) {
          const lineupData = await lineupRes.json()
          log('loadLineupFromDB: loaded lineup from DB', teamId, formationId, lineupData)
          return lineupData
        } else {
          log('loadLineupFromDB: failed to load lineup from DB', lineupRes.status)
        }
      } catch (lineupError) {
        log('loadLineupFromDB: error loading lineup', lineupError.message)
      }
      
      return null
    }catch(e){ console.error('loadLineupFromDB failed', e); return null }
  }

  // auto-save whenever important state changes (debounced)
  useEffect(()=>{
    if (!team || !team.id) return
    const timer = setTimeout(()=>{
      const state = { lineup, stadiumParts, sponsors, balance, transactions, jersey, currentFormation, formationRows, stadiumEntryPrice, fanFriendship, roster }
      // save lineup to DB and to localStorage as fallback
      log('autosave: saving state', team.id, { lineup })
      saveLineupToDB(team.id, state)
      try{ localStorage.setItem(localStateKey(team.id), JSON.stringify(state)); log('autosave: wrote local state', localStateKey(team.id)) }catch(e){ log('autosave: local write failed', e) }
    }, 600)
    return () => clearTimeout(timer)
  }, [team && team.id, lineup, stadiumParts, sponsors, balance, transactions, jersey, currentFormation, stadiumEntryPrice, fanFriendship, roster])

   // when formation changes, reload lineup from DB
    useEffect(()=>{
      if (!team || !team.id || !currentFormation) return
      (async ()=>{
        const lineupData = await loadLineupFromDB(team.id, currentFormation)
        if (lineupData) {
          log('Formation changed: reloading lineup from DB', team.id, currentFormation, lineupData)
          setLineup(lineupData)
        } else {
          log('No lineup in DB for team', team.id, 'formation', currentFormation)
        }
      })()
    }, [team && team.id, currentFormation])

    // Handle matchdayChanged event (triggered after Spieltag-Wechsel)
    useEffect(()=>{
      const handleMatchdayChange = () => {
        log('matchdayChanged event received - new in-game day')
        
        // Erhöhe gameDay für neue "In-Game" Tagesfinanzen
        setGameDay(d => d + 1)
        // Speichere die aktuelle Zeit als letzter GameDay-Wechsel
        localStorage.setItem('fm_lastGameDayTime', Date.now().toString())
      }
      
      window.addEventListener('matchdayChanged', handleMatchdayChange)
      return () => window.removeEventListener('matchdayChanged', handleMatchdayChange)
    }, [])

    // Handle teamUpdated event (triggered after transfer market operations, match simulation, etc.)
    useEffect(()=>{
      const handleTeamUpdate = async () => {
        if (!team || !team.id) return
        log('teamUpdated event received - reloading team data')
        
        try {
          // Reload players from DB
          const authRaw = localStorage.getItem('fm_auth')
          const token = authRaw ? JSON.parse(authRaw).token : null
          const headers = {}
          if (token) headers['X-Auth-Token'] = token
          
            const playersRes = await fetch(`${API_BASE}/api/v2/players/team/${team.id}`, { headers })
          if (playersRes.ok) {
            const teamPlayers = await playersRes.json()
            log('Reloaded ' + teamPlayers.length + ' players from DB')
            setRoster(teamPlayers)
          }
          
          // Reload budget from server
            const teamRes = await fetch(`${API_BASE}/api/teams/${team.id}`, { headers })
          if (teamRes.ok) {
            const teamData = await teamRes.json()
            if (teamData && typeof teamData.budget === 'number') {
              log('Reloaded budget from server:', teamData.budget)
              setBalance(teamData.budget)
            }
            // Update team object with new capacities
            if (teamData) {
              setTeam(prev => ({
                ...prev,
                budget: teamData.budget,
                stadiumCapacity: teamData.stadiumCapacity,
                stadiumCapacityStanding: teamData.stadiumCapacityStanding,
                stadiumCapacitySeated: teamData.stadiumCapacitySeated,
                stadiumCapacityVip: teamData.stadiumCapacityVip,
                fanSatisfaction: teamData.fanSatisfaction,
                ticketPriceStanding: teamData.ticketPriceStanding,
                ticketPriceSeated: teamData.ticketPriceSeated,
                ticketPriceVip: teamData.ticketPriceVip
              }))
              log('Updated team object with stadium capacities:', teamData)
            }
          }
          
          // Reload lineup from DB
          const lineupData = await loadLineupFromDB(team.id, currentFormation)
          if (lineupData) {
            log('Reloaded lineup from DB', team.id, currentFormation, lineupData)
            setLineup(lineupData)
          }
          
          // Reload transactions from DB
          const transRes = await fetch(`${API_BASE}/api/v2/transactions/team/${team.id}`, { headers })
          if (transRes.ok) {
            const transactions = await transRes.json()
            log('Reloaded transactions from DB:', transactions.length)
            setTransactions(transactions)
          }
        } catch (e) {
          log('Error reloading team data:', e)
        }
      }
      
      window.addEventListener('teamUpdated', handleTeamUpdate)
      return () => window.removeEventListener('teamUpdated', handleTeamUpdate)
    }, [team && team.id, currentFormation])

    // ...existing code...
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
          // NICHT: if (parsed.lineup) setLineup(parsed.lineup)  - wir laden die Lineup von der DB!
          if (parsed.currentFormation) setFormation(parsed.currentFormation)
          if (parsed.formationRows) setFormationRows(parsed.formationRows)
          if (parsed.stadiumParts) setStadiumParts(parsed.stadiumParts)
          if (parsed.sponsors) setSponsors(parsed.sponsors)
          if (typeof parsed.balance === 'number') setBalance(parsed.balance)
          if (parsed.transactions) setTransactions(parsed.transactions)
          if (parsed.jersey) setJersey(parsed.jersey)
          if (parsed.stadiumEntryPrice) setStadiumEntryPrice(parsed.stadiumEntryPrice)
          if (typeof parsed.fanFriendship === 'number') setFanFriendship(parsed.fanFriendship)
        } else {
          log('no local cached state for team', team.id)
          // Clear roster when no local state is found
          setRoster([])
        }
       }catch(e){ log('error applying local cached state', e) }

       // then fetch lineup from DB and apply it
       (async ()=>{
         const lineupData = await loadLineupFromDB(team.id, currentFormation)
         if (lineupData) {
           log('Setting lineup from DB', team.id, currentFormation, lineupData)
           setLineup(lineupData)
         } else {
           log('No lineup in DB for team', team.id, 'formation', currentFormation)
         }
         
         // Always load players from DB for the current team
         try {
           log('Loading players from DB for team ' + team.id)
           const authRaw = localStorage.getItem('fm_auth')
           const token = authRaw ? JSON.parse(authRaw).token : null
           const headers = {}
           if (token) headers['X-Auth-Token'] = token
           
           const playersRes = await fetch(`${API_BASE}/api/v2/players/team/${team.id}`, { headers })
           if (playersRes.ok) {
             const teamPlayers = await playersRes.json()
             if (teamPlayers.length > 0) {
               log('Loaded ' + teamPlayers.length + ' players from DB')
               setRoster(teamPlayers)
             } else {
               log('No players found for team ' + team.id)
               setRoster([])
             }
           } else {
             log('Failed to load players: ' + playersRes.status)
             setRoster([])
           }
         } catch (e) {
           log('Failed to load players from DB:', e)
           setRoster([])
         }
         
         // Load current budget from server (override local value)
         try {
           log('Loading budget from server for team ' + team.id)
           const authRaw = localStorage.getItem('fm_auth')
           const token = authRaw ? JSON.parse(authRaw).token : null
           const headers = {}
           if (token) headers['X-Auth-Token'] = token
           
           const teamRes = await fetch(`${API_BASE}/api/teams/${team.id}`, { headers })
           if (teamRes.ok) {
             const teamData = await teamRes.json()
             if (teamData && typeof teamData.budget === 'number') {
               log('Loaded budget from server:', teamData.budget)
               setBalance(teamData.budget)
             }
           }
         } catch (e) {
           log('Failed to load budget from server:', e)
         }
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
    setFormationRows(rows)
    setCurrentFormation(name)
    
    // Speichere die aktive Formation auf dem Backend - DISABLED da Endpoint nicht existiert
    // if (team && team.id) {
    //   const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'
    //   const authRaw = localStorage.getItem('fm_auth')
    //   const token = authRaw ? JSON.parse(authRaw).token : null
    //   const headers = { 'Content-Type': 'application/json' }
    //   if (token) headers['X-Auth-Token'] = token
    //   
    //   fetch(`${API_BASE}/api/teams/${team.id}`, {
    //     method: 'PUT',
    //     headers,
    //     body: JSON.stringify({ ...team, activeFormation: name })
    //   }).catch(e => console.error('Fehler beim Speichern der Formation:', e))
    // }
  }

  function assignPlayerToSlot(slotId, playerId){
    setLineup(prev => {
      // remove player from any other slot
      const next = {...prev}
      Object.keys(next).forEach(k => { if (next[k] === playerId) next[k] = null })
      // assign
      next[slotId] = playerId

      // Nicht hier speichern! Der autosave Effect kümmert sich darum
      log('assignPlayerToSlot', { teamId: team && team.id, slotId, playerId, prev, next })

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
      // Nicht hier speichern! Der autosave Effect kümmert sich darum
      log('swapSlots', { teamId: team && team.id, slotA, slotB, prev, next })
      return next
    })
  }

  function removePlayerFromSlot(slotId){
    setLineup(prev => {
      const next = {...prev, [slotId]: null}
      // Nicht hier speichern! Der autosave Effect kümmert sich darum
      log('removePlayerFromSlot', { teamId: team && team.id, slotId, prev, next })
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
    // Nutze stadiumCapacity vom Team wenn vorhanden
    // Diese wird vom Backend beim Stadionausbau aktualisiert
    let standingTotal = 1000
    let seatedTotal = 0
    let vipTotal = 0
    
    if (team) {
      standingTotal = team.stadiumCapacityStanding || team.stadiumCapacity || 1000
      seatedTotal = team.stadiumCapacitySeated || 0
      vipTotal = team.stadiumCapacityVip || 0
    }
    
    const total = standingTotal + seatedTotal + vipTotal
    
    return {
      base: 1000,
      added: total - 1000,
      total: total,
      seats: { standing: standingTotal, seated: seatedTotal, vip: vipTotal },
      parts: { 
        standing: Math.ceil(standingTotal / 1000), 
        seated: Math.ceil(seatedTotal / 1000), 
        vip: Math.ceil(vipTotal / 1000) 
      },
      entryPrice: stadiumEntryPrice,
      fanFriendship
    }
  }

   const value = {
     team, token, roster, lineup, formationRows, currentFormation, stadiumLevel, stadiumParts, stadiumBaseCapacity, sponsors, jersey,
     createTeam, setFormation, assignPlayerToSlot, swapSlots, removePlayerFromSlot, upgradeStadium, setSponsors, setJersey,
     setStadiumPart, removeStadiumPart, getStadiumCapacity, getStadiumSummary,
     // ...existing code...
     // offers API
     generateOffersForPart, acceptOffer, rejectOffer, negotiateOffer,
     // pending type and freshness
     setPendingType, ensureOffersFresh,
     // stadium economics
     stadiumEntryPrice, setStadiumEntryPrice, fanFriendship, setFanFriendship,
     // finances
     balance, transactions, addTransaction, addSponsorObject,
      // league and matchday
      currentMatchday, setCurrentMatchday, gameDay, season, setSeason,
     // NEW: User-Liga
     userLeagueId, setUserLeagueId, userLeagueLabel, setUserLeagueLabel,
     // allow setting team from auth flow
     setTeam,
     // logging helpers
     getLogs, exportLogs, clearLogs
   }

  return <GameContext.Provider value={value}>{children}</GameContext.Provider>
}

export default GameContext