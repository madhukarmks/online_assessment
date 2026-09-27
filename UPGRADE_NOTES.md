# Online Assessment Platform - Phase 1 Fixed + Coding Upgrade

## What was fixed
1. Repeated HTTP 500s on admin dashboard, assessments, questions and results caused by lazy JPA relations being accessed after repository transactions. Read-only transaction boundaries were added to the affected service methods.
2. Backend now logs unexpected exceptions server-side so future 500s can be diagnosed from Render/server logs.
3. Question management now supports MCQ, multiple-choice, true/false and coding questions.
4. Coding questions support starter code, selected compiler languages and hidden input/output test cases.
5. Students get a coding editor inside the assessment with Run Code, Save Code and compiler output.
6. Coding submissions are evaluated against the hidden test cases when the assessment is submitted.
7. Assessment attempts request camera + microphone permissions before the exam UI is shown.
8. Existing JWT/admin/student access control remains in place; admin question APIs are protected by `ROLE_ADMIN`.

## Camera + microphone
Browsers allow `getUserMedia()` on HTTPS sites and on localhost. If deployed on Amplify/another HTTPS host, the browser permission prompt should appear. The app does not record or upload the camera/microphone stream in this phase; it only requests access as an assessment proctoring prerequisite.

## Compiler
The backend uses Judge0 CE through configurable environment variables:

- `JUDGE0_URL=https://ce.judge0.com` (or your self-hosted Judge0 URL)
- `JUDGE0_API_KEY=` (optional when the selected Judge0 instance does not require one)
- `JUDGE0_POLL_MS=500`
- `JUDGE0_MAX_POLLS=30`

Supported language IDs in the admin UI:
- C: 50
- C++: 54
- Java: 62
- Python 3: 71
- JavaScript: 63

For a production/high-volume assessment platform, use an authenticated/self-hosted Judge0 instance rather than relying on a public compiler endpoint.

## Local run
### Backend
```bash
cd backend
mvn clean package -DskipTests
java -jar target/online-assessment-backend-1.0.0.jar
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Set `frontend/.env`:
```env
VITE_API_URL=http://localhost:8080/api
```

For deployment, set `VITE_API_URL` to the HTTPS backend API URL and set backend `FRONTEND_URL` to the HTTPS frontend origin.

## Database migration
The backend uses `spring.jpa.hibernate.ddl-auto=update` by default, so the new `starter_code`, `allowed_language_ids`, `test_cases_json`, `code`, and `code_language_id` columns are created automatically. For production, review and migrate these columns explicitly with your database migration tool before changing `DDL_AUTO` to `validate`.
