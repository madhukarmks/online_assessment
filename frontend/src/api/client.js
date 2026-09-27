import axios from 'axios'

const api = axios.create({baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api', timeout:30000, headers:{'Content-Type':'application/json'}})

api.interceptors.request.use(config=>{
  const token=localStorage.getItem('oa_token')
  if(token) config.headers.Authorization=`Bearer ${token}`
  return config
})
api.interceptors.response.use(r=>r,e=>{
  if(e.response?.status===401){localStorage.removeItem('oa_token');localStorage.removeItem('oa_user')}
  return Promise.reject(e)
})
export const unwrap = response => response?.data?.data ?? response?.data ?? response
export const errorMessage = e => e?.response?.data?.message || e?.response?.data?.error || (Array.isArray(e?.response?.data?.errors)?e.response.data.errors.map(x=>x.message||x).join(', '):null) || e?.message || 'Something went wrong'
export default api
