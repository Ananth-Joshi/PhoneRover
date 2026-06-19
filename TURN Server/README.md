# PhoneRover Cloud Infrastructure (TURN Server)

To drive the PhoneRover over a 4G/LTE cellular network, you need a relay server to punch through the mobile carrier's strict firewalls. 

This folder contains a secure, ready-to-deploy [Coturn](https://github.com/coturn/coturn) server optimized for WebRTC video and low-latency JSON telemetry. 

## ☁️ Prerequisites

You need a Linux cloud server with a **static public IP address**. 
*We highly recommend the **Oracle Cloud Always Free Tier** (Compute VM), but AWS EC2, DigitalOcean, or Linode will work perfectly.*

You also need [Docker and Docker Compose](https://docs.docker.com/engine/install/) installed on that server.

---

## 🛠️ Step 1: Open Your Cloud Firewalls

Before booting the server, you **must** configure your cloud provider's networking dashboard (e.g., Oracle Security Lists or AWS Security Groups) to allow WebRTC traffic. 

Add the following **Ingress Rules** to your cloud firewall:
1. **The WebRTC Handshake:** Allow `TCP` and `UDP` on port `3478`
2. **The Video/Telemetry Relay:** Allow `UDP` ONLY on ports `50000 - 50010`

*Note: If you are using Ubuntu, you may also need to open these ports on the internal firewall using `iptables`.*

---

## 🔒 Step 2: Configure Environment Variables

For security, **never hardcode your passwords or IPs into the repository.**

1. Inside this folder on your cloud server, duplicate the example environment file:
   ```bash
   cp .env.example .env