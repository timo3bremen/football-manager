FRONTEND-INTEGRATION ANLEITUNG: LIGA-SYSTEM
=============================================

## 1. REGISTRIERUNGSSEITE (Register.jsx)

### Schritt 1: Ligen laden beim Komponenten-Mount

```jsx
const [leagues, setLeagues] = useState([]);
const [selectedLeagueId, setSelectedLeagueId] = useState(null);

useEffect(() => {
  // Verfügbare Ligen abrufen
  fetch('/api/auth/leagues')
    .then(res => res.json())
    .then(data => {
      setLeagues(data);
      if (data.length > 0) {
        setSelectedLeagueId(data[0].id); // Standard: erste Liga
      }
    })
    .catch(err => console.error('Fehler beim Laden der Ligen:', err));
}, []);
```

### Schritt 2: Liga-Auswahl Dropdown anzeigen

```jsx
<div className="form-group">
  <label htmlFor="league">Liga wählen:</label>
  <select 
    id="league" 
    value={selectedLeagueId || ''} 
    onChange={(e) => setSelectedLeagueId(Number(e.target.value))}
  >
    <option value="">-- Liga wählen --</option>
    {leagues.map(league => (
      <option key={league.id} value={league.id}>
        {league.divisionLabel} 
        ({league.filledSlots}/{league.totalSlots} Teams)
      </option>
    ))}
  </select>
</div>
```

### Schritt 3: Registrierung mit Liga durchführen

```jsx
const handleRegister = async () => {
  if (!selectedLeagueId) {
    alert('Bitte wählen Sie eine Liga');
    return;
  }

  try {
    const response = await fetch('/api/auth/register-with-league', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username,
        password: password,
        teamName: teamName,
        leagueId: selectedLeagueId
      })
    });

    const data = await response.json();
    if (response.ok) {
      // Token speichern
      localStorage.setItem('token', data.token);
      localStorage.setItem('teamId', data.teamId);
      
      // Zu Home/Team-Seite navigieren
      window.location.href = '/home';
    } else {
      alert('Registrierung fehlgeschlagen: ' + data.message);
    }
  } catch (error) {
    console.error('Registrierungsfehler:', error);
    alert('Fehler bei der Registrierung');
  }
};
```

---

## 2. SPIELPLAN-SEITE (Schedule.jsx / Spielplan.jsx)

### Schritt 1: Ligen und aktuelle Liga state

```jsx
const [leagues, setLeagues] = useState([]);
const [currentLeagueId, setCurrentLeagueId] = useState(null);
const [standings, setStandings] = useState([]);

useEffect(() => {
  // Ligen abrufen
  fetch('/api/v2/schedule/leagues')
    .then(res => res.json())
    .then(data => {
      setLeagues(data);
      // Setze User-Liga als Standard (muss vom Backend mitgeteilt werden)
      // Für jetzt: erste Liga
      if (data.length > 0) {
        setCurrentLeagueId(data[0].id);
      }
    });
}, []);

useEffect(() => {
  if (currentLeagueId) {
    // Tabelle für aktuelle Liga abrufen
    fetch(`/api/v2/schedule/standings/league/${currentLeagueId}`)
      .then(res => res.json())
      .then(data => setStandings(data))
      .catch(err => console.error('Fehler beim Laden der Tabelle:', err));
  }
}, [currentLeagueId]);
```

### Schritt 2: Liga-Wechsel Dropdown anzeigen

```jsx
<div className="schedule-header">
  <h2>Spielplan & Tabelle</h2>
  
  <div className="league-selector">
    <label htmlFor="leagueSelect">Liga wechseln:</label>
    <select 
      id="leagueSelect"
      value={currentLeagueId || ''}
      onChange={(e) => setCurrentLeagueId(Number(e.target.value))}
    >
      <option value="">-- Liga wählen --</option>
      {leagues.map(league => (
        <option key={league.id} value={league.id}>
          {league.divisionLabel}
        </option>
      ))}
    </select>
  </div>
</div>
```

### Schritt 3: Tabelle rendern

