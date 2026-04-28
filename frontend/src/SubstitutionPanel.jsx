import React, { useState, useEffect, useRef } from 'react';

/**
 * Substitution-Panel für Live-Match
 * Erlaubt User, Spieler während der Simulation auszuwechseln
 */
const API_BASE = 'http://192.168.178.21:8080';

export default function SubstitutionPanel({ token, teamId, matchId, isSimulationRunning }) {
	const [lineup, setLineup] = useState([]);
	const [bench, setBench] = useState([]);
	const [selectedOut, setSelectedOut] = useState(null);
	const [selectedIn, setSelectedIn] = useState(null);
	const [substituting, setSubstituting] = useState(false);
	const refreshIntervalRef = useRef(null);
	
	// Haupt-Funktion zum Laden der Team-Daten
	const fetchTeamData = async (isRefresh = false) => {
		if (!token || !teamId) return;
		try {
			const authHeader = { 'X-Auth-Token': token };
			
			// Lade Team-Info um ActiveFormation zu erhalten
			const teamRes = await fetch(`${API_BASE}/api/teams/${teamId}`, {
				headers: authHeader
			});
			if (!teamRes.ok) {
				throw new Error(`Team nicht gefunden: ${teamRes.status}`);
			}
			const teamData = await teamRes.json();
			const formation = teamData.activeFormation || '4-4-2';
			
			// Lade alle Spieler des Teams
			const playersRes = await fetch(`${API_BASE}/api/v2/players/team/${teamId}`, {
				headers: authHeader
			});
			if (!playersRes.ok) {
				throw new Error(`Spieler nicht gefunden: ${playersRes.status}`);
			}
			const allPlayersData = await playersRes.json();
			
			if (!allPlayersData || allPlayersData.length === 0) {
				console.warn('Keine Spieler für Team gefunden');
				setLineup([]);
				setBench([]);
				return;
			}
			
			// Versuche echte Lineup-Daten vom Backend zu laden
			try {
				const lineupRes = await fetch(
					`${API_BASE}/api/lineups/${teamId}/${formation}`,
					{ headers: authHeader }
				);
				
				if (lineupRes.ok) {
					const lineupSlots = await lineupRes.json();
					
					// Sammle Spieler-IDs aus Lineup-Slots
					const lineupPlayerIds = new Set();
					if (Array.isArray(lineupSlots)) {
						lineupSlots.forEach(slot => {
							if (slot.playerId) {
								lineupPlayerIds.add(slot.playerId);
							}
						});
					} else if (typeof lineupSlots === 'object') {
						// Wenn es ein Map-Objekt ist
						Object.values(lineupSlots).forEach(playerId => {
							if (playerId) {
								lineupPlayerIds.add(playerId);
							}
						});
					}
					
					// Teile Spieler nach Lineup-Zugehörigkeit
					const lineupPlayers = [];
					const benchPlayers = [];
					
					allPlayersData.forEach(player => {
						if (lineupPlayerIds.has(player.id)) {
							lineupPlayers.push(player);
						} else {
							benchPlayers.push(player);
						}
					});
					
					// Sortiere beide Listen nach Position
					const sortByPosition = (players) => {
						const positionOrder = { 'GK': 0, 'DEF': 1, 'MID': 2, 'FWD': 3 };
						return players.sort((a, b) => {
							const posA = positionOrder[a.position] || 999;
							const posB = positionOrder[b.position] || 999;
							return posA - posB;
						});
					};
					
					setLineup(sortByPosition(lineupPlayers));
					setBench(sortByPosition(benchPlayers));
					if (!isRefresh) {
						console.log('Echte Lineup und Bank geladen:', { 
							formation,
							lineupCount: lineupPlayers.length,
							benchCount: benchPlayers.length
						});
					}
					return;
				}
			} catch (e) {
				console.warn('Könnte echte Lineup nicht laden, nutze Fallback:', e);
			}
			
			// Fallback: Sortiere nach Position wenn echte Lineup nicht verfügbar
			const positionOrder = { 'GK': 0, 'DEF': 1, 'MID': 2, 'FWD': 3 };
			const sortedPlayers = [...allPlayersData].sort((a, b) => {
				const posA = positionOrder[a.position] || 999;
				const posB = positionOrder[b.position] || 999;
				return posA - posB;
			});
			
			const lineupPlayers = sortedPlayers.slice(0, 11);
			const benchPlayers = sortedPlayers.slice(11);
			
			setLineup(lineupPlayers);
			setBench(benchPlayers);
			if (!isRefresh) {
				console.log('Fallback: Lineup und Bank geladen nach Position');
			}
		} catch (err) {
			console.error('Fehler beim Laden der Team-Daten:', err);
			setLineup([]);
			setBench([]);
		}
	};
	
	// Initialisiere Team-Daten beim Mount
	useEffect(() => {
		fetchTeamData(false);
	}, [teamId, token]);
	
	// Auto-Refresh der Frische während Simulation läuft
	useEffect(() => {
		if (isSimulationRunning) {
			// Aktualisiere Frische alle 3 Sekunden (= 1 Spielminute)
			refreshIntervalRef.current = setInterval(() => {
				fetchTeamData(true); // true = ist ein Refresh, keine Logs
			}, 3000);
			
			return () => {
				if (refreshIntervalRef.current) {
					clearInterval(refreshIntervalRef.current);
				}
			};
		}
	}, [isSimulationRunning, token, teamId]);
	
	// Filtere Bank um ausgewechselte Spieler zu verstecken
	const getAvailableBench = () => {
		if (!selectedOut || bench.length === 0) return bench;
		
		const selectedOutPlayer = lineup.find(p => p.id === selectedOut);
		if (!selectedOutPlayer) return bench;
		
		// Filtere Bank: nur Spieler mit gleicher Position anzeigen
		return bench.filter(p => p.position === selectedOutPlayer.position);
	};
	
	// Filtere Aufstellung um nur Spieler mit gleicher Position wie Bank-Spieler anzuzeigen
	const getAvailableLineup = () => {
		if (!selectedIn || lineup.length === 0) return lineup;
		
		const selectedInPlayer = bench.find(p => p.id === selectedIn);
		if (!selectedInPlayer) return lineup;
		
		// Filtere Lineup: nur Spieler mit gleicher Position anzeigen
		return lineup.filter(p => p.position === selectedInPlayer.position);
	};
	
	// Auswechslung durchführen
	const performSubstitution = async () => {
		if (!selectedOut || !selectedIn) {
			console.warn('Bitte wählen Sie beide Spieler aus!');
			return;
		}
		
		if (!token || !matchId) {
			console.warn('Token oder Match ID fehlt!');
			return;
		}
		
		setSubstituting(true);
		
		try {
			const res = await fetch(`${API_BASE}/api/v2/live-simulation/substitute`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'X-Auth-Token': token
				},
				body: JSON.stringify({
					matchId,
					teamId,
					playerOutId: selectedOut,
					playerInId: selectedIn
				})
			});
			
			if (res.ok) {
				const data = await res.json();
				console.log('Substitution erfolgreich:', data.message);
				
				// Aktualisiere Lineup und Bank lokal
				const playerOut = lineup.find(p => p.id === selectedOut);
				const playerIn = bench.find(p => p.id === selectedIn);
				
				if (playerOut && playerIn) {
					setLineup(prev => prev.map(p => (p.id === selectedOut ? playerIn : p)));
					setBench(prev => [...prev.filter(p => p.id !== selectedIn), playerOut]);
					
					setSelectedOut(null);
					setSelectedIn(null);
				}
			} else {
				const error = await res.json();
				console.error('Substitution Fehler:', error.error);
			}
		} catch (err) {
			console.error('Fehler bei Substitution:', err);
		} finally {
			setSubstituting(false);
		}
	};
	
	// Toggle Selection - klick auf gleichen Spieler deselektiert ihn
	const toggleSelectOut = (playerId) => {
		if (selectedOut === playerId) {
			setSelectedOut(null);
		} else {
			setSelectedOut(playerId);
		}
	};
	
	const toggleSelectIn = (playerId) => {
		if (selectedIn === playerId) {
			setSelectedIn(null);
		} else {
			setSelectedIn(playerId);
		}
	};
	
	// Bestimme Fitness-Farbe basierend auf Fitness-Wert
	const getFitnessColor = (fitness) => {
		if (fitness < 20) return '#F44336'; // Rot
		if (fitness < 60) return '#FFC107'; // Gelb
		return '#4CAF50'; // Grün
	};
	
	if (!isSimulationRunning) {
		return (
			<div style={styles.disabledPanel}>
				<p>⏸ Substitutionen sind nur während einer laufenden Simulation möglich.</p>
			</div>
		);
	}
	
	return (
		<div style={styles.container}>
			<h3 style={styles.title}>🔄 Spieler-Auswechslung</h3>
			
			<div style={styles.columns}>
				{/* Lineup */}
				<div style={styles.column}>
					<h4 style={styles.columnTitle}>Aufstellung (Raus)</h4>
					<div style={styles.playerList}>
						{(selectedIn ? getAvailableLineup() : lineup).map(player => (
							<div
								key={player.id}
								onClick={() => toggleSelectOut(player.id)}
								style={{
									...styles.playerCard,
									...(selectedOut === player.id ? styles.selectedCard : {})
								}}
							>
								<div style={styles.playerName}>{player.name}</div>
								<div style={styles.playerInfo}>
									{player.position} | Rating: {player.rating}
								</div>
								<div style={{ ...styles.playerFresh, color: getFitnessColor(player.fitness !== undefined ? player.fitness : 100) }}>
									💪 Frische: {player.fitness !== undefined ? player.fitness : 100}
							</div>
						</div>
					))}
					</div>
				</div>
				
				{/* Bank */}
				<div style={styles.column}>
					<h4 style={styles.columnTitle}>Bank (Rein)</h4>
					<div style={styles.playerList}>
						{(selectedOut ? getAvailableBench() : bench).map(player => (
							<div
								key={player.id}
								onClick={() => toggleSelectIn(player.id)}
								style={{
									...styles.playerCard,
									...(selectedIn === player.id ? styles.selectedCard : {})
								}}
							>
								<div style={styles.playerName}>{player.name}</div>
								<div style={styles.playerInfo}>
									{player.position} | Rating: {player.rating}
								</div>
								<div style={{ ...styles.playerFresh, color: getFitnessColor(player.fitness !== undefined ? player.fitness : 100) }}>
									💪 Frische: {player.fitness !== undefined ? player.fitness : 100}
							</div>
						</div>
					))}
					</div>
				</div>
			</div>
			
			{/* Auswechsel-Button */}
			<button
				onClick={performSubstitution}
				disabled={!selectedOut || !selectedIn || substituting}
				style={{
					...styles.substituteButton,
					...((!selectedOut || !selectedIn || substituting) ? styles.disabledButton : {})
				}}
			>
				{substituting ? '⏳ Wechsle aus...' : '🔄 Auswechseln'}
			</button>
			
			{selectedOut && selectedIn && (
				<div style={styles.preview}>
					<p>
						<strong>Auswechslung:</strong>{' '}
						{lineup.find(p => p.id === selectedOut)?.name} ➡️{' '}
						{bench.find(p => p.id === selectedIn)?.name}
					</p>
				</div>
			)}
		</div>
	);
}

