import axios from 'axios'
const api = axios.create({ baseURL: '/api/v1' })
export function triggerEmergencyRecall(data) { return api.post('/emergency/recall', data) }
export function getEmergencyStatus(sessionId) { return api.get(`/emergency/status/${sessionId}`) }
export function getEmergencyResult(sessionId) { return api.get(`/emergency/result/${sessionId}`) }
