export function decodeJwt(token){try{const part=token.split('.')[1];return JSON.parse(atob(part.replace(/-/g,'+').replace(/_/g,'/')))}catch{return {}}}