const styles = {
	container: {
		padding: '20px',
		background: 'white',
		borderRadius: '8px',
		boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
		marginTop: '20px'
	},
	disabledPanel: {
		padding: '20px',
		background: '#f5f5f5',
		borderRadius: '8px',
		textAlign: 'center',
		color: '#999',
		marginTop: '20px'
	},
	title: {
		textAlign: 'center',
		color: '#333',
		marginBottom: '20px'
	},
	columns: {
		display: 'grid',
		gridTemplateColumns: '1fr 1fr',
		gap: '20px',
		marginBottom: '20px'
	},
	column: {
		border: '1px solid #ddd',
		borderRadius: '8px',
		padding: '15px',
		background: '#fafafa'
	},
	columnTitle: {
		textAlign: 'center',
		color: '#555',
		marginBottom: '15px',
		fontSize: '14px',
		fontWeight: 'bold'
	},
	playerList: {
		maxHeight: '300px',
		overflowY: 'auto'
	},
	playerCard: {
		padding: '10px',
		marginBottom: '10px',
		background: 'white',
		border: '2px solid transparent',
		borderRadius: '6px',
		cursor: 'pointer',
		transition: 'all 0.2s'
	},
	selectedCard: {
		borderColor: '#4CAF50',
		background: '#E8F5E9',
		transform: 'scale(1.02)'
	},
	playerName: {
		fontSize: '14px',
		fontWeight: 'bold',
		color: '#333',
		marginBottom: '4px'
	},
	playerInfo: {
		fontSize: '12px',
		color: '#666'
	},
	playerFresh: {
		fontSize: '11px',
		color: '#4CAF50',
		fontWeight: 'bold',
		marginTop: '4px'
	},
	substituteButton: {
		display: 'block',
		width: '100%',
		padding: '15px',
		fontSize: '16px',
		fontWeight: 'bold',
		color: 'white',
		background: '#4CAF50',
		border: 'none',
		borderRadius: '8px',
		cursor: 'pointer',
		transition: 'background 0.3s'
	},
	disabledButton: {
		background: '#ccc',
		cursor: 'not-allowed'
	},
	preview: {
		marginTop: '15px',
		padding: '15px',
		background: '#E3F2FD',
		borderRadius: '8px',
		borderLeft: '4px solid #2196F3',
		fontSize: '14px',
		color: '#1565C0'
	}
};
