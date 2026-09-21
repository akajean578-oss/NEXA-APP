import axios from 'axios';

const client = axios.create({
  baseURL: '/api'
});

// Ajoute automatiquement le token JWT à chaque requête si présent
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('nexa_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default client;
