import api,{unwrap} from './client'
export const login=async(payload)=>unwrap(await api.post('/auth/login',payload))
export const register=async(payload)=>unwrap(await api.post('/auth/register',payload))
export const me=async()=>unwrap(await api.get('/auth/me'))
export const changePassword=async(payload)=>unwrap(await api.put('/auth/change-password',payload))
