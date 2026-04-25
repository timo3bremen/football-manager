# Manager App im Netzwerk - Lokale Einrichtung

## So kannst du die App auf deinem Handy im selben Netzwerk öffnen

### 1. **Deine lokale IP-Adresse finden**

Öffne die Eingabeaufforderung und führe aus:
```cmd
ipconfig
```

Suche nach "IPv4-Adresse" unter deinem aktiven Netzwerkadapter (z.B. **192.168.x.x**)

### 2. **Backend starten** (Terminal 1)

```cmd
cd C:\Users\TimoH\eclipse-workspace\manager
mvnw spring-boot:run
```

Das Backend läuft dann auf: `http://DEINE_IP:8080`

### 3. **Frontend starten** (Terminal 2)

```cmd
cd C:\Users\TimoH\eclipse-workspace\manager\frontend
npm install
npm run dev
```

Das Frontend läuft dann auf: `http://DEINE_IP:5173`

### 4. **Auf deinem Handy öffnen**

Gib in deinem Handy-Browser ein:
```
http://DEINE_IP:5173
```

Beispiel: `http://192.168.1.100:5173`

## Wichtige Punkte

- ✅ Backend und Frontend müssen beide laufen
- ✅ Handy und Computer müssen im selben WLAN sein
- ✅ Firewall könnte Port 5173 und 8080 blockieren (ggf. freigeben)
- ✅ Wenn du die IP änderst, musst du einen neuen Tab im Handy öffnen

## Troubleshooting

**Problem: Kann nicht auf die App zugreifen vom Handy?**
- Prüfe deine Firewall (Windows Defender)
- Starte die App nochmal neu
- Prüfe, ob beide im gleichen Netzwerk sind

**Problem: Frontend lädt aber zeigt Fehler?**
- Öffne Browser-Developer-Tools (F12)
- Schau in der Console nach Fehlern
- Prüfe, ob das Backend unter `http://DEINE_IP:8080` erreichbar ist
