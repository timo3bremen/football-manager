import React, { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SubstitutionPanel from './SubstitutionPanel';

const API_BASE = 'http://192.168.178.21:8080'
const WS_BASE = 'ws://192.168.178.21:8080'

/**
 * Live-Match-Simulation Komponente
 * Zeigt Events in Echtzeit während der 270-sekündigen Simulation
 */
export default function LiveMatchSimulation({ token, teamId }) {
	const [events, setEvents] = useState([]);
	const [status, setStatus] = useState(null);
	const [connected, setConnected] = useState(false);
	const [selectedMatch, setSelectedMatch] = useState(null);
	const [matches, setMatches] = useState([]);
	const [userMatch, setUserMatch] = useState(null); // Match in dem der User spielt
	const [notification, setNotification] = useState(null);
	const [simulationStarted, setSimulationStarted] = useState(false); // Local flag
	
	const stompClientRef = useRef(null);
	const eventsEndRef = useRef(null);
	const eventsRef = useRef([]); // Persistent Events Storage
	
	// Initialisiere State aus SessionStorage beim Mount
	useEffect(() => {
		const savedEvents = sessionStorage.getItem('liveMatchEvents');
		const savedSimulationStarted = sessionStorage.getItem('simulationStarted');
		const savedStartTime = sessionStorage.getItem('simulationStartTime');
		const savedTeamId = sessionStorage.getItem('simulationTeamId');
		
		console.log('🔄 Lade State aus SessionStorage. TeamId:', teamId, 'SavedTeamId:', savedTeamId);
		
		if (savedEvents) {
			try {
				const events = JSON.parse(savedEvents);
				console.log('✅ Events geladen:', events.length, 'Events');
				eventsRef.current = events;
				setEvents(events);
			} catch (e) {
				console.error('❌ Fehler beim Laden von Events:', e);
			}
		} else {
			console.log('ℹ️ Keine Events in SessionStorage');
		}
		
		if (savedSimulationStarted === 'true') {
			console.log('✅ Simulation setze auf true');
			setSimulationStarted(true);
		}
		
		if (savedStartTime) {
			try {
				const startTime = new Date(savedStartTime);
				const now = new Date();
				const secondsElapsed = Math.floor((now - startTime) / 1000);
				
				// Prüfe ob noch innerhalb der 27 Sekunden (10x schneller)
				if (secondsElapsed <= 27) {
					const secondsRemaining = Math.max(0, 27 - secondsElapsed);
					const currentMinute = Math.floor((secondsElapsed / 0.3) % 91);
					
					setStatus({
						isRunning: true,
						running: true,
						currentMinute,
						secondsRemaining,
						startTime: startTime.toISOString(),
						expectedEndTime: new Date(startTime.getTime() + 27000).toISOString(),
						totalDurationSeconds: 27
					});
					console.log('✅ Status wiederhergestellt: Minute', currentMinute, 'von 90');
				} else {
					console.log('ℹ️ Simulation ist vorbei (>27 Sekunden verstrichen)');
					setSimulationStarted(false);
				}
			} catch (e) {
				console.error('❌ Fehler beim Laden des Startzeitpunkts:', e);
			}
		}
	}, []); // Nur beim Mount laden!
	
	// Prüfe wenn teamId sich ändert, ob es das gleiche Team ist
	useEffect(() => {
		const savedTeamId = sessionStorage.getItem('simulationTeamId');
		
		if (teamId && savedTeamId && String(teamId) !== String(savedTeamId)) {
			console.log('⚠️ Team gewechselt von', savedTeamId, 'zu', teamId, '- lösche alte Daten');
			sessionStorage.removeItem('liveMatchEvents');
			sessionStorage.removeItem('simulationStarted');
			sessionStorage.removeItem('simulationStartTime');
			sessionStorage.removeItem('simulationTeamId');
			
			// Lösche auch lokale State
			setEvents([]);
			setSimulationStarted(false);
			eventsRef.current = [];
		} else if (teamId) {
			// Aktualisiere gespeicherte TeamId
			sessionStorage.setItem('simulationTeamId', String(teamId));
		}
	}, [teamId]);
	
	// Auto-scroll zu neuesten Events
	const scrollToBottom = () => {
		eventsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
	};
	
	useEffect(() => {
		scrollToBottom();
	}, [events]);
	
	// Speichere Events in SessionStorage immer (nicht nur wenn > 0)
	useEffect(() => {
		sessionStorage.setItem('liveMatchEvents', JSON.stringify(events));
	}, [events]);
	
	// Speichere simulationStarted in SessionStorage
	useEffect(() => {
		sessionStorage.setItem('simulationStarted', simulationStarted.toString());
	}, [simulationStarted]);
	
	// WebSocket-Verbindung aufbauen
	useEffect(() => {
		const socket = new WebSocket(`${WS_BASE}/ws-live-match`);
		const client = new Client({
			brokerURL: `${WS_BASE}/ws-live-match`,
			reconnectDelay: 5000,
			heartbeatIncoming: 4000,
			heartbeatOutgoing: 4000,
			onConnect: () => {
				console.log('✅ WebSocket verbunden');
				setConnected(true);
				
				// Subscribe nur zu spezifischem Match (wenn ausgewählt)
				if (selectedMatch) {
					client.subscribe(`/topic/live-match/${selectedMatch}`, (message) => {
						const event = JSON.parse(message.body);
						
						// Prüfe ob es ein simulation_complete Event ist
						if (event.type === 'simulation_complete') {
							console.log('🏁 Simulation abgeschlossen, lade Daten neu...');
							
							// Lade Nachrichten aus der DB neu
							fetch(`${API_BASE}/api/v2/live-simulation/messages/${selectedMatch}`, {
								headers: { 'X-Auth-Token': token }
							})
							.then(res => res.json())
							.then(messages => {
								if (messages && messages.length > 0) {
									console.log('✅ ' + messages.length + ' Nachrichten nach Abschluss geladen');
									eventsRef.current = messages;
									setEvents(messages);
									sessionStorage.setItem('liveMatchEvents', JSON.stringify(messages));
								}
							})
							.catch(err => console.error('Fehler beim Nachladen:', err));
							
							// Setze simulationStarted auf false, OHNE Events zu löschen
							setSimulationStarted(false);
							sessionStorage.removeItem('simulationStarted');
							sessionStorage.removeItem('simulationStartTime');
							
							// Trigger teamUpdated event um Finanzen und Balance neu zu laden
							// Mehrfach mit Verzögerung, um sicherzustellen dass es ankommt
							console.log('💰 Triggere teamUpdated Event um Finanzen neu zu laden');
							window.dispatchEvent(new CustomEvent('teamUpdated'));
							
							// Nochmal nach 500ms
							setTimeout(() => {
								console.log('💰 Trigger teamUpdated erneut (500ms Verzögerung)');
								window.dispatchEvent(new CustomEvent('teamUpdated'));
							}, 500);
							
							// Und nochmal nach 1500ms zur Sicherheit
							setTimeout(() => {
								console.log('💰 Trigger teamUpdated final (1500ms Verzögerung)');
								window.dispatchEvent(new CustomEvent('teamUpdated'));
							}, 1500);
							
							return; // Nicht als normales Event behandeln
						}
						
						// Wenn match_start Event empfangen wird, starte frisch
						if (event.type === 'match_start') {
							console.log('🔔 Match Start empfangen, starte Event-Liste frisch');
							eventsRef.current = [event];
							setEvents([event]);
							return;
						}
						
						// Speichere in Ref um Events persistent zu halten
						eventsRef.current = [...eventsRef.current, event];
						setEvents([...eventsRef.current]);
					});
				}
			},
			onDisconnect: () => {
				console.log('❌ WebSocket getrennt');
				setConnected(false);
			},
			onStompError: (frame) => {
				console.error('STOMP Fehler:', frame);
			}
		});
		
		client.activate();
		stompClientRef.current = client;
		
		return () => {
			if (stompClientRef.current) {
				stompClientRef.current.deactivate();
			}
		};
	}, [selectedMatch, token]);
	
	// Lade Status alle 1 Sekunde (nur wenn Simulation läuft)
	useEffect(() => {
		const fetchStatus = async () => {
			if (!token) return;
			try {
				const res = await fetch(`${API_BASE}/api/v2/live-simulation/status`, {
					headers: { 'X-Auth-Token': token },
					cache: 'no-store'
				});
				const data = await res.json();
				console.log('📊 Status aktualisiert:', data);
				setStatus(data);
				
				// Prüfe ob Simulation beendet wurde - nutze data.running statt data.isRunning
				const isStillRunning = data.running === true || data.isRunning === true;
				
				console.log('🔍 Prüfe Simulation: isStillRunning=', isStillRunning, 'simulationStarted=', simulationStarted);
				
				// Wenn Simulation gerade beendet wurde, setze Flag zurück OHNE Events zu löschen
				if (simulationStarted && !isStillRunning) {
					console.log('⏹️ Simulation beendet, behalte Events...');
					setSimulationStarted(false);
					sessionStorage.removeItem('simulationStarted');
					sessionStorage.removeItem('simulationStartTime');
					// Events NICHT löschen - sie bleiben sichtbar!
				}
			} catch (err) {
				console.error('Fehler beim Laden des Status:', err);
			}
		};
		
		// Nur status laden wenn simulationStarted true ist
		if (simulationStarted) {
			console.log('▶️ Status-Poll gestartet');
			// Lade sofort beim Mount
			fetchStatus();
			
			// Dann alle 1 Sekunde aktualisieren
			const interval = setInterval(fetchStatus, 1000);
			
			return () => {
				console.log('⏸️ Status-Poll gestoppt');
				clearInterval(interval);
			};
		}
	}, [token, simulationStarted]);
	
	// Lade Matches des aktuellen Spieltags
	useEffect(() => {
		const fetchMatches = async () => {
			if (!token || !teamId) return;
			try {
				const authHeader = { 'X-Auth-Token': token };
				
				// Lade alle aktuellen Matchdays
				const res = await fetch(`${API_BASE}/api/v2/schedule/matchdays`, {
					headers: authHeader
				});
				const allMatchdays = await res.json();
				
				// Wenn Events bereits geladen sind, überspringe das Laden der Nachrichten von DB
				if (events.length > 0) {
					setMatches(allMatchdays.length > 0 ? allMatchdays : []);
					return;
				}
				
				// Finde aktuellen Spieltag von API
				const statusRes = await fetch(`${API_BASE}/api/v2/schedule/current-matchday`, {
					headers: authHeader
				});
				const statusData = await statusRes.json();
				const currentMatchday = statusData.currentMatchday;
				
				// Finde alle Matches des aktuellen Spieltags
				let allMatches = [];
				for (const md of allMatchdays) {
					if (md.dayNumber === currentMatchday && !md.isOffSeason) {
						allMatches = allMatches.concat(md.matches || []);
					}
				}
				
				// Lade Teamnamen für alle Matches
				for (const match of allMatches) {
					if (match.homeTeamId && !match.homeTeamName) {
						try {
							const homeRes = await fetch(`${API_BASE}/api/teams/${match.homeTeamId}`, {
								headers: authHeader
							});
							if (homeRes.ok) {
								const homeTeam = await homeRes.json();
								match.homeTeamName = homeTeam.name;
							}
						} catch (e) {
							console.error('Fehler beim Laden des Heimteams:', e);
						}
					}
					
					if (match.awayTeamId && !match.awayTeamName) {
						try {
							const awayRes = await fetch(`${API_BASE}/api/teams/${match.awayTeamId}`, {
								headers: authHeader
							});
							if (awayRes.ok) {
								const awayTeam = await awayRes.json();
								match.awayTeamName = awayTeam.name;
							}
						} catch (e) {
							console.error('Fehler beim Laden des Auswärtsteams:', e);
						}
					}
				}
				
				setMatches(allMatches);
				
				// Finde Match des Users (sein Team spielt)
				const userMatchData = allMatches.find(
					m => m.homeTeamId === teamId || m.awayTeamId === teamId
				);
				
				if (userMatchData) {
					setUserMatch(userMatchData);
					setSelectedMatch(userMatchData.id);
					console.log('User Match gefunden:', userMatchData);
				}
			} catch (err) {
				console.error('Fehler beim Laden der Matches:', err);
			}
		};
		
		// Lade immer Matches, auch wenn simulationStarted nicht gesetzt ist
		fetchMatches();
	}, [token, teamId]);
	
	// Lade gespeicherte Nachrichten von der DB wenn ein Match vorhanden ist
	useEffect(() => {
		const loadSavedMessages = async () => {
			if (!userMatch || !token) return;
			
			// Prüfe ob bereits Nachrichten geladen wurden (aber nicht aus SessionStorage)
			const hasLoadedFromDB = sessionStorage.getItem('messagesLoadedFromDB_' + userMatch.id);
			if (hasLoadedFromDB && events.length > 0) {
				console.log('ℹ️ Nachrichten bereits aus DB geladen');
				return;
			}
			
			try {
				console.log('🔄 Versuche gespeicherte Nachrichten für Match ' + userMatch.id + ' zu laden');
				const res = await fetch(`${API_BASE}/api/v2/live-simulation/messages/${userMatch.id}`, {
					headers: { 'X-Auth-Token': token }
				});
				
				if (res.ok) {
					const messages = await res.json();
					if (messages && messages.length > 0) {
						console.log('✅ ' + messages.length + ' gespeicherte Nachrichten geladen');
						eventsRef.current = messages;
						setEvents(messages);
						// Speichere auch in SessionStorage
						sessionStorage.setItem('liveMatchEvents', JSON.stringify(messages));
						sessionStorage.setItem('messagesLoadedFromDB_' + userMatch.id, 'true');
					} else {
						console.log('ℹ️ Keine gespeicherten Nachrichten gefunden');
					}
				}
			} catch (err) {
				console.error('Fehler beim Laden gespeicherter Nachrichten:', err);
			}
		};
		
		loadSavedMessages();
	}, [userMatch, token]);
	
	// Starte Simulation manuell
	const startSimulation = async () => {
		if (!token || !teamId) {
			setNotification('Nicht authentifiziert oder Team ID nicht vorhanden!');
			return;
		}
		try {
			const res = await fetch(`${API_BASE}/api/v2/live-simulation/start/${teamId}`, {
				method: 'POST',
				headers: { 'X-Auth-Token': token }
			});
			
			if (res.ok) {
				const data = await res.json();
				setNotification(data.message);
				
				// NICHT mehr Events löschen - das Backend löscht alte Nachrichten
				// Events werden über WebSocket frisch empfangen
				
				// Setze lokalen Flag und Status
				const startTime = new Date();
				setSimulationStarted(true);
				sessionStorage.setItem('simulationStarted', 'true');
				sessionStorage.setItem('simulationStartTime', startTime.toISOString());
				sessionStorage.setItem('simulationTeamId', String(teamId)); // Speichere Team ID
				
				setStatus({
					isRunning: true,
					currentMinute: 0,
					secondsRemaining: 27,
					startTime: startTime.toISOString(),
					expectedEndTime: new Date(startTime.getTime() + 27000).toISOString(),
					totalDurationSeconds: 27
				});
				
				// NICHT mehr manuell Anpfiff-Event erstellen - das Backend macht das und inkludiert Zuschauerzahl!
				// Events werden über WebSocket empfangen
				
				// Auto-clear notification nach 3 Sekunden
				setTimeout(() => setNotification(null), 3000);
			} else {
				const error = await res.json();
				setNotification('Fehler: ' + error.error);
				setTimeout(() => setNotification(null), 5000);
			}
		} catch (err) {
			console.error('Fehler beim Starten:', err);
			setNotification('Fehler beim Starten der Simulation!');
			setTimeout(() => setNotification(null), 5000);
		}
	};
	
	// Event-Icon basierend auf Typ
	const getEventIcon = (type) => {
		switch (type) {
			case 'goal': return '⚽';
			case 'chance': return '💥';
			case 'action': return '⚡'; // Allgemeine Spielaktionen
			case 'error': return '⚠️'; // Fehler
			case 'yellow_card': return '🟨';
			case 'red_card': return '🟥';
			case 'injury': return '🚑';
			case 'substitution': return '🔄';
			case 'match_start': return '🔔'; // Pfeife statt Ball
			case 'match_end': return '🏁';
			case 'halftime': return '⏸';
			default: return '📢';
		}
	};
	
	// Event-Farbe basierend auf Typ
	const getEventColor = (type) => {
		switch (type) {
			case 'goal': return '#4CAF50';
			case 'chance': return '#FFC107';
			case 'action': return '#2196F3'; // Blau für Aktionen
			case 'error': return '#FF9800'; // Orange für Fehler
			case 'yellow_card': return '#FFD700';
			case 'red_card': return '#F44336';
			case 'injury': return '#FF5722';
			case 'match_start': return '#2196F3';
			case 'match_end': return '#9C27B0';
			case 'halftime': return '#607D8B';
			default: return '#757575';
		}
	};
	
	return (
		<div style={styles.container}>
			<h2 style={styles.title}>⚽ Live-Spielsimulation</h2>
			
			{/* Notification */}
			{notification && (
				<div style={styles.notification}>
					{notification}
				</div>
			)}
			
			{/* Status-Anzeige */}
			<div style={styles.statusBar}>
				{/* Teamnamen und Spielstand - prominent */}
				{userMatch && (
					<div style={styles.scoreDisplay}>
						<strong>{userMatch.homeTeamName || 'Heimteam'}</strong>
						<span style={styles.score}>{events.length > 0 ? events[events.length - 1]?.homeGoals || 0 : 0} : {events.length > 0 ? events[events.length - 1]?.awayGoals || 0 : 0}</span>
						<strong>{userMatch.awayTeamName || 'Auswärtsteam'}</strong>
					</div>
				)}
				
				{/* WebSocket Status */}
				{simulationStarted && (
					<div style={styles.statusItem}>
						<strong>WebSocket:</strong>{' '}
						<span style={{ color: connected ? '#4CAF50' : '#F44336' }}>
							{connected ? '🟢 Verbunden' : '🔴 Getrennt'}
						</span>
					</div>
				)}
			</div>
			
			{/* Start-Button */}
			{!status?.isRunning && (
				<button 
					onClick={startSimulation} 
					style={styles.startButton}
					disabled={status?.isRunning}
				>
					🎮 Simulation starten
				</button>
			)}
			
			{/* Info-Text */}
			{!status?.isRunning && (
				<div style={styles.info}>
					<p>Die Simulation dauert 270 Sekunden (4,5 Minuten).</p>
					<p>Sie können auch später beitreten, wenn Sie sich während der Simulation einloggen.</p>
					<p>Die Simulation startet automatisch jeden Tag um 18:51 Uhr.</p>
				</div>
			)}
			
			{/* Events-Anzeige */}
			{events.length > 0 && (
				<div style={styles.eventsContainer}>
					<h3 style={styles.eventsTitle}>📢 Live-Events</h3>
					<div style={styles.eventsList}>
						{events.map((event, index) => (
							<div
								key={index}
								style={{
									...styles.eventItem,
									borderLeftColor: getEventColor(event.type)
								}}
							>
								<div style={styles.eventHeader}>
									<span style={styles.eventIcon}>{getEventIcon(event.type)}</span>
									<span style={styles.eventMinute}>{event.minute}'</span>
									{event.homeGoals !== null && event.awayGoals !== null && (
										<span style={styles.eventScore}>
											{event.homeGoals} : {event.awayGoals}
										</span>
									)}
								</div>
								<div style={styles.eventDescription}>{event.description}</div>
								{event.playerName && (
									<div style={styles.eventPlayer}>
										<strong>{event.playerName}</strong> ({event.teamName})
									</div>
								)}
							</div>
						))}
						<div ref={eventsEndRef} />
					</div>
				</div>
			)}
			
			{/* Hinweis wenn keine Events */}
			{events.length === 0 && status?.isRunning && (
				<div style={styles.waiting}>
					<p>⏳ Warte auf Events...</p>
				</div>
			)}
			
			{/* Substitution-Panel (nur wenn User an einem Match beteiligt ist und Simulation läuft/hat Events) */}
			{userMatch && (simulationStarted || events.length > 0) && (
				<SubstitutionPanel
					token={token}
					teamId={teamId}
					matchId={userMatch.id}
					isSimulationRunning={true}
				/>
			)}
		</div>
	);
}

const styles = {
	container: {
		padding: '20px',
		maxWidth: '900px',
		margin: '0 auto',
		fontFamily: 'Arial, sans-serif'
	},
	title: {
		textAlign: 'center',
		color: '#333',
		marginBottom: '20px'
	},
	statusBar: {
		display: 'flex',
		justifyContent: 'center',
		alignItems: 'center',
		flexWrap: 'wrap',
		background: '#f5f5f5',
		padding: '20px',
		borderRadius: '8px',
		marginBottom: '20px',
		gap: '20px'
	},
	scoreDisplay: {
		display: 'flex',
		alignItems: 'center',
		gap: '20px',
		fontSize: '24px',
		fontWeight: 'bold',
		color: '#333'
	},
	score: {
		fontSize: '32px',
		fontWeight: 'bold',
		color: '#4CAF50',
		minWidth: '80px',
		textAlign: 'center'
	},
	statusItem: {
		fontSize: '14px',
		color: '#555'
	},
	startButton: {
		display: 'block',
		margin: '20px auto',
		padding: '15px 30px',
		fontSize: '18px',
		fontWeight: 'bold',
		color: 'white',
		background: '#4CAF50',
		border: 'none',
		borderRadius: '8px',
		cursor: 'pointer',
		transition: 'background 0.3s'
	},
	info: {
		background: '#E3F2FD',
		padding: '15px',
		borderRadius: '8px',
		borderLeft: '4px solid #2196F3',
		marginBottom: '20px',
		fontSize: '14px',
		color: '#1565C0'
	},
	eventsContainer: {
		marginTop: '20px'
	},
	eventsTitle: {
		color: '#333',
		marginBottom: '15px'
	},
	eventsList: {
		maxHeight: '600px',
		overflowY: 'auto',
		background: 'white',
		border: '1px solid #ddd',
		borderRadius: '8px',
		padding: '10px'
	},
	eventItem: {
		padding: '12px',
		marginBottom: '10px',
		background: '#fafafa',
		borderLeft: '4px solid #333',
		borderRadius: '4px',
		transition: 'transform 0.2s',
		animation: 'slideIn 0.3s ease-out'
	},
	eventHeader: {
		display: 'flex',
		alignItems: 'center',
		gap: '10px',
		marginBottom: '5px',
		fontSize: '14px',
		fontWeight: 'bold'
	},
	eventIcon: {
		fontSize: '20px'
	},
	eventMinute: {
		color: '#666',
		fontSize: '13px'
	},
	eventScore: {
		marginLeft: 'auto',
		fontSize: '16px',
		fontWeight: 'bold',
		color: '#4CAF50'
	},
	eventDescription: {
		fontSize: '14px',
		color: '#333',
		marginBottom: '5px'
	},
	eventPlayer: {
		fontSize: '12px',
		color: '#666'
	},
	waiting: {
		textAlign: 'center',
		padding: '40px',
		color: '#999'
	},
	notification: {
		padding: '15px',
		marginBottom: '15px',
		background: '#4CAF50',
		color: 'white',
		borderRadius: '8px',
		textAlign: 'center',
		fontWeight: 'bold',
		animation: 'slideDown 0.3s ease-out'
	}
};