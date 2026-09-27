# Online Assessment Platform — Frontend

React + Vite + Tailwind CSS frontend for the Online Assessment Platform backend.

## Requirements

- Node.js 20+
- npm 10+
- Backend running at `http://localhost:8080` by default

## Setup

```bash
npm install
cp .env.example .env
npm run dev
```

On Windows PowerShell, create `.env` manually if `cp` is unavailable:

```text
VITE_API_URL=http://localhost:8080/api
```

Open `http://localhost:5173`.

## Production build

```bash
npm run build
npm run preview
```

## Environment

`VITE_API_URL` must point to the backend API root, for example:

```text
VITE_API_URL=https://your-backend.example.com/api
```

## Implemented flows

- Student registration/login/logout/profile/password change
- Admin/student role-aware routing
- Student dashboard, assessment catalogue, assessment details
- Server-time based exam countdown using the backend `endsAt`
- Answer persistence and mark-for-review state
- Auto-submit when the server deadline is reached
- Result and question-wise analysis after submission
- Attempt history
- Admin dashboard, assessment CRUD, question CRUD, students, results and leaderboard
- Responsive UI with Tailwind CSS
- Axios JWT interceptor and centralized error handling

## Backend contract

The frontend expects the REST API described in the project prompt, under `/api`:

- `/auth/*`
- `/student/*`
- `/admin/*`
- `/assessments/{id}/leaderboard`

Responses can be wrapped as `{ success, message, data }`; the API client unwraps `data` automatically.

## Exam monitoring configuration

The student exam now includes live camera/microphone preview, media-status monitoring, fullscreen enforcement, tab/window visibility detection, network online/offline handling, configurable proctoring warnings, automatic submission after the warning limit, coding auto-save/local backup, server-based timer handling, and the existing coding compiler/custom-input/question-palette features.

Set this Vite variable in `frontend/.env` if you want a different warning limit:

```env
VITE_MAX_PROCTOR_WARNINGS=3
```

The warning counter is client-side. Browser APIs cannot reliably detect every form of physical movement or determine cheating from camera pixels alone, so this build does not claim to provide biometric/AI movement detection.
