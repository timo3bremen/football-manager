#!/bin/bash
# Test-Skript für Liga-System API

BASE_URL="http://localhost:8080"

echo "===== LIGA-SYSTEM API TESTS ====="
echo ""

# 1. Ligen abrufen
echo "1. Verfügbare Ligen abrufen:"
echo "GET /api/auth/leagues"
curl -s "$BASE_URL/api/auth/leagues" | jq '.' || echo "API nicht erreichbar"
echo ""
echo "---"
echo ""

# 2. Mit Liga registrieren
echo "2. Benutzer mit Liga registrieren:"
echo "POST /api/auth/register-with-league"
echo '{"username":"spieler1","password":"passwort123","teamName":"FC Meister","leagueId":1}'
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register-with-league" \
  -H "Content-Type: application/json" \
  -d '{"username":"spieler1","password":"passwort123","teamName":"FC Meister","leagueId":1}')
echo "$REGISTER_RESPONSE" | jq '.' || echo "$REGISTER_RESPONSE"
TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.token' 2>/dev/null)
TEAM_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.teamId' 2>/dev/null)
echo ""
echo "Token: $TOKEN"
echo "Team ID: $TEAM_ID"
echo ""
echo "---"
echo ""

# 3. Tabelle der Ligastelle 1 abrufen
echo "3. Ligatabelle von Liga 1 abrufen:"
echo "GET /api/v2/schedule/standings/league/1"
curl -s "$BASE_URL/api/v2/schedule/standings/league/1" | jq '.' | head -40
echo ""
echo "---"
echo ""

# 4. Tabelle von Liga 3 (3. Liga C) abrufen
echo "4. Ligatabelle von 3. Liga C abrufen:"
echo "GET /api/v2/schedule/standings/league/6"
curl -s "$BASE_URL/api/v2/schedule/standings/league/6" | jq '.' | head -40
echo ""
echo "---"
echo ""

# 5. User-Liga Tabelle abrufen (Standard)
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo "5. Tabelle der User-Liga abrufen (Standard):"
  echo "GET /api/v2/schedule/standings"
  echo "Header: X-Auth-Token: $TOKEN"
  curl -s -H "X-Auth-Token: $TOKEN" "$BASE_URL/api/v2/schedule/standings" | jq '.' | head -40
  echo ""
fi

echo ""
echo "===== TEST ABGESCHLOSSEN ====="
