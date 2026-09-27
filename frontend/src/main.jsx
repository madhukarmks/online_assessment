import React from 'react';
import ReactDOM from 'react-dom/client';
import {
  BrowserRouter,
  Routes,
  Route
} from 'react-router-dom';

import './styles.css';

import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppShell from './layouts/AppShell';

import Landing from './pages/Landing';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

import StudentDashboard from './pages/student/Dashboard';
import StudentAssessments from './pages/student/Assessments';
import AssessmentDetails from './pages/student/AssessmentDetails';
import Exam from './pages/student/Exam';
import StudentResult from './pages/student/Result';
import History from './pages/student/History';
import Profile from './pages/student/Profile';

import AdminDashboard from './pages/admin/Dashboard';
import AdminAssessments from './pages/admin/Assessments';
import AssessmentForm from './pages/admin/AssessmentForm';
import Questions from './pages/admin/Questions';
import Students from './pages/admin/Students';
import Results from './pages/admin/Results';
import ResultDetail from './pages/admin/ResultDetail';
import Leaderboard from './pages/admin/Leaderboard';
import NotFound from './pages/NotFound';

const Shell = ({ children, role }) => (
  <ProtectedRoute role={role}>
    <AppShell>
      {children}
    </AppShell>
  </ProtectedRoute>
);

function App() {
  return (
    <AuthProvider>
      <Routes>

        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          path="/student"
          element={
            <Shell role="STUDENT">
              <StudentDashboard />
            </Shell>
          }
        />

        <Route
          path="/student/assessments"
          element={
            <Shell role="STUDENT">
              <StudentAssessments />
            </Shell>
          }
        />

        <Route
          path="/student/assessments/:id"
          element={
            <Shell role="STUDENT">
              <AssessmentDetails />
            </Shell>
          }
        />

        <Route
          path="/student/attempt/:id"
          element={
            <Shell role="STUDENT">
              <Exam />
            </Shell>
          }
        />

        <Route
          path="/student/results/:id"
          element={
            <Shell role="STUDENT">
              <StudentResult />
            </Shell>
          }
        />

        <Route
          path="/student/history"
          element={
            <Shell role="STUDENT">
              <History />
            </Shell>
          }
        />

        <Route
          path="/student/profile"
          element={
            <Shell role="STUDENT">
              <Profile />
            </Shell>
          }
        />

        <Route
          path="/admin"
          element={
            <Shell role="ADMIN">
              <AdminDashboard />
            </Shell>
          }
        />

        <Route
          path="/admin/assessments"
          element={
            <Shell role="ADMIN">
              <AdminAssessments />
            </Shell>
          }
        />

        <Route
          path="/admin/assessments/new"
          element={
            <Shell role="ADMIN">
              <AssessmentForm />
            </Shell>
          }
        />

        <Route
          path="/admin/assessments/:id/edit"
          element={
            <Shell role="ADMIN">
              <AssessmentForm />
            </Shell>
          }
        />

        <Route
          path="/admin/assessments/:aid/questions"
          element={
            <Shell role="ADMIN">
              <Questions />
            </Shell>
          }
        />

        <Route
          path="/admin/students"
          element={
            <Shell role="ADMIN">
              <Students />
            </Shell>
          }
        />

        <Route
          path="/admin/results"
          element={
            <Shell role="ADMIN">
              <Results />
            </Shell>
          }
        />

        <Route
          path="/admin/results/:id"
          element={
            <Shell role="ADMIN">
              <ResultDetail />
            </Shell>
          }
        />

        <Route
          path="/admin/profile"
          element={
            <Shell role="ADMIN">
              <Profile />
            </Shell>
          }
        />

        <Route
          path="/admin/assessments/:id/leaderboard"
          element={
            <Shell role="ADMIN">
              <Leaderboard />
            </Shell>
          }
        />

        <Route path="*" element={<NotFound />} />

      </Routes>
    </AuthProvider>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);