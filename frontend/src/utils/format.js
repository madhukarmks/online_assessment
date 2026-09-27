export const money=n=>Number(n||0).toFixed(2)
export const pct=n=>`${Number(n||0).toFixed(1)}%`
export const dateTime=v=>v?new Date(v).toLocaleString(): '—'
export const duration=s=>{let n=Number(s||0);const h=Math.floor(n/3600);n%=3600;const m=Math.floor(n/60),sec=n%60;return [h?`${h}h`:null,m?`${m}m`:null,`${sec}s`].filter(Boolean).join(' ')}
export const unwrapList=x=>Array.isArray(x)?x:(x?.content||x?.items||x?.results||[])
export const idOf=x=>x?.id??x?.assessmentId??x?.attemptId
