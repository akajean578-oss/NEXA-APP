import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client = null;

/**
 * Ouvre une connexion temps réel authentifiée avec le JWT courant.
 * Retourne le client STOMP déjà connecté (ou en cours de connexion).
 */
export function connectRealtime(onConnected) {
  const token = localStorage.getItem('nexa_token');
  if (!token) return null;

  if (client && client.active) return client;

  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 4000,
    onConnect: () => onConnected?.(client),
  });

  client.activate();
  return client;
}

export function disconnectRealtime() {
  if (client) {
    client.deactivate();
    client = null;
  }
}