```jsx
<div className="standings-table">
  <table>
    <thead>
      <tr>
        <th>Position</th>
        <th>Team</th>
        <th>Spiele</th>
        <th>Siege</th>
        <th>Unentschieden</th>
        <th>Niederlagen</th>
        <th>Tore</th>
        <th>Punkte</th>
        <th>Stärke</th>
      </tr>
    </thead>
    <tbody>
      {standings.map((entry, idx) => (
        <tr key={entry.teamId} className={entry.teamId === userTeamId ? 'user-team' : ''}>
          <td>{entry.position}</td>
          <td>{entry.teamName}</td>
          <td>{entry.played}</td>
          <td>{entry.won}</td>
          <td>{entry.drawn}</td>
          <td>{entry.lost}</td>
          <td>{entry.goalsFor} : {entry.goalsAgainst}</td>
          <td><strong>{entry.points}</strong></td>
          <td>{entry.strength}</td>
        </tr>
      ))}
    </tbody>
  </table>
</div>
```

### Schritt 4: CSS für Highlights

```css
.league-selector {
  margin: 20px 0;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 5px;
}

.league-selector label {
  margin-right: 10px;
  font-weight: bold;
}

.league-selector select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.standings-table table tr.user-team {
  background-color: #e3f2fd;
  font-weight: bold;
}

.standings-table table tr:hover {
  background-color: #f9f9f9;
}
```

---

## 3. LIGA-INFO ANZEIGE (Optional: GameContext.jsx)

Speichere User-Liga in GlobalContext:

```jsx
const [userLeagueId, setUserLeagueId] = useState(null);
const [userLeagueLabel, setUserLeagueLabel] = useState('');

// Nach erfolgreichem Login: User-Liga laden
const loadUserLeagueInfo = async () => {
  const leagueId = localStorage.getItem('userLeagueId');
  if (leagueId) {
    const leagues = await fetch('/api/v2/schedule/leagues').then(r => r.json());
    const userLeague = leagues.find(l => l.id === Number(leagueId));
    if (userLeague) {
      setUserLeagueId(userLeague.id);
      setUserLeagueLabel(userLeague.divisionLabel);
    }
  }
};
```

---

## 4. API-ANTWORT BEISPIELE

### GET /api/auth/leagues
```json
[
  {
    "id": 1,
    "name": "1. Liga",
    "division": 1,
    "divisionLabel": "1. Liga",
    "filledSlots": 12,
    "totalSlots": 12
  },
  {
    "id": 2,
    "name": "2. Liga A",
    "division": 2,
    "divisionLabel": "2. Liga A",
    "filledSlots": 11,
    "totalSlots": 12
  },
  ...
]
```

### GET /api/v2/schedule/standings/league/1
```json
[
  {
    "teamId": 5,
    "teamName": "Bayern München",
    "position": 1,
    "played": 10,
    "won": 8,
    "drawn": 1,
    "lost": 1,
    "goalsFor": 28,
    "goalsAgainst": 8,
    "points": 25,
    "strength": 445
  },
  ...
]
```

### POST /api/auth/register-with-league (Response)
```json
{
  "token": "uuid-token-here",
  "teamId": 42
}
```

---

## 5. FEHLERBEHANDLUNG

```jsx
const handleLeagueFetch = async () => {
  try {
    const response = await fetch('/api/v2/schedule/standings/league/' + leagueId);
    if (!response.ok) {
      if (response.status === 404) {
        console.error('Liga nicht gefunden');
      } else if (response.status === 500) {
        console.error('Server-Fehler');
      }
    }
    const data = await response.json();
    setStandings(data);
  } catch (error) {
    console.error('Netzwerkfehler:', error);
    // Fallback zur ersten Liga
    setCurrentLeagueId(leagues[0]?.id);
  }
};
```

---

## 6. TESTING

Verwende test_liga_api.sh für Backend-Tests oder:

```bash
# In Browser-Konsole testen:
fetch('/api/auth/leagues')
  .then(r => r.json())
  .then(d => console.log(d));

fetch('/api/v2/schedule/standings/league/1')
  .then(r => r.json())
  .then(d => console.log(d));
```

---

**Fertig! Frontend sollte jetzt vollständig mit dem Liga-System integriert sein.**
