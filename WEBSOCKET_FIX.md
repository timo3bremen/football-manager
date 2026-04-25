# WebSocket-Fix für Live-Simulation

## Problem
Fehler: `Uncaught ReferenceError: global is not defined` - Das Problem war, dass `sockjs-client` Node.js-Umgebungsvariablen brauchte, die in Vite nicht automatisch verfügbar sind.

## Lösung
✅ **Entfernung von SockJS**: Wir verwenden jetzt **native WebSockets** direkt, anstatt auf SockJS zurückzugreifen.

## Änderungen

### 1. Frontend (React) - LiveMatchSimulation.jsx
- ✅ Entfernt: `import SockJS from 'sockjs-client'`
- ✅ Geändert: WebSocket-Initialisierung von `new SockJS()` zu `new WebSocket()`
- ✅ Angepasst: Client-Konfiguration für direkte WebSocket-Nutzung

**Vorher:**
```javascript
const socket = new SockJS('http://localhost:8080/ws-live-match');
const client = new Client({
  webSocketFactory: () => socket,
  ...
});
```

**Nachher:**
```javascript
const socket = new WebSocket('ws://localhost:8080/ws-live-match');
const client = new Client({
  webSocket: socket,
  ...
});
```

### 2. Frontend - package.json
- ✅ Entfernt: `sockjs-client`
- ✅ Entfernt: `process` und `buffer` Polyfills (nicht mehr nötig)
- ✅ Hinzugefügt: `@vitejs/plugin-react`

### 3. Frontend - vite.config.js
- ✅ Erstellt: Neue Konfigurationsdatei
- ✅ Vereinfacht: Nur React Plugin, keine komplexen Polyfills nötig

### 4. Backend - WebSocketConfig.java
- ✅ Entfernt: `.withSockJS()`
- ✅ Behält native WebSocket-Unterstützung

## Installation & Test

### 1. Dependencies neu installieren
```bash
cd C:\Users\TimoH\eclipse-workspace\manager\frontend
rm -r node_modules package-lock.json  # Alte Dependencies löschen
npm install
```

### 2. Frontend neu starten
```bash
npm run dev
```

### 3. Backend muss laufen
Stelle sicher, dass die Spring Boot Anwendung läuft:
```bash
mvn spring-boot:run
```

## Browser-Kompatibilität
✅ **Alle modernen Browser** unterstützen native WebSockets:
- Chrome 16+
- Firefox 11+
- Safari 6+
- Edge (alle Versionen)
- Opera 11+
- Internet Explorer 10+

## Vorteile der Lösung
✅ Keine Node.js Module im Browser nötig
✅ Kleineres Bundle (kein sockjs-client)
✅ Schnellere WebSocket-Verbindung
✅ Einfachere Wartung und weniger Dependencies
✅ Bessere Kompatibilität mit Vite

## Falls noch Probleme auftreten

### CORS-Fehler
Stelle sicher, dass in WebSocketConfig alle Origins erlaubt sind:
```java
.setAllowedOriginPatterns("*")
```

### WebSocket-Port nicht erreichbar
- Prüfe ob Backend läuft: `http://localhost:8080`
- Prüfe Firewall-Einstellungen
- Stelle sicher dass der WebSocket-Port (Standard: 8080) offen ist

### Schnelle Test
Öffne die Browser-Konsole und versuche:
```javascript
const ws = new WebSocket('ws://localhost:8080/ws-live-match');
ws.onopen = () => console.log('✅ Verbunden');
ws.onerror = (e) => console.error('❌ Fehler:', e);
```

---

**Status**: ✅ Behoben
**Datum**: 2026-04-20
