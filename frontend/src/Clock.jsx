import React, { useState, useEffect } from 'react'

export default function Clock(){
  const [time, setTime] = useState(new Date())

  useEffect(() => {
    const timer = setInterval(() => {
      setTime(new Date())
    }, 1000)
    
    return () => clearInterval(timer)
  }, [])

  const formatDate = (date) => {
    const options = { weekday: 'short', year: 'numeric', month: '2-digit', day: '2-digit' }
    return date.toLocaleDateString('de-DE', options)
  }

  const formatTime = (date) => {
    return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }

  return (
    <div className="clock">
      <div className="clock-time">{formatTime(time)}</div>
      <div className="clock-date">{formatDate(time)}</div>
    </div>
  )
}
