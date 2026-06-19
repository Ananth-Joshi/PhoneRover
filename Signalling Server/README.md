# 🌐 PhoneRover Signaling Server

This directory contains the Node.js signaling server that acts as a real-time communication broker for the 4G RC car project. It uses Socket.IO to orchestrate connection handshakes and exchange WebRTC SDP parameters (offers, answers) and ICE candidates between the ground controller and the onboard rover phone.

---

## 🚀 Getting Started

### 1. Installation
Install the required Socket.IO dependency:

```bash
npm install socket.io

```

### 2. Running Locally

Start the signaling server on default port `3000`:

```bash
node server.js

```

---

## 🎛️ Socket events

The communication layer handles the following WebSocket signaling events:

* **`join-room`**: Subscribes a client to a specific room channel ID.
* **`offer`**: Relays the local WebRTC session description from the caller.
* **`answer`**: Relays the remote WebRTC configuration response.
* **`ice-candidate`**: Standardizes network routing paths between both devices.


