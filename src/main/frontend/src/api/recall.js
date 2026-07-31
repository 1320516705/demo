import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export function getRecallList(params) { return api.get('/recall/list', { params }) }
export function getRecallDetail(id) { return api.get(`/recall/${id}`) }
export function outreachDriver(id, data) { return api.put(`/recall/${id}/outreach`, data) }
export function batchOutreach(data) { return api.post('/recall/batch-outreach', data) }
export function getHeatmapData(dataDate) { return api.get('/map/heatmap', { params: { dataDate } }) }
