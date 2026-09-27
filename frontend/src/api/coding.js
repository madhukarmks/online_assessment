import api,{unwrap} from './client'
export const languages=async()=>unwrap(await api.get('/coding/languages'))
export const runCode=async(payload)=>unwrap(await api.post('/coding/run',payload))
