# Quick Start - Live-Simulation Setup

## 1️⃣ Abhängigkeiten installieren

```bash
cd C:\Users\TimoH\eclipse-workspace\manager\frontend
npm install
```

## 2️⃣ Backend starten

```bash
cd C:\Users\TimoH\eclipse-workspace\manager
mvn spring-boot:run
```

Backend läuft auf: `http://localhost:8080`

## 3️⃣ Frontend starten

```bash
cd C:\Users\TimoH\eclipse-workspace\manager\frontend
npm run dev
```

Frontend läuft auf: `http://localhost:5173` (oder nächster Port)

## 4️⃣ Testen

1. Öffne Browser und gehe zu: `http://localhost:5173`
2. Logge dich ein oder erstelle ein Team
3. Gehe zu Spiel > "🔴 Live" Tab
4. Klicke auf "🎮 Simulation starten" (für manuellen Start)
5. Beobachte Events in Echtzeit

## 🚀 Automatischer Start

Die Simulation startet **automatisch jeden Tag um 18:51 Uhr**.

Um die Zeit zu ändern, bearbeite `MatchdaySchedulerService.java`:
```java
@Scheduled(cron = "0 51 18 * * ?") // Hier ändern
```

## 📱 Substitution

Während der laufenden Simulation:
1. Öffne das Substitution-Panel
2. Wähle Spieler aus Aufstellung (links)
3. Wähle Spieler von Bank (rechts)
4. Klicke "🔄 Auswechseln"

## ⚠️ Troubleshooting

### WebSocket-Fehler
```
❌ WebSocket getrennt
```
→ Backend läuft nicht. Starte Backend mit `mvn spring-boot:run`

### Events werden nicht angezeigt
→ Drücke F12, öffne Browser-Konsole und prüfe auf Fehler
→ Stelle sicher dass aktueller Spieltag nicht "Off-Season" ist

### Port bereits in Benutzung
→ Frontend: Vite zeigt nächsten verfügbaren Port an
→ Backend: Ändere port in `application.properties`

## 📚 Weitere Infos

- `LIVE_SIMULATION_README.md` - Ausführliche Dokumentation
- `WEBSOCKET_FIX.md` - WebSocket-Konfiguration
- `LiveMatchSimulation.jsx` - Frontend-Komponente
- `LiveMatchSimulationService.java` - Backend-Service

---

**Viel Spaß mit der Live-Simulation! ⚽🎮**
