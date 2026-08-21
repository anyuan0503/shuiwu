// 开发预览用反向代理：服务前端 dist + 代理 /api 与 /ws 到后端 127.0.0.1:8080
// 用法: node dev-serve.js [端口]
const http = require('http')
const https = require('https')
const fs = require('fs')
const path = require('path')

const PORT = Number(process.argv[2] || 8081)
const DIST = path.join(__dirname, 'frontend', 'dist')
const BACKEND = { host: '127.0.0.1', port: 8080 }

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json'
}

function serveStatic(req, res) {
  let urlPath = decodeURIComponent(req.url.split('?')[0])
  if (urlPath === '/') urlPath = '/index.html'
  let file = path.join(DIST, urlPath)
  if (!file.startsWith(DIST)) { res.writeHead(403); res.end(); return }
  fs.stat(file, (err, st) => {
    if (err || !st.isFile()) {
      // SPA fallback
      file = path.join(DIST, 'index.html')
      fs.readFile(file, (e, data) => {
        if (e) { res.writeHead(404); res.end('not found'); return }
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
        res.end(data)
      })
      return
    }
    const ext = path.extname(file).toLowerCase()
    fs.readFile(file, (e, data) => {
      if (e) { res.writeHead(404); res.end(); return }
      res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' })
      res.end(data)
    })
  })
}

function proxyApi(req, res) {
  const headers = Object.assign({}, req.headers)
  headers.host = BACKEND.host + ':' + BACKEND.port
  const proxyReq = http.request({
    host: BACKEND.host,
    port: BACKEND.port,
    method: req.method,
    path: req.url,
    headers
  }, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, proxyRes.headers)
    proxyRes.pipe(res)
  })
  proxyReq.on('error', () => { res.writeHead(502); res.end('bad gateway') })
  req.pipe(proxyReq)
}

function proxyWs(req, socket, head) {
  const headers = Object.assign({}, req.headers)
  headers.host = BACKEND.host + ':' + BACKEND.port
  const proxyReq = http.request({
    host: BACKEND.host,
    port: BACKEND.port,
    method: 'GET',
    path: req.url,
    headers
  })
  proxyReq.on('upgrade', (proxyRes, proxySocket, proxyHead) => {
    socket.write('HTTP/1.1 101 Switching Protocols\r\n')
    for (const [k, v] of Object.entries(proxyRes.headers)) {
      socket.write(k + ': ' + v + '\r\n')
    }
    socket.write('\r\n')
    if (proxyHead) proxySocket.unshift(proxyHead)
    proxySocket.pipe(socket)
    socket.pipe(proxySocket)
  })
  proxyReq.on('error', () => socket.destroy())
  proxyReq.end()
}

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) return proxyApi(req, res)
  serveStatic(req, res)
})

server.on('upgrade', (req, socket, head) => {
  if (req.url.startsWith('/ws')) return proxyWs(req, socket, head)
  socket.destroy()
})

server.listen(PORT, () => console.log('dev proxy ready on http://127.0.0.1:' + PORT))