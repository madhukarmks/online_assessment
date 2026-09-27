import {createContext,useContext,useEffect,useMemo,useState} from 'react'
import {login as loginApi,register as registerApi,me as meApi} from '../api/auth'
import {errorMessage} from '../api/client'

const AuthContext=createContext(null)
const tokenKey='oa_token', userKey='oa_user'
const normalize=u=>u?.data??u
const roleOf=u=>u?.role || u?.authorities?.[0]?.replace('ROLE_','') || ''

export function AuthProvider({children}){
 const [user,setUser]=useState(()=>{try{return JSON.parse(localStorage.getItem(userKey)||'null')}catch{return null}})
 const [loading,setLoading]=useState(Boolean(localStorage.getItem(tokenKey)))
 useEffect(()=>{if(!localStorage.getItem(tokenKey)){setLoading(false);return} meApi().then(u=>{const n=normalize(u);setUser(n);localStorage.setItem(userKey,JSON.stringify(n))}).catch(()=>{localStorage.removeItem(tokenKey);localStorage.removeItem(userKey);setUser(null)}).finally(()=>setLoading(false))},[])
 const login=async(credentials)=>{const raw=normalize(await loginApi(credentials));const token=raw?.token||raw?.accessToken||raw?.jwt; if(!token) throw new Error('Login response did not contain a JWT token.'); localStorage.setItem(tokenKey,token); const u=raw?.user||raw?.profile||await meApi(); const n=normalize(u); localStorage.setItem(userKey,JSON.stringify(n));setUser(n);return n}
 const register=async(payload)=>{const raw=normalize(await registerApi(payload)); if(raw?.token||raw?.accessToken){const token=raw.token||raw.accessToken;localStorage.setItem(tokenKey,token);const u=raw.user||await meApi();const n=normalize(u);localStorage.setItem(userKey,JSON.stringify(n));setUser(n);return n} return raw}
 const logout=()=>{localStorage.removeItem(tokenKey);localStorage.removeItem(userKey);setUser(null)}
 const value=useMemo(()=>({user,loading,login,register,logout,role:roleOf(user),isAdmin:roleOf(user)==='ADMIN',isStudent:roleOf(user)==='STUDENT'}),[user,loading])
 return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export const useAuth=()=>useContext(AuthContext)
export {errorMessage}
