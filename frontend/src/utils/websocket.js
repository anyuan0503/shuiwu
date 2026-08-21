// WebSocket 连接封装：带 token、心跳、断线重连
// 连接地址: /ws?token=<JWT>  (dev 走 vite proxy /ws)

const defaultOptions = {
  url: '/ws',
  heartbeat: 20000, // 心跳间隔 ms
  reconnect: true,
  reconnectInterval: 3000,
  handlers: {
    onOpen: null,
    onMessage: null,
    onClose: null,
    onError: null
  }
}

let ws = null
let heartbeatTimer = null
let reconnectTimer = null
let manualClosed = false
let connected = false

function buildUrl(url) {
  const token = localStorage.getItem('sw_token') || ''
  if (window.location.protocol === 'https:') {
    url = url.replace(/^ws:/, 'wss:').replace(/^http:/, 'https:')
  }
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}token=${encodeURIComponent(token)}`
}

export function connectWs(options = {}) {
  const opts = Object.assign({}, defaultOptions, options)
  const handlers = Object.assign({}, defaultOptions.handlers, opts.handlers)

  manualClosed = false

  function open() {
    ws = new WebSocket(buildUrl(opts.url))
    connected = false

    ws.onopen = () => {
      connected = true
      startHeartbeat()
      handlers.onOpen && handlers.onOpen()
    }

    ws.onmessage = (evt) => {
      let parsed = null
      try {
        parsed = JSON.parse(evt.data)
      } catch (e) {
        parsed = { type: 'raw', data: evt.data }
      }
      // 收到任何消息都说明连接存活
      handlers.onMessage && handlers.onMessage(parsed, evt)
    }

    ws.onclose = () => {
      connected = false
      stopHeartbeat()
      handlers.onClose && handlers.onClose()
      if (opts.reconnect && !manualClosed) scheduleReconnect()
    }

    ws.onerror = (err) => {
      handlers.onError && handlers.onError(err)
      // onerror 后触发 onclose
    }
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (connected && ws && ws.readyState === WebSocket.OPEN) {
        try {
          ws.send(JSON.stringify({ type: 'heartbeat', data: 'ping' }))
        } catch (e) {
          /* ignore */
        }
      }
    }, opts.heartbeat)
  }
  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }
  function scheduleReconnect() {
    clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      if (!manualClosed) open()
    }, opts.reconnectInterval)
  }

  open()

  return {
    send(data) {
      if (connected && ws && ws.readyState === WebSocket.OPEN) {
        ws.send(typeof data === 'string' ? data : JSON.stringify(data))
        return true
      }
      return false
    },
    close() {
      manualClosed = true
      stopHeartbeat()
      clearTimeout(reconnectTimer)
      if (ws) {
        ws.onclose = null
        ws.close()
        ws = null
      }
    },
    isConnected: () => connected
  }
}

export default connectWs