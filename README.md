# Distributed-API-rate-limiting-system
Enforce rate limiting per API key across multiple gateway instances using a custom reliable UDP protocol.

*   [Overview](#overview)


## Overview

The **Distributed API Rate-Limit System** is a peer-to-peer Java service that enforces global quotas without any central datastore. Each approved HTTP request (secured via OAuth2/JWT) consumes one token from a local in-memory limiter (Token-Bucket, Leaky-Bucket or Sliding-Window), then broadcasts a tiny UDP “delta” packet to all peers. A lightweight ACK + retransmit layer on top of UDP guarantees exactly-once delivery and eliminates race conditions, keeping cross-node state in sync in under 200 ms at 10 k req/s.  

Key features:
- **Decentralized UDP sync:** Peer-to-peer token‐delta replication for 2–50+ nodes  
- **Reliable delivery:** ACK-driven retries prevent lost updates and races  
- **Pluggable algorithms:** Swap Token-Bucket, Leaky-Bucket or Sliding-Window at runtime  
- **JWT-secured API:** Easy OAuth2/JWT integration for per-key quotas  
- **One-click local deployment:** Docker Compose spins up a two-node cluster in <10 s  
 


