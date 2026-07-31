import axios from 'axios'
const api = axios.create({ baseURL: '/api/v1' })
export function getTrendChart(days, endDate) { return api.get('/chart/trend', { params: { days, endDate } }) }
export function getDistribution(dataDate, type) { return api.get('/chart/distribution', { params: { dataDate, type } }) }
