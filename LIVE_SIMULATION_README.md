# Live-Match-Simulation - Anleitung

## Übersicht

Die Live-Match-Simulation ermöglicht es Benutzern, Fußballspiele in Echtzeit zu verfolgen und während der Simulation Spieler auszuwechseln.

## Features

✅ **Live-Simulation**: 270 Sekunden Echtzeit (entspricht 90 Minuten Spielzeit, 20:1 Verhältnis)
✅ **Echtzeit-Events**: Tore, Chancen, Verletzungen, Gelbe/Rote Karten werden live angezeigt
✅ **WebSocket-Unterstützung**: Alle User sehen Events in Echtzeit
✅ **Spieler-Auswechslung**: User können während der Simulation Spieler austauschen
✅ **Automatischer Start**: Simulation startet täglich um 18:51 Uhr automatisch
✅ **Nachträglicher Beitritt**: User können auch später beitreten und die laufende Simulation verfolgen

## Installation

### Backend (Java/Spring Boot)

Das Backend ist bereits vollständig konfiguriert. Alle notwendigen Services, Controller und DTOs wurden erstellt.

**Wichtige Dateien:**
- `LiveMatchSimulationService.java` - Hauptlogik für die Simulation
- `LiveSimulationController.java` - REST-Endpoints
- `MatchdaySchedulerService.java` - Automatischer Scheduler
- `WebSocketConfig.java` - WebSocket-Konfiguration

### Frontend (React)

**1. Installiere npm-Pakete:**

```bash
cd C:\Users\TimoH\eclipse-workspace\manager\frontend
npm install
```

Dies installiert:
- sockjs-client (^1.6.1)
- @stomp/stompjs (^7.0.0)

**2. Starte Frontend-Entwicklungsserver:**

```bash
npm run dev
```

## Nutzung

### Für User

1. **Manueller Start** (nur zu Testzwecken):
   - Navigiere zum Tab "🔴 Live" im Hauptmenü
   - Klicke auf "🎮 Simulation starten"

2. **Automatischer Start** (Produktiv):
   - Die Simulation startet automatisch jeden Tag um 18:51 Uhr
   - User können sich zu jeder Zeit während der 270 Sekunden einloggen
   - Events werden in Echtzeit angezeigt

3. **Spieler auswechseln**:
   - Während der laufenden Simulation erscheint das "Substitution Panel"
   - Wähle einen Spieler aus der Aufstellung (links)
   - Wähle einen Spieler von der Bank (rechts)
   - Klicke auf "🔄 Auswechseln"
   - Die Auswechslung wird sofort durchgeführt und allen Usern angezeigt

### Events

Die Simulation generiert folgende Events:

- **⚽ Tore**: Basierend auf Teamstärke und Spielerpositionen
- **💥 Chancen**: 5-15 pro Team
- **🟨 Gelbe Karten**: 0-3 pro Match
- **🟥 Rote Karten**: Sehr selten (10% Chance)
- **🚑 Verletzungen**: 0-2 pro Match (30% Chance)
- **🔄 Auswechslungen**: Von Usern durchgeführt
- **⏸ Halbzeit**: Nach 45 Spielminuten (135 Sekunden)
- **🏁 Abpfiff**: Nach 90 Spielminuten (270 Sekunden)

## Technische Details

### Zeitberechnung

- **1 Spielminute = 3 Sekunden Echtzeit**
- **90 Spielminuten = 270 Sekunden = 4,5 Minuten Echtzeit**
- **Verhältnis: 20:1 (20 Minuten Ingame = 1 Minute Real)**

### WebSocket-Kommunikation

**Endpoints:**
- `/ws-live-match` - WebSocket-Verbindung
- `/topic/live-match/all` - Alle Events (alle Matches)
- `/topic/live-match/{matchId}` - Events eines spezifischen Matches

**REST-Endpoints:**
- `GET /api/v2/live-simulation/status` - Aktueller Status
- `POST /api/v2/live-simulation/start` - Manueller Start
- `POST /api/v2/live-simulation/substitute` - Spieler auswechseln

### Datenfluss

1. **Scheduler** startet Simulation um 18:51 Uhr
2. **LiveMatchSimulationService** generiert Event-Timeline für jedes Match
3. Events werden alle 3 Sekunden über **WebSocket** gesendet
4. **Frontend** empfängt Events und zeigt sie in Echtzeit an
5. User können **Substitutionen** durchführen
6. Nach 270 Sekunden werden **Ergebnisse gespeichert** und **Spieltag** wird erhöht

## Scheduler-Konfiguration

Der automatische Scheduler ist in `MatchdaySchedulerService.java` konfiguriert:

```java
@Scheduled(cron = "0 51 18 * * ?") // 18:51 Uhr jeden Tag
public void advanceMatchdayDaily() {
    liveSimulationService.startLiveSimulation();
}
```

**Cron-Expression ändern:**
- `0 51 18 * * ?` - 18:51 Uhr jeden Tag
- `0 0 20 * * ?` - 20:00 Uhr jeden Tag
- `0 30 15 * * ?` - 15:30 Uhr jeden Tag

## Troubleshooting

### WebSocket verbindet nicht
- Prüfe ob Backend läuft: `http://localhost:8080`
- Prüfe CORS-Einstellungen in `WebSocketConfig.java`
- Prüfe Browser-Konsole auf Fehler

### Events werden nicht angezeigt
- Prüfe ob Simulation läuft: `/api/v2/live-simulation/status`
- Prüfe WebSocket-Verbindung (grüner Punkt)
- Prüfe ob Matches für aktuellen Spieltag existieren

### Substitution funktioniert nicht
- Prüfe ob Simulation läuft (nur während aktiver Simulation möglich)
- Prüfe ob beide Spieler zum richtigen Team gehören
- Prüfe ob Spieler in Aufstellung ist (playerOut) und auf Bank (playerIn)

## Testing

**Manueller Test:**

1. Starte Backend: `mvn spring-boot:run`
2. Starte Frontend: `npm run dev`
3. Logge dich ein und navigiere zu "🔴 Live"
4. Klicke auf "🎮 Simulation starten"
5. Beobachte Events in Echtzeit
6. Teste Substitution während laufender Simulation

**Automatischer Test:**

1. Warte bis 18:51 Uhr (oder ändere Cron-Expression)
2. Simulation startet automatisch
3. Logge dich ein und beobachte Events

## Erweiterungsmöglichkeiten

- 🎥 **Video-Highlights**: Integration von generierten Highlight-Clips
- 📊 **Live-Statistiken**: Ballbesitz, Schüsse, Pässe in Echtzeit
- 💬 **Live-Chat**: User können während der Simulation chatten
- 🎮 **Taktik-Änderungen**: Formation während Simulation ändern
- 🏆 **Achievements**: Badges für besondere Events (z.B. Hattrick)
- 📱 **Push-Notifications**: Benachrichtigungen bei wichtigen Events

## Kontakt

Bei Fragen oder Problemen wenden Sie sich an den Entwickler.

---

**Version:** 1.0.0
**Datum:** 2026-04-20
