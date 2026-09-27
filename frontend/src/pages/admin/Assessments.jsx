import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { assessments, deleteAssessment } from '../../api/admin';
import Loading from '../../components/Loading';
import Empty from '../../components/Empty';
import Badge from '../../components/Badge';
import Button from '../../components/Button';
import { unwrapList, dateTime } from '../../utils/format';
import Toast from '../../components/Toast';
import { errorMessage } from '../../api/client';

export default function Assessments() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  const load = async () => {
    try {
      setError('');

      const response = await assessments();
      setItems(unwrapList(response));
    } catch (e) {
      setError(errorMessage(e));
      setItems([]);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const del = async (id) => {
    if (
      !confirm(
        'Delete this assessment? Questions must be removed first if the backend enforces that rule.'
      )
    ) {
      return;
    }

    try {
      await deleteAssessment(id);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  if (!items) {
    return <Loading />;
  }

  return (
    <div>
      <Toast
        message={error}
        type="error"
        onClose={() => setError('')}
      />

      <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">
            Assessments
          </h1>

          <p className="mt-2 text-slate-500">
            Create and manage your examination catalogue.
          </p>
        </div>

        <Link to="/admin/assessments/new">
          <Button variant="brand">
            + Create assessment
          </Button>
        </Link>
      </div>

      {items.length === 0 ? (
        <Empty
          title="No assessments"
          text="Create your first assessment to begin."
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
          {items.map((a) => (
            <div
              key={a.id}
              className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
            >
              <div className="flex items-start justify-between gap-3">
                <h3 className="font-bold">
                  {a.title}
                </h3>

                <Badge
                  tone={a.published ? 'green' : 'yellow'}
                >
                  {a.published ? 'Published' : 'Draft'}
                </Badge>
              </div>

              <p className="mt-2 min-h-12 text-sm text-slate-500">
                {a.description || 'No description'}
              </p>

              <div className="mt-5 grid grid-cols-3 gap-2 text-xs">
                <Mini
                  t="Duration"
                  v={`${a.durationMinutes}m`}
                />

                <Mini
                  t="Questions"
                  v={a.questionCount ?? 0}
                />

                <Mini
                  t="Marks"
                  v={a.totalMarks}
                />
              </div>

              <p className="mt-4 text-xs text-slate-400">
                {a.startAt
                  ? dateTime(a.startAt)
                  : 'No start time'}

                {a.endAt
                  ? ` → ${dateTime(a.endAt)}`
                  : ''}
              </p>

              <div className="mt-5 flex gap-2">
                <Link
                  className="flex-1"
                  to={`/admin/assessments/${a.id}/edit`}
                >
                  <Button
                    variant="secondary"
                    className="w-full"
                  >
                    Edit
                  </Button>
                </Link>

                <Button
                  variant="danger"
                  onClick={() => del(a.id)}
                >
                  Delete
                </Button>
              </div>

              <Link
                to={`/admin/assessments/${a.id}/questions`}
                className="mt-2 block rounded-xl bg-slate-900 px-3 py-2.5 text-center text-sm font-semibold text-white"
              >
                Manage questions
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const Mini = ({ t, v }) => (
  <div className="rounded-lg bg-slate-50 p-2">
    <span className="text-slate-400">
      {t}
    </span>

    <p className="mt-1 font-semibold text-slate-700">
      {v}
    </p>
  </div>
);