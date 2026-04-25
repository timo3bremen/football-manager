# 🧪 Test-Anleitung - Live-Match-Simulation

## Vorbereitung

### 1. Frontend Dependencies installieren
```bash
cd C:\Users\TimoH\eclipse-workspace\manager\frontend
npm install
```

### 2. Backend starten
```bash
cd C:\Users\TimoH\eclipse-workspace\manager
mvn spring-boot:run
```

Backend läuft auf: `http://localhost:8080`

### 3. Frontend starten
```bash
cd C:\Users\TimoH\eclipse-workspace\manager\frontend
npm run dev
```

Frontend läuft auf: `http://localhost:5173` (oder ähnlich)

---

## 🧪 Komponenten-Tests

### Test 1: Authentifizierung
1. Browser öffnen: `http://localhost:5173`
2. **Erwartet**: Registrierungs/Anmelde-Screen
3. Team erstellen oder einloggen
4. **Überprüfen**: In Browser-Console sollte `fm_auth` in localStorage sein
   ```javascript
   localStorage.getItem('fm_auth')
   // Sollte zurückgeben: {"token":"...","teamId":...}
   ```

### Test 2: GameContext - Token verfügbar
1. Nach erfolgreichem Login zur Spiel-Seite navigieren
2. Browser-Console öffnen (F12)
3. Folgenden Code ausführen:
   ```javascript
   // Prüfe ob Token in Context ist
   console.log('Token sollte vorhanden sein')
   ```
4. **Überprüfen**: Keine Fehler in der Konsole

### Test 3: Live-Tab laden
1. Im Spiel zu "🔴 Live" Tab navigieren
2. **Erwartet**: 
   - Kein Fehler
   - Status-Anzeige mit "Gestoppt" (🔴 Gestoppt)
   - Start-Button "🎮 Simulation starten"
   - Info-Text

### Test 4: WebSocket-Verbindung
1. Im Live-Tab sich öffnen
2. Browser-Konsole öffnen (F12)
3. **Überprüfen**: Folgende Meldungen sollten sichtbar sein:
   ```
   ✅ WebSocket verbunden
   ```

### Test 5: Manuelle Simulation starten
1. Klick auf "🎮 Simulation starten"
2. **Erwartet**:
   - Alert: "Live-Simulation gestartet!"
   - Status ändert sich zu "🟢 Läuft"
   - Countdown läuft (270 Sekunden)
   - Events beginnen zu erscheinen

### Test 6: Events anzeigen
1. Während Simulation (nach Test 5)
2. **Erwartet**: Events erscheinen in Echtzeit
   - ⚽ Anpfiff
   - 💥 Chancen
   - ⚽ Tore
   - 🟨 Gelbe Karten
   - ⏸ Halbzeit (nach ~135 Sekunden)
   - ⚽ Weitere Events
   - 🏁 Abpfiff (nach 270 Sekunden)

### Test 7: Substitution Panel
1. Während Simulation navigiere zu Live-Tab
2. **Erwartet**: Substitution Panel sichtbar
   - Links: Aufstellung (Raus)
   - Rechts: Bank (Rein)
   - Spieler sind anwählbar

### Test 8: Spieler auswechseln
1. Im Substitution Panel:
   - Klick auf einen Spieler links (Aufstellung)
   - Klick auf einen Spieler rechts (Bank)
   - Klick auf "🔄 Auswechseln"
2. **Erwartet**:
   - Alert: "Spieler erfolgreich ausgewechselt!"
   - Event erscheint im Live-Feed
   - Spieler tauschen Positionen (lokal)

### Test 9: Nach Simulation
1. Nach 270 Sekunden
2. **Erwartet**:
   - Status: "🔴 Gestoppt"
   - Letztes Event: "🏁 Abpfiff: Team A 2 : 1 Team B"
   - Start-Button wieder aktiv

---

## 🔍 Fehlerbehandlung

### Problem: "WebSocket getrennt" 🔴
**Ursache**: Backend läuft nicht
**Lösung**: 
1. Terminal öffnen
2. `mvn spring-boot:run` ausführen
3. Warten bis "Started" Meldung

### Problem: "TypeError: Cannot read token"
**Ursache**: User nicht authentifiziert
**Lösung**:
1. Seite neu laden (F5)
2. Neu einloggen

### Problem: Keine Events sichtbar
**Ursache**: Simulation noch nicht gestartet
**Lösung**:
1. Prüfe Status (sollte 🟢 sein)
2. Klick "🎮 Simulation starten" falls nötig

### Problem: Substitution funktioniert nicht
**Ursache**: Simulation läuft nicht
**Lösung**:
1. Starte Simulation
2. Warte bis Events sichtbar sind
3. Dann Substitution versuchen

---

## 📊 Browser-Konsole Checks

Öffne die Browser-Konsole (F12) und überprüfe folgende Meldungen:

### ✅ Erfolgreich:
```
✅ WebSocket verbunden
[LiveMatchSimulation] Status geladen...
[LiveSimulation] Live-Simulation gestartet!
```

### ❌ Fehler:
```
❌ WebSocket getrennt
TypeError: Cannot set property...
ReferenceError: token is not defined
Uncaught ReferenceError: global is not defined
```

---

## 📱 Mobile Test

1. Öffne `http://localhost:5173` auf Mobile-Gerät (im gleichen Netzwerk)
2. IP-Adresse des PCs statt localhost
   - Beispiel: `http://192.168.1.100:5173`
3. Sollte genauso funktionieren wie Desktop

---

## ⏱️ Automatischer Scheduler-Test

1. Zeit auf 18:50 Uhr stellen (System-Uhr)
2. Warten bis 18:51
3. **Erwartet**: Simulation startet automatisch
   - Ohne Button-Klick
   - Automatisch um diese Zeit jeden Tag

Oder in `MatchdaySchedulerService.java` Cron-Expression ändern für schnelleren Test:
```java
@Scheduled(cron = "0 0 * * * ?") // Jede Stunde zur vollen Stunde
```

---

## 📚 Zusätzliche Infos

- **QUICK_START.md** - Schnelle Start-Anleitung
- **IMPLEMENTATION_SUMMARY.md** - Komplette Übersicht
- **WEBSOCKET_FIX.md** - WebSocket-Details
- **FIXES_APPLIED.md** - Alle Fehler die behoben wurden

---

**Viel Spaß beim Testen! 🎮⚽🔴**
