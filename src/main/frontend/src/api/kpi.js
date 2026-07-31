import axios from 'axios'
const api = axios.create({ baseURL: '/api/v1' })
export function getKpiSummary(dataDate) { return api.get('/kpi/summary', { params: { dataDate } }) }
