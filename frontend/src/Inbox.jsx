import React, { useState, useEffect } from 'react'
import { useGame } from './GameContext'
import './inbox.css'

export default function Inbox() {
  const { team, token } = useGame()
  const [messages, setMessages] = useState([])
  const [selectedMessage, setSelectedMessage] = useState(null)
  const [filter, setFilter] = useState('all') // 'all', 'unread', 'contract', 'bonus', 'injury', 'suspension'
  const [loading, setLoading] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)

  const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__) || import.meta.env.VITE_API_URL || 'http://localhost:8080'

  // Laden der Nachrichten
  useEffect(() => {
    if (team?.id) {
      loadMessages()
      loadUnreadCount()
    }
  }, [team?.id, filter])

  const loadMessages = async () => {
    setLoading(true)
    try {
      let url = `${API_BASE}/api/v2/messages/${team.id}`
      if (filter === 'unread') {
        url += '/unread'
      } else if (filter !== 'all') {
        url += `/type/${filter}`
      }

      const response = await fetch(url)
      if (response.ok) {
        const data = await response.json()
        setMessages(data)
        if (data.length > 0 && !selectedMessage) {
          setSelectedMessage(data[0])
          markAsRead(data[0].id)
        }
      }
    } catch (error) {
      console.error('Fehler beim Laden der Nachrichten:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadUnreadCount = async () => {
    try {
      const response = await fetch(`${API_BASE}/api/v2/messages/${team.id}/unread/count`)
      if (response.ok) {
        const count = await response.json()
        setUnreadCount(count)
      }
    } catch (error) {
      console.error('Fehler beim Laden der ungelesenen Anzahl:', error)
    }
  }

  const markAsRead = async (messageId) => {
    try {
      await fetch(`${API_BASE}/api/v2/messages/${messageId}/mark-read`, {
        method: 'PUT'
      })
      loadUnreadCount()
    } catch (error) {
      console.error('Fehler beim Markieren als gelesen:', error)
    }
  }

  const deleteMessage = async (messageId) => {
    if (!window.confirm('Nachricht wirklich löschen?')) return

    try {
      const response = await fetch(`${API_BASE}/api/v2/messages/${messageId}`, {
        method: 'DELETE'
      })
      if (response.ok) {
        setMessages(messages.filter(m => m.id !== messageId))
        if (selectedMessage?.id === messageId) {
          setSelectedMessage(messages.find(m => m.id !== messageId) || null)
        }
      }
    } catch (error) {
      console.error('Fehler beim Löschen der Nachricht:', error)
    }
  }

  const formatDate = (dateString) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now - date
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 60) return `vor ${diffMins}m`
    if (diffHours < 24) return `vor ${diffHours}h`
    if (diffDays < 7) return `vor ${diffDays}d`
    
    return date.toLocaleDateString('de-DE')
  }

  const getMessageTypeIcon = (type) => {
    switch(type) {
      case 'contract': return '📋'
      case 'bonus': return '💰'
      case 'injury': return '🤕'
      case 'suspension': return '🟥'
      case 'info': return 'ℹ️'
      default: return '📧'
    }
  }

  const getMessageTypeLabel = (type) => {
    const labels = {
      'contract': 'Vertrag',
      'bonus': 'Prämie',
      'injury': 'Verletzung',
      'suspension': 'Sperrung',
      'info': 'Info'
    }
    return labels[type] || type
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2>📬 Postfach {unreadCount > 0 && <span style={styles.badge}>{unreadCount}</span>}</h2>
      </div>

      <div style={styles.filterBar}>
        <button 
          style={{...styles.filterBtn, ...(filter === 'all' ? styles.filterBtnActive : {})}}
          onClick={() => setFilter('all')}
        >
          Alle ({messages.length})
        </button>
        <button 
          style={{...styles.filterBtn, ...(filter === 'unread' ? styles.filterBtnActive : {})}}
          onClick={() => setFilter('unread')}
        >
          Ungelesen
        </button>
        <button 
          style={{...styles.filterBtn, ...(filter === 'contract' ? styles.filterBtnActive : {})}}
          onClick={() => setFilter('contract')}
        >
          📋 Verträge
        </button>
        <button 
          style={{...styles.filterBtn, ...(filter === 'bonus' ? styles.filterBtnActive : {})}}
          onClick={() => setFilter('bonus')}
        >
          💰 Prämien
        </button>
        <button 
          style={{...styles.filterBtn, ...(filter === 'injury' ? styles.filterBtnActive : {})}}
          onClick={() => setFilter('injury')}
        >
          🤕 Verletzungen
        </button>
        <button 
          style={{...styles.filterBtn, ...(filter === 'suspension' ? styles.filterBtnActive : {})}}
          onClick={() => setFilter('suspension')}
        >
          🟥 Sperrungen
        </button>
      </div>

      <div style={styles.mainLayout}>
        {/* Nachrichtenliste (Links) */}
        <div style={styles.messageList}>
          {loading ? (
            <div style={styles.loadingText}>Lädt...</div>
          ) : messages.length === 0 ? (
            <div style={styles.emptyText}>Keine Nachrichten</div>
          ) : (
            messages.map(msg => (
              <div
                key={msg.id}
                style={{
                  ...styles.messageItem,
                  ...(selectedMessage?.id === msg.id ? styles.messageItemActive : {}),
                  ...(msg.read === false ? styles.messageItemUnread : {})
                }}
                onClick={() => {
                  setSelectedMessage(msg)
                  if (!msg.read) {
                    markAsRead(msg.id)
                  }
                }}
              >
                <div style={styles.messageItemHeader}>
                  <span style={styles.messageIcon}>{getMessageTypeIcon(msg.type)}</span>
                  <span style={styles.messageTitle}>{msg.title}</span>
                </div>
                <div style={styles.messageMeta}>
                  <div style={styles.messageSender}>{msg.sender}</div>
                  <div style={styles.messageTime}>{formatDate(msg.createdAt)}</div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Nachrichteninhalt (Rechts) */}
        <div style={styles.messageContent}>
          {selectedMessage ? (
            <>
              <div style={styles.contentHeader}>
                <div>
                  <h3 style={styles.contentTitle}>
                    <span style={styles.typeIcon}>{getMessageTypeIcon(selectedMessage.type)}</span>
                    {selectedMessage.title}
                  </h3>
                  <div style={styles.contentMeta}>
                    <span style={styles.contentSender}>Von: <strong>{selectedMessage.sender}</strong></span>
                    <span style={styles.contentType}>Typ: {getMessageTypeLabel(selectedMessage.type)}</span>
                    <span style={styles.contentDate}>{new Date(selectedMessage.createdAt).toLocaleString('de-DE')}</span>
                  </div>
                </div>
                <button 
                  style={styles.deleteBtn}
                  onClick={() => deleteMessage(selectedMessage.id)}
                  title="Löschen"
                >
                  🗑️
                </button>
              </div>

              <div style={styles.contentBody}>
                {selectedMessage.content}
              </div>
            </>
          ) : (
            <div style={styles.noSelection}>
              Wähle eine Nachricht, um sie zu lesen
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    gap: '16px'
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  badge: {
    background: '#ef4444',
    color: 'white',
    borderRadius: '12px',
    padding: '2px 6px',
    fontSize: '11px',
    fontWeight: '700'
  },
  filterBar: {
    display: 'flex',
    gap: '8px',
    flexWrap: 'wrap'
  },
  filterBtn: {
    padding: '6px 12px',
    borderRadius: '6px',
    border: '1px solid rgba(255,255,255,0.1)',
    background: 'transparent',
    color: '#cbd5e1',
    cursor: 'pointer',
    fontSize: '12px',
    fontWeight: '500',
    transition: 'all 0.2s'
  },
  filterBtnActive: {
    background: 'linear-gradient(90deg, #06b6d4, #7c3aed)',
    color: '#022',
    border: 'none'
  },
  mainLayout: {
    display: 'flex',
    gap: '16px',
    flex: 1,
    minHeight: '600px'
  },
  '@media (max-width: 768px)': {
    mainLayout: {
      flexDirection: 'column',
      minHeight: 'auto'
    }
  },
  messageList: {
    flex: '0 0 320px',
    background: 'rgba(255,255,255,0.02)',
    border: '1px solid rgba(255,255,255,0.04)',
    borderRadius: '10px',
    padding: '12px',
    overflowY: 'auto',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    '@media (max-width: 768px)': {
      flex: '0 0 150px'
    }
  },
  messageItem: {
    padding: '12px',
    borderRadius: '8px',
    border: '1px solid rgba(255,255,255,0.04)',
    background: 'rgba(255,255,255,0.01)',
    cursor: 'pointer',
    transition: 'all 0.2s',
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  messageItemActive: {
    background: 'linear-gradient(135deg, rgba(6,182,212,0.1), rgba(124,58,237,0.1))',
    border: '1px solid rgba(6,182,212,0.3)',
    boxShadow: '0 0 10px rgba(6,182,212,0.2)'
  },
  messageItemUnread: {
    borderLeft: '3px solid #06b6d4'
  },
  messageItemHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  messageIcon: {
    fontSize: '16px'
  },
  messageTitle: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#e6eef6',
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
  },
  messageMeta: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px'
  },
  messageSender: {
    fontSize: '11px',
    color: '#94a3b8'
  },
  messageTime: {
    fontSize: '10px',
    color: '#64748b'
  },
  loadingText: {
    textAlign: 'center',
    color: '#94a3b8',
    padding: '20px'
  },
  emptyText: {
    textAlign: 'center',
    color: '#94a3b8',
    padding: '20px',
    fontSize: '13px'
  },
  messageContent: {
    flex: 1,
    background: 'rgba(255,255,255,0.02)',
    border: '1px solid rgba(255,255,255,0.04)',
    borderRadius: '10px',
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    overflowY: 'auto'
  },
  contentHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '20px',
    paddingBottom: '16px',
    borderBottom: '1px solid rgba(255,255,255,0.1)'
  },
  contentTitle: {
    fontSize: '18px',
    margin: '0 0 8px 0',
    color: '#e6eef6',
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  typeIcon: {
    fontSize: '20px'
  },
  contentMeta: {
    display: 'flex',
    gap: '12px',
    fontSize: '12px',
    color: '#94a3b8',
    flexWrap: 'wrap'
  },
  contentSender: {
    padding: '4px 8px',
    background: 'rgba(6,182,212,0.1)',
    borderRadius: '4px'
  },
  contentType: {
    padding: '4px 8px',
    background: 'rgba(124,58,237,0.1)',
    borderRadius: '4px'
  },
  contentDate: {
    padding: '4px 8px',
    background: 'rgba(255,255,255,0.05)',
    borderRadius: '4px'
  },
  deleteBtn: {
    background: 'transparent',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '6px',
    padding: '6px 10px',
    cursor: 'pointer',
    fontSize: '16px',
    transition: 'all 0.2s',
    color: '#e6eef6'
  },
  contentBody: {
    flex: 1,
    fontSize: '14px',
    lineHeight: '1.6',
    color: '#cbd5e1',
    whiteSpace: 'pre-wrap',
    wordWrap: 'break-word'
  },
  noSelection: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100%',
    color: '#94a3b8',
    fontSize: '14px'
  }
}
