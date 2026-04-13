# Liga-System Implementation - Zusammenfassung

## Übersicht der Änderungen

Diese Implementierung fügt ein umfassendes Liga-System mit 7 Ligen und CPU-Teams hinzu, wie gewünscht.

### 1. Modell-Änderungen

#### League.java
- **Neue Felder**: `division` (int), `divisionLabel` (String)
- Ermöglicht die Kennzeichnung von Ligen (1. Liga, 2. Liga A/B, 3. Liga A/B/C/D)

#### User.java
- **Neues Feld**: `leagueId` (Long)
- Speichert die Liga des Users
- Neuer Konstruktor: `User(username, passwordHash, teamId, leagueId)`

### 2. Neue DTOs

#### RegistrationRequest.java
- Erweitert die Registrierung um `leagueId`
- Ermöglicht die Ligawahl bei der Registrierung

#### LeagueInfoDTO.java
- Informationen über verfügbare Ligen
- Zeigt Division, Label, gefüllte und leere Slots

### 3. Backend-Logik (RepositoryService)

#### initializeLigues()
Erstellt beim Start 7 Ligen:
1. **1. Liga** (1x) - 12 CPU-Teams
2. **2. Liga A** (1x) - 12 CPU-Teams  
3. **2. Liga B** (1x) - 12 CPU-Teams
4. **3. Liga A** (1x) - 12 CPU-Teams
5. **3. Liga B** (1x) - 12 CPU-Teams
6. **3. Liga C** (1x) - 12 CPU-Teams
7. **3. Liga D** (1x) - 12 CPU-Teams

**Total**: 7 Ligen mit 84 CPU-Teams

#### Neue Methoden:
- `registerUserWithLeague(username, password, teamName, leagueId)`
  - Registriert User mit Ligawahl
  - Ersetzt einen zufälligen CPU-Team-Slot (Positions 1-12) mit dem neuen User-Team
  - Löscht das alte CPU-Team
  
- `getAvailableLeagues()`
  - Gibt alle Ligen mit Metadaten zurück (gefüllte/leere Slots)
  - Sortiert nach Division und Namen
  
- `getLeagueStandingsByLeagueId(Long leagueId)`
  - Gibt die Tabelle einer spezifischen Liga zurück
  - Berechnet Punkte, Tore, etc. nur für Teams dieser Liga

### 4. API-Endpoints

#### Authentication
- `POST /api/auth/register` - Alte Registrierung (ohne Liga-Wahl)
- `POST /api/auth/register-with-league` - Neue Registrierung mit Ligawahl
- `GET /api/auth/leagues` - Alle verfügbaren Ligen abrufen

#### Spielplan / Liga-Ansicht
- `GET /api/v2/schedule/leagues` - Alle Ligen abrufen
- `GET /api/v2/schedule/standings` - Tabelle der User-Liga (Standard)
- `GET /api/v2/schedule/standings/league/{leagueId}` - Tabelle einer spezifischen Liga

#### Admin
- `POST /api/admin/initialize-leagues` - Manuell Ligen initialisieren
- `DELETE /api/admin/clear-all` - Alle Ligen, Teams, Users löschen

### 5. Automatische Initialisierung

ApplicationStartupListener.java:
- Wird beim Server-Start ausgeführt
- Initialisiert automatisch die 7 Ligen mit CPU-Teams
- Zeigt Logs bei erfolgreicher Initialisierung

### 6. Frontend-Integration (Empfehlungen)

#### Registrierungsseite
1. Zeige Dropdown mit verfügbaren Ligen von `/api/auth/leagues`
2. User wählt Liga (z.B. "1. Liga" oder "3. Liga B")
3. Sende `POST /api/auth/register-with-league` mit:
   ```json
   {
     "username": "spieler1",
     "password": "passwort123",
     "teamName": "Mein Team",
     "leagueId": 1
   }
   ```

#### Spielplan-Seite (Schedule)
1. Zeige Standard die Tabelle der User-Liga
2. Dropdown für Liga-Wechsel (von `/api/v2/schedule/leagues`)
3. Bei Liga-Wechsel rufe `/api/v2/schedule/standings/league/{leagueId}` auf
4. Zeige Ligatabelle mit allen Teams der gewählten Liga

### 7. Workflow bei User-Registrierung

1. **Ligawahl angezeigt**: GET `/api/auth/leagues`
   ```json
   [
     { "id": 1, "name": "1. Liga", "division": 1, "divisionLabel": "1. Liga", "filledSlots": 12, "totalSlots": 12 },
     { "id": 2, "name": "2. Liga A", "division": 2, "divisionLabel": "2. Liga A", "filledSlots": 11, "totalSlots": 12 },
     ...
   ]
   ```

2. **User registriert sich**: POST `/api/auth/register-with-league`
   - User wählt z.B. "3. Liga C" (leagueId=6)
   - System sucht Liga 6
   - Findet zufälligen Slot mit CPU-Team (z.B. Slot 5)
   - Ersetzt CPU-Team durch User-Team
   - Löscht altes CPU-Team
   - User wird in Liga 6 eingefügt

3. **User sieht Spielplan**: GET `/api/v2/schedule/standings`
   - Zeigt Tabelle von Ligastelle (leagueId aus User-Entity)
   - User kann andere Ligen in Dropdown wählen
   - Beim Wechsel: GET `/api/v2/schedule/standings/league/{leagueId}`

### 8. Datenbank-Struktur

Neue Spalten:
- **LEAGUES.DIVISION** (INT)
- **LEAGUES.DIVISION_LABEL** (VARCHAR)
- **USERS.LEAGUE_ID** (LONG, Foreign Key)

### 9. Testing

Manual API-Test:
```bash
# Ligen anzeigen
curl http://localhost:8080/api/auth/leagues

# Mit Liga registrieren
curl -X POST http://localhost:8080/api/auth/register-with-league \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123","teamName":"Test FC","leagueId":1}'

# Ligatabelle anzeigen
curl http://localhost:8080/api/v2/schedule/standings/league/1
```

### 10. Feature-Beschreibung für User

✅ **7 Ligen verfügbar**
- 1. Liga (1x)
- 2. Liga (2x: A und B)
- 3. Liga (4x: A, B, C, D)

✅ **Registrierung mit Ligawahl**
- Beim Registrieren kann User Liga wählen
- Zufälliger CPU-Team-Slot wird durch User-Team ersetzt

✅ **Spielplan mit Liga-Ansicht**
- Standard: Eigene Liga wird angezeigt
- Dropdown zum Wechsel zu anderen Ligen
- Jede Liga mit eigener Tabelle und Spielplan

✅ **CPU-Teams**
- 84 CPU-Teams initial vorhanden (12 pro Liga)
- Spielen untereinander in ihren Ligen

---

**Implementiert durch Modell-, Service-, Controller- und DTO-Erweiterungen sowie automatische Initialisierung beim Server-Start.**
