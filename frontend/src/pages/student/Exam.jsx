import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import {
  getAttempt,
  saveAnswer,
  submitAttempt
} from '../../api/student';

import { runCode } from '../../api/coding';

import Loading from '../../components/Loading';
import Button from '../../components/Button';
import Badge from '../../components/Badge';

import { errorMessage } from '../../api/client';
import { dateTime } from '../../utils/format';

export default function Exam() {
  const { id } = useParams();
  const nav = useNavigate();

  /* =======================================================
     ATTEMPT STATE
  ======================================================= */

  const [attempt, setAttempt] = useState(null);
  const [current, setCurrent] = useState(0);

  const [answers, setAnswers] = useState({});
  const [marked, setMarked] = useState(new Set());

  const [codes, setCodes] = useState({});
  const [langs, setLangs] = useState({});
  const [stdins, setStdins] = useState({});

  const [remaining, setRemaining] = useState(0);

  const [saving, setSaving] = useState(false);
  const [running, setRunning] = useState(false);
  const [output, setOutput] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  /* =======================================================
     CAMERA + MICROPHONE STATE
  ======================================================= */

  const [media, setMedia] = useState('checking');

  const [cameraEnabled, setCameraEnabled] =
    useState(false);

  const [microphoneEnabled, setMicrophoneEnabled] =
    useState(false);

  const streamRef = useRef(null);
  const videoRef = useRef(null);

  const submitted = useRef(false);

  /* =======================================================
     PROCTORING / WARNING STATE
  ======================================================= */

  const [warnings, setWarnings] = useState(0);

  const warningLockRef = useRef(false);
  const warningCountRef = useRef(0);
  const autoSaveTimerRef = useRef(null);
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [isFullscreen, setIsFullscreen] = useState(!!document.fullscreenElement);
  const [fullscreenRequired, setFullscreenRequired] = useState(false);
  const [lastViolation, setLastViolation] = useState('');

  // Configurable from Vite: VITE_MAX_PROCTOR_WARNINGS=3
  const maxWarnings = Math.max(1, Number(import.meta.env.VITE_MAX_PROCTOR_WARNINGS || 3));

  /* =======================================================
     CAMERA + MICROPHONE
     
     IMPORTANT:
     DO NOT STOP TRACKS AFTER getUserMedia().
     Camera and microphone must remain active
     during the complete assessment.
  ======================================================= */

  useEffect(() => {
    let mounted = true;

    const requestMedia = async () => {
      try {
        if (
          !navigator.mediaDevices ||
          !navigator.mediaDevices.getUserMedia
        ) {
          throw new Error(
            'Camera and microphone are not supported by this browser.'
          );
        }

        const stream =
          await navigator.mediaDevices.getUserMedia({
            video: {
              facingMode: 'user',
              width: {
                ideal: 1280
              },
              height: {
                ideal: 720
              }
            },
            audio: true
          });

        if (!mounted) {
          stream
            .getTracks()
            .forEach((track) => track.stop());

          return;
        }

        streamRef.current = stream;

        const videoTracks =
          stream.getVideoTracks();

        const audioTracks =
          stream.getAudioTracks();

        setCameraEnabled(
          videoTracks.length > 0 &&
            videoTracks[0].enabled
        );

        setMicrophoneEnabled(
          audioTracks.length > 0 &&
            audioTracks[0].enabled
        );

        setMedia('granted');

      } catch (err) {
        console.error(
          'Camera/microphone permission error:',
          err
        );

        if (mounted) {
          setMedia('denied');
        }
      }
    };

    requestMedia();

    return () => {
      mounted = false;

      /*
       * Stop camera and microphone ONLY
       * when Exam component is unmounted.
       */
      if (streamRef.current) {
        streamRef.current
          .getTracks()
          .forEach((track) => {
            track.stop();
          });

        streamRef.current = null;
      }
    };
  }, []);

  /* =======================================================
     ATTACH LIVE STREAM TO VIDEO ELEMENT
  ======================================================= */

  useEffect(() => {
    if (
      media !== 'granted' ||
      !videoRef.current ||
      !streamRef.current
    ) {
      return;
    }

    const video = videoRef.current;

    video.srcObject = streamRef.current;
    video.muted = true;
    video.playsInline = true;

    const playVideo = async () => {
      try {
        await video.play();
      } catch (err) {
        console.warn(
          'Video autoplay was blocked:',
          err
        );
      }
    };

    playVideo();

    return () => {
      if (video) {
        video.srcObject = null;
      }
    };
  }, [media]);

  /* =======================================================
     MEDIA TRACK MONITOR
  ======================================================= */

  useEffect(() => {
    if (
      media !== 'granted' ||
      !streamRef.current
    ) {
      return;
    }

    const stream = streamRef.current;

    const videoTracks =
      stream.getVideoTracks();

    const audioTracks =
      stream.getAudioTracks();

    const updateMediaStatus = () => {
      const videoTrack =
        videoTracks[0];

      const audioTrack =
        audioTracks[0];

      setCameraEnabled(
        !!videoTrack &&
        videoTrack.readyState === 'live' &&
        videoTrack.enabled
      );

      setMicrophoneEnabled(
        !!audioTrack &&
        audioTrack.readyState === 'live' &&
        audioTrack.enabled
      );
    };

    videoTracks.forEach((track) => {
      track.addEventListener(
        'ended',
        updateMediaStatus
      );

      track.addEventListener(
        'mute',
        updateMediaStatus
      );

      track.addEventListener(
        'unmute',
        updateMediaStatus
      );
    });

    audioTracks.forEach((track) => {
      track.addEventListener(
        'ended',
        updateMediaStatus
      );

      track.addEventListener(
        'mute',
        updateMediaStatus
      );

      track.addEventListener(
        'unmute',
        updateMediaStatus
      );
    });

    updateMediaStatus();

    const interval = setInterval(
      updateMediaStatus,
      1000
    );

    return () => {
      clearInterval(interval);

      videoTracks.forEach((track) => {
        track.removeEventListener(
          'ended',
          updateMediaStatus
        );

        track.removeEventListener(
          'mute',
          updateMediaStatus
        );

        track.removeEventListener(
          'unmute',
          updateMediaStatus
        );
      });

      audioTracks.forEach((track) => {
        track.removeEventListener(
          'ended',
          updateMediaStatus
        );

        track.removeEventListener(
          'mute',
          updateMediaStatus
        );

        track.removeEventListener(
          'unmute',
          updateMediaStatus
        );
      });
    };
  }, [media]);

  /* =======================================================
     PROCTORING WARNING FUNCTION
     
     Browser-detectable events:
     1. Leaving exam tab/window
     2. Camera disabled/stopped
     3. Microphone disabled/stopped
     
     After 3 warnings -> automatic submission.
  ======================================================= */

  const addWarning = (reason) => {
    if (submitted.current || submitting || warningLockRef.current) return;

    warningLockRef.current = true;
    const next = Math.min(maxWarnings, warningCountRef.current + 1);
    warningCountRef.current = next;
    setWarnings(next);
    setLastViolation(reason);
    setError(`Proctoring warning: ${reason}. Warning ${next}/${maxWarnings}.`);

    if (next >= maxWarnings) {
      submitted.current = true;
      setTimeout(() => doSubmit(true), 0);
    }

    setTimeout(() => {
      warningLockRef.current = false;
    }, 1500);
  };

  const enterFullscreen = async () => {
    try {
      if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
        await document.documentElement.requestFullscreen();
      }
      setIsFullscreen(!!document.fullscreenElement);
      setFullscreenRequired(false);
    } catch (e) {
      console.warn('Fullscreen request failed:', e);
      setFullscreenRequired(true);
    }
  };

  /* =======================================================
     TAB / WINDOW VISIBILITY MONITOR
  ======================================================= */

  useEffect(() => {
    if (media !== 'granted') return;

    const handleVisibility = () => {
      if (
        document.visibilityState === 'hidden'
      ) {
        addWarning(
          'You left the assessment screen'
        );
      }
    };

    const handleBlur = () => {
      addWarning(
        'Assessment window lost focus'
      );
    };

    document.addEventListener(
      'visibilitychange',
      handleVisibility
    );

    window.addEventListener(
      'blur',
      handleBlur
    );

    return () => {
      document.removeEventListener(
        'visibilitychange',
        handleVisibility
      );

      window.removeEventListener(
        'blur',
        handleBlur
      );
    };
  }, [media]);

  /* =======================================================
     CAMERA / MICROPHONE PROCTORING MONITOR
  ======================================================= */

  useEffect(() => {
    if (
      media !== 'granted' ||
      !streamRef.current
    ) {
      return;
    }

    const checkTracks = () => {
      if (
        submitted.current ||
        submitting
      ) {
        return;
      }

      const stream =
        streamRef.current;

      const videoTrack =
        stream.getVideoTracks()[0];

      const audioTrack =
        stream.getAudioTracks()[0];

      if (
        !videoTrack ||
        videoTrack.readyState !== 'live' ||
        !videoTrack.enabled
      ) {
        addWarning(
          'Camera is disabled or unavailable'
        );

        return;
      }

      if (
        !audioTrack ||
        audioTrack.readyState !== 'live' ||
        !audioTrack.enabled
      ) {
        addWarning(
          'Microphone is disabled or unavailable'
        );
      }
    };

    const interval = setInterval(
      checkTracks,
      2000
    );

    return () => {
      clearInterval(interval);
    };
  }, [media, submitting]);

  /* =======================================================
     FULLSCREEN ENFORCEMENT
  ======================================================= */

  useEffect(() => {
    if (media !== 'granted') return;

    const handleFullscreen = () => {
      const active = !!document.fullscreenElement;
      setIsFullscreen(active);
      if (!active && !submitted.current && !submitting) {
        setFullscreenRequired(true);
        addWarning('Fullscreen mode was exited');
      }
    };

    document.addEventListener('fullscreenchange', handleFullscreen);
    return () => document.removeEventListener('fullscreenchange', handleFullscreen);
  }, [media, submitting]);

  /* =======================================================
     NETWORK MONITORING
  ======================================================= */

  useEffect(() => {
    const online = () => {
      setIsOnline(true);
      setError('Network connection restored.');
    };
    const offline = () => {
      setIsOnline(false);
      setError('Network disconnected. Answers will be retried when connection returns.');
    };

    window.addEventListener('online', online);
    window.addEventListener('offline', offline);
    return () => {
      window.removeEventListener('online', online);
      window.removeEventListener('offline', offline);
    };
  }, []);

  /* =======================================================
     LOAD ATTEMPT
  ======================================================= */

  useEffect(() => {
    getAttempt(id)
      .then((a) => {
        setAttempt(a);

        const qs = a.questions || [];

        const map = {};
        const cm = {};
        const lm = {};
        const im = {};

        qs.forEach((question) => {
          map[question.questionId] =
            new Set(
              question.selectedOptionIds ||
                []
            );

          if (question.code != null) {
            cm[question.questionId] =
              question.code;
          } else if (
            question.starterCode
          ) {
            cm[question.questionId] =
              question.starterCode;
          }

          if (
            question.codeLanguageId
          ) {
            lm[question.questionId] =
              question.codeLanguageId;
          }

          const savedCode = localStorage.getItem(`assessment:${id}:q:${question.questionId}:code`);
          const savedStdin = localStorage.getItem(`assessment:${id}:q:${question.questionId}:stdin`);
          im[question.questionId] = savedStdin ?? question.stdin ?? '';
          if (savedCode !== null) cm[question.questionId] = savedCode;
        });

        setAnswers(map);
        setCodes(cm);
        setLangs(lm);
        setStdins(im);

        setMarked(
          new Set(
            qs
              .filter(
                (question) =>
                  question.markedForReview
              )
              .map(
                (question) =>
                  question.questionId
              )
          )
        );

        setRemaining(
          Math.max(
            0,
            new Date(
              a.endsAt
            ).getTime() -
              Date.now()
          )
        );
      })
      .catch((e) => {
        setError(
          errorMessage(e)
        );
      });
  }, [id]);

  /* =======================================================
     TIMER
  ======================================================= */

  useEffect(() => {
    if (!attempt) return;

    const end =
      new Date(
        attempt.endsAt
      ).getTime();

    const tick = () => {
      setRemaining(
        Math.max(
          0,
          end - Date.now()
        )
      );
    };

    tick();

    const timer =
      setInterval(
        tick,
        500
      );

    return () =>
      clearInterval(timer);
  }, [attempt]);

  /* =======================================================
     AUTO SUBMIT WHEN TIMER ENDS
  ======================================================= */

  useEffect(() => {
    if (
      attempt &&
      remaining <= 0 &&
      !submitted.current
    ) {
      submitted.current = true;

      doSubmit(true);
    }
  }, [remaining, attempt]);

  /* =======================================================
     CURRENT QUESTION
  ======================================================= */

  const qs =
    attempt?.questions || [];

  const q = qs[current];

  /* =======================================================
     AUTO-SAVE CODING WORK
     Saves code/stdin after a short idle period and also keeps
     a local backup so a temporary network failure does not
     immediately destroy the student's work.
  ======================================================= */

  useEffect(() => {
    if (!q || q.type !== 'CODING' || !attempt || !isOnline || submitting) return;

    const code = codes[q.questionId] || '';
    const stdin = stdins[q.questionId] || '';
    localStorage.setItem(`assessment:${id}:q:${q.questionId}:code`, code);
    localStorage.setItem(`assessment:${id}:q:${q.questionId}:stdin`, stdin);

    clearTimeout(autoSaveTimerRef.current);
    autoSaveTimerRef.current = setTimeout(() => {
      persist(
        q,
        new Set(answers[q.questionId] || []),
        marked.has(q.questionId),
        code,
        Number(langs[q.questionId] || q.allowedLanguageIds?.[0] || 54)
      );
    }, 1200);

    return () => clearTimeout(autoSaveTimerRef.current);
  }, [q?.questionId, codes[q?.questionId], stdins[q?.questionId], isOnline]);


  /* =======================================================
     ANSWERED COUNT
  ======================================================= */

  const answeredCount =
    Object.values(
      answers
    ).filter(
      (s) => s?.size
    ).length +
    Object.values(
      codes
    ).filter(
      (x) => x?.trim()
    ).length;

  /* =======================================================
     TIMER FORMAT
  ======================================================= */

  const timeText = useMemo(() => {
    const sec =
      Math.ceil(
        remaining / 1000
      );

    const m =
      Math.floor(
        sec / 60
      );

    const s =
      sec % 60;

    return `${String(
      m
    ).padStart(
      2,
      '0'
    )}:${String(
      s
    ).padStart(
      2,
      '0'
    )}`;
  }, [remaining]);

  /* =======================================================
     SAVE ANSWER
  ======================================================= */

  const persist = async (
    question,
    selected,
    review,
    code =
      codes[
        question.questionId
      ] || '',
    languageId =
      langs[
        question.questionId
      ]
  ) => {
    setSaving(true);

    try {
      await saveAnswer(id, {
        questionId:
          question.questionId,

        selectedOptionIds: [
          ...selected
        ],

        markedForReview:
          review,

        code:
          question.type ===
          'CODING'
            ? code
            : null,

        codeLanguageId:
          question.type ===
          'CODING'
            ? Number(
                languageId
              )
            : null
      });
    } catch (e) {
      setError(
        errorMessage(e)
      );
    } finally {
      setSaving(false);
    }
  };

  /* =======================================================
     MCQ OPTION
  ======================================================= */

  const choose = async (
    optionId
  ) => {
    if (
      !q ||
      saving ||
      submitting ||
      q.type === 'CODING'
    ) {
      return;
    }

    let next =
      new Set(
        answers[
          q.questionId
        ] || []
      );

    if (
      q.type ===
      'MCQ_MULTIPLE'
    ) {
      next.has(
        optionId
      )
        ? next.delete(
            optionId
          )
        : next.add(
            optionId
          );
    } else {
      next =
        new Set([
          optionId
        ]);
    }

    setAnswers({
      ...answers,
      [q.questionId]:
        next
    });

    await persist(
      q,
      next,
      marked.has(
        q.questionId
      )
    );
  };

  /* =======================================================
     RUN CODE
  ======================================================= */

  const run = async () => {
    if (
      !q ||
      q.type !==
        'CODING' ||
      !codes[
        q.questionId
      ]?.trim()
    ) {
      return;
    }

    setRunning(true);
    setOutput(
      'Running...'
    );

    try {
      const languageId =
        Number(
          langs[
            q.questionId
          ] ||
            q.allowedLanguageIds?.[0] ||
            54
        );

      const r =
        await runCode({
          questionId:
            q.questionId,

          languageId,

          sourceCode:
            codes[
              q.questionId
            ],

          stdin:
            stdins[
              q.questionId
            ] || ''
        });

      setOutput(
        [
          r.status,
          r.stdout,
          r.stderr,
          r.compileOutput,
          r.message
        ]
          .filter(Boolean)
          .join(
            '\n\n'
          )
      );

      await persist(
        q,
        new Set(
          answers[
            q.questionId
          ] || []
        ),
        marked.has(
          q.questionId
        ),
        codes[
          q.questionId
        ],
        languageId
      );

    } catch (e) {
      setOutput(
        errorMessage(e)
      );
    } finally {
      setRunning(false);
    }
  };

  /* =======================================================
     MARK FOR REVIEW
  ======================================================= */

  const toggleReview =
    async () => {
      if (!q) return;

      const next =
        new Set(marked);

      next.has(
        q.questionId
      )
        ? next.delete(
            q.questionId
          )
        : next.add(
            q.questionId
          );

      setMarked(next);

      await persist(
        q,
        answers[
          q.questionId
        ] || new Set(),
        next.has(
          q.questionId
        )
      );
    };

  /* =======================================================
     CLEAR QUESTION
  ======================================================= */

  const clear =
    async () => {
      if (!q) return;

      setAnswers({
        ...answers,
        [q.questionId]:
          new Set()
      });

      setCodes({
        ...codes,
        [q.questionId]:
          ''
      });

      setStdins({
        ...stdins,
        [q.questionId]:
          ''
      });

      await persist(
        q,
        new Set(),
        marked.has(
          q.questionId
        ),
        '',
        langs[
          q.questionId
        ]
      );

      setOutput('');
    };

  /* =======================================================
     SUBMIT
  ======================================================= */

  const doSubmit =
    async (
      auto = false
    ) => {
      if (submitting) {
        return;
      }

      setSubmitting(true);

      try {
        const r =
          await submitAttempt(
            id
          );

        nav(
          `/student/results/${
            r?.summary?.id ||
            r?.id ||
            r?.resultId
          }`,
          {
            replace: true,
            state: {
              autoSubmitted:
                auto
            }
          }
        );

      } catch (e) {
        setError(
          errorMessage(e)
        );

        submitted.current =
          false;

        setSubmitting(
          false
        );
      }
    };

  /* =======================================================
     LOADING
  ======================================================= */

  if (!attempt) {
    return <Loading />;
  }

  /* =======================================================
     CAMERA/MIC PERMISSION SCREEN
  ======================================================= */

  if (
    media !== 'granted'
  ) {
    return (
      <div className="mx-auto max-w-xl rounded-2xl border bg-white p-8 text-center shadow-sm">

        <div className="text-5xl">
          {media ===
          'checking'
            ? '🎥'
            : '⚠️'}
        </div>

        <h1 className="mt-4 text-2xl font-bold">
          Camera & microphone
          required
        </h1>

        <p className="mt-2 text-sm text-slate-500">
          This assessment requires
          camera and microphone
          access. Please allow both
          permissions in your browser.
        </p>

        {media ===
          'denied' && (
          <div className="mt-5 rounded-xl bg-red-50 p-4 text-left text-sm text-red-700">

            <p className="font-semibold">
              Permission was denied.
            </p>

            <p className="mt-1">
              Open your browser site
              permissions and allow:
            </p>

            <ul className="mt-2 list-disc pl-5">
              <li>
                Camera
              </li>

              <li>
                Microphone
              </li>
            </ul>

          </div>
        )}

        <Button
          variant="brand"
          className="mt-5"
          onClick={() =>
            window.location.reload()
          }
        >
          Check permissions again
        </Button>

      </div>
    );
  }

  /* =======================================================
     NO QUESTIONS
  ======================================================= */

  if (
    qs.length === 0
  ) {
    return (
      <div className="rounded-2xl bg-white p-10 text-center">
        No questions were returned
        for this attempt.
      </div>
    );
  }

  /* =======================================================
     MAIN UI
  ======================================================= */

  return (
    <div className="mx-auto max-w-7xl">

      {/* =================================================
          HEADER
      ================================================= */}

      <div className="sticky top-16 z-20 -mx-4 border-b bg-white/95 px-4 py-3 backdrop-blur md:-mx-7 md:px-7">

        <div className="flex items-center justify-between gap-4">

          <div>
            <p className="text-xs text-slate-400">
              Assessment
            </p>

            <h1 className="font-bold">
              {
                attempt.assessmentTitle
              }
            </h1>
          </div>

          <div className="flex items-center gap-3">

            <div className={`rounded-xl px-3 py-2 text-xs font-semibold ${isOnline ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
              {isOnline ? '● Online' : '● Offline'}
            </div>

            <div className={`rounded-xl px-3 py-2 text-xs font-semibold ${isFullscreen ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
              {isFullscreen ? '⛶ Fullscreen' : '⚠ Fullscreen'}
            </div>

            {/* PROCTORING WARNING COUNTER */}

            <div
              className={`rounded-xl px-3 py-2 text-xs font-semibold ${
                warnings === 0
                  ? 'bg-emerald-100 text-emerald-700'
                  : warnings <
                    maxWarnings
                  ? 'bg-amber-100 text-amber-700'
                  : 'bg-red-100 text-red-700'
              }`}
            >
              ⚠️ {warnings}/
              {maxWarnings}
            </div>

            {/* TIMER */}

            <div
              className={`rounded-xl px-4 py-2 font-mono text-lg font-bold ${
                remaining <=
                60000
                  ? 'bg-red-100 text-red-700'
                  : remaining <=
                    300000
                  ? 'bg-amber-100 text-amber-700'
                  : 'bg-slate-900 text-white'
              }`}
            >
              ◷ {timeText}
            </div>

          </div>

        </div>

      </div>

      {!isOnline && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          <b>Network disconnected.</b> Keep working; saved answers will be retried after the connection returns. Avoid closing or refreshing this page.
        </div>
      )}

      {fullscreenRequired && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <span><b>Fullscreen required.</b> Re-enter fullscreen to continue the monitored assessment.</span>
          <Button variant="brand" onClick={enterFullscreen}>Enter fullscreen</Button>
        </div>
      )}

      {/* =================================================
          CONTENT
      ================================================= */}

      <div className="mt-5 grid gap-5 lg:grid-cols-[1fr_280px]">

        {/* =================================================
            QUESTION
        ================================================= */}

        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-7">

          <div className="flex items-center justify-between">

            <Badge tone="blue">
              Question{' '}
              {current + 1}{' '}
              / {qs.length}
            </Badge>

            <span className="text-xs text-slate-400">
              {saving
                ? 'Saving...'
                : 'Saved'}
            </span>

          </div>

          <h2 className="mt-7 text-xl font-bold leading-8">
            {
              q.questionText
            }
          </h2>

          <p className="mt-2 text-xs text-slate-400">
            {q.marks} mark
            {q.marks === 1
              ? ''
              : 's'}{' '}
            · {q.type}
          </p>

          {/* =================================================
              CODING QUESTION
          ================================================= */}

          {q.type ===
          'CODING' ? (

            <div className="mt-6 space-y-4">

              {/* LANGUAGE + RUN */}

              <div className="flex flex-wrap items-center gap-3">

                <select
                  value={
                    langs[
                      q.questionId
                    ] ||
                    q.allowedLanguageIds?.[0] ||
                    54
                  }
                  onChange={(
                    e
                  ) =>
                    setLangs({
                      ...langs,
                      [q.questionId]:
                        Number(
                          e.target
                            .value
                        )
                    })
                  }
                  className="rounded-xl border px-3 py-2"
                >

                  {(
                    q.allowedLanguageIds ||
                    []
                  ).map(
                    (
                      langId
                    ) => (
                      <option
                        key={
                          langId
                        }
                        value={
                          langId
                        }
                      >
                        {
                          {
                            50: 'C',
                            54: 'C++',
                            62: 'Java',
                            71: 'Python 3',
                            63: 'JavaScript'
                          }[
                            langId
                          ] ||
                          `Language ${langId}`
                        }
                      </option>
                    )
                  )}

                </select>

                <Button
                  variant="brand"
                  loading={
                    running
                  }
                  onClick={
                    run
                  }
                >
                  ▶ Run code
                </Button>

                <Button
                  variant="secondary"
                  onClick={() =>
                    persist(
                      q,
                      new Set(
                        answers[
                          q.questionId
                        ] || []
                      ),
                      marked.has(
                        q.questionId
                      ),
                      codes[
                        q.questionId
                      ] || '',
                      Number(
                        langs[
                          q.questionId
                        ] ||
                          q.allowedLanguageIds?.[0] ||
                          54
                      )
                    )
                  }
                >
                  Save code
                </Button>

              </div>

              {/* CODE EDITOR */}

              <div>

                <p className="mb-2 text-sm font-semibold text-slate-600">
                  Source Code
                </p>

                <textarea
                  value={
                    codes[
                      q.questionId
                    ] ??
                    q.starterCode ??
                    ''
                  }
                  onChange={(
                    e
                  ) =>
                    setCodes({
                      ...codes,
                      [q.questionId]:
                        e.target
                          .value
                    })
                  }
                  className="min-h-[380px] w-full rounded-2xl bg-slate-950 p-4 font-mono text-sm text-slate-100 outline-none"
                  spellCheck="false"
                  placeholder="Write your solution here..."
                />

              </div>

              {/* CUSTOM INPUT */}

              <div>

                <p className="mb-2 text-sm font-semibold text-slate-600">
                  Custom Input
                </p>

                <textarea
                  value={
                    stdins[
                      q.questionId
                    ] || ''
                  }
                  onChange={(
                    e
                  ) =>
                    setStdins({
                      ...stdins,
                      [q.questionId]:
                        e.target
                          .value
                    })
                  }
                  className="min-h-[110px] w-full rounded-2xl border border-slate-300 bg-slate-50 p-4 font-mono text-sm text-slate-800 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
                  spellCheck="false"
                  placeholder={`Enter input for your program...\nExample:\n5`}
                />

                <p className="mt-1 text-xs text-slate-400">
                  This input is sent to
                  your program through
                  stdin when you click
                  Run Code.
                </p>

              </div>

              {/* COMPILER OUTPUT */}

              <div className="rounded-2xl bg-slate-950 p-4 text-sm text-slate-200">

                <p className="mb-2 font-semibold text-slate-400">
                  Compiler output
                </p>

                <pre className="min-h-16 whitespace-pre-wrap">
                  {output ||
                    'Run your code to see output.'}
                </pre>

              </div>

            </div>

          ) : (

            /* =================================================
               MCQ QUESTION
            ================================================= */

            <div className="mt-7 space-y-3">

              {(
                q.options ||
                []
              ).map(
                (o) => {

                  const selected =
                    answers[
                      q.questionId
                    ]?.has(
                      o.id
                    );

                  return (
                    <button
                      key={
                        o.id
                      }
                      onClick={() =>
                        choose(
                          o.id
                        )
                      }
                      className={`flex w-full items-center gap-3 rounded-xl border p-4 text-left transition ${
                        selected
                          ? 'border-brand-500 bg-brand-50 ring-2 ring-brand-100'
                          : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                      }`}
                    >

                      <span
                        className={`grid h-5 w-5 shrink-0 place-items-center rounded-full border text-xs ${
                          selected
                            ? 'border-brand-600 bg-brand-600 text-white'
                            : 'border-slate-300'
                        }`}
                      >
                        {selected
                          ? '✓'
                          : ''}
                      </span>

                      <span className="text-sm font-medium">
                        {
                          o.optionText
                        }
                      </span>

                    </button>
                  );
                }
              )}

            </div>
          )}

          {/* =================================================
              QUESTION CONTROLS
          ================================================= */}

          <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t pt-5">

            <div className="flex gap-2">

              <Button
                variant="secondary"
                disabled={
                  current ===
                  0
                }
                onClick={() =>
                  setCurrent(
                    (x) =>
                      x - 1
                  )
                }
              >
                ← Previous
              </Button>

              <Button
                variant="secondary"
                disabled={
                  current ===
                  qs.length -
                    1
                }
                onClick={() =>
                  setCurrent(
                    (x) =>
                      x + 1
                  )
                }
              >
                Next →
              </Button>

            </div>

            <div className="flex gap-2">

              <Button
                variant="secondary"
                onClick={
                  clear
                }
              >
                Clear
              </Button>

              <Button
                variant={
                  marked.has(
                    q.questionId
                  )
                    ? 'brand'
                    : 'secondary'
                }
                onClick={
                  toggleReview
                }
              >
                {marked.has(
                  q.questionId
                )
                  ? 'Marked for review'
                  : 'Mark for review'}
              </Button>

            </div>

          </div>

        </section>

        {/* =================================================
            RIGHT SIDEBAR
        ================================================= */}

        <aside className="h-fit space-y-5 lg:sticky lg:top-36">

          {/* =================================================
              LIVE CAMERA
          ================================================= */}

          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-slate-950 shadow-sm">

            {/* CAMERA HEADER */}

            <div className="flex items-center justify-between border-b border-slate-800 px-4 py-3">

              <div className="flex items-center gap-2">

                <span
                  className={`h-2.5 w-2.5 rounded-full ${
                    cameraEnabled
                      ? 'bg-emerald-500'
                      : 'bg-red-500'
                  }`}
                />

                <span className="text-sm font-semibold text-white">
                  Camera
                </span>

              </div>

              <span className="rounded-full bg-red-600 px-2 py-0.5 text-[10px] font-bold text-white">
                LIVE
              </span>

            </div>

            {/* VIDEO */}

            <div className="relative aspect-video w-full bg-black">

              <video
                ref={
                  videoRef
                }
                autoPlay
                muted
                playsInline
                className="h-full w-full object-cover"
              />

              {!cameraEnabled && (
                <div className="absolute inset-0 grid place-items-center bg-slate-900 text-center text-sm text-red-300">
                  Camera unavailable
                </div>
              )}

              <div className="absolute bottom-2 left-2 rounded-lg bg-black/60 px-2 py-1 text-[10px] font-medium text-white backdrop-blur">
                Student camera
              </div>

            </div>

            {/* MIC STATUS */}

            <div className="flex items-center justify-between px-4 py-3">

              <div className="flex items-center gap-2 text-xs text-slate-300">

                <span className="text-base">
                  🎙️
                </span>

                <span>
                  Microphone
                </span>

              </div>

              <span
                className={`text-xs font-semibold ${
                  microphoneEnabled
                    ? 'text-emerald-400'
                    : 'text-red-400'
                }`}
              >
                {microphoneEnabled
                  ? 'Active'
                  : 'Unavailable'}
              </span>

            </div>

          </div>

          {/* =================================================
              PROCTORING STATUS
          ================================================= */}

          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">

            <div className="flex items-center justify-between">

              <div>
                <p className="text-sm font-bold text-slate-800">
                  Proctoring
                </p>

                <p className="text-xs text-slate-400">
                  Keep this window active
                </p>
              </div>

              <span
                className={`rounded-full px-2 py-1 text-xs font-bold ${
                  warnings === 0
                    ? 'bg-emerald-100 text-emerald-700'
                    : warnings <
                      maxWarnings
                    ? 'bg-amber-100 text-amber-700'
                    : 'bg-red-100 text-red-700'
                }`}
              >
                {warnings}/
                {maxWarnings}
              </span>

            </div>

            <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100">

              <div
                className={`h-full transition-all ${
                  warnings === 0
                    ? 'bg-emerald-500'
                    : warnings === 1
                    ? 'bg-amber-400'
                    : 'bg-red-500'
                }`}
                style={{
                  width: `${Math.min(
                    100,
                    (warnings /
                      maxWarnings) *
                      100
                  )}%`
                }}
              />

            </div>

            <p className="mt-2 text-[11px] text-slate-400">
              After {maxWarnings}{' '}
              warnings, the assessment
              will be submitted
              automatically.
            </p>

            {lastViolation && (
              <div className="mt-3 rounded-lg bg-slate-50 p-2 text-[11px] text-slate-500">
                Last event: {lastViolation}
              </div>
            )}

            {!isFullscreen && (
              <Button variant="secondary" className="mt-3 w-full" onClick={enterFullscreen}>
                ⛶ Enter fullscreen
              </Button>
            )}

          </div>

          {/* =================================================
              QUESTION PALETTE
          ================================================= */}

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

            <div className="flex items-center justify-between">

              <h3 className="font-bold">
                Question palette
              </h3>

              <span className="text-xs text-slate-400">
                {answeredCount}/
                {qs.length}{' '}
                answered
              </span>

            </div>

            <div className="mt-4 grid grid-cols-5 gap-2">

              {qs.map(
                (
                  x,
                  i
                ) => {

                  const ans =
                    x.type ===
                    'CODING'
                      ? Boolean(
                          codes[
                            x.questionId
                          ]?.trim()
                        )
                      : answers[
                          x.questionId
                        ]?.size >
                        0;

                  const rev =
                    marked.has(
                      x.questionId
                    );

                  return (
                    <button
                      key={
                        x.questionId
                      }
                      onClick={() =>
                        setCurrent(
                          i
                        )
                      }
                      className={`relative h-10 rounded-lg text-sm font-semibold ${
                        i ===
                        current
                          ? 'ring-2 ring-brand-500 ring-offset-2'
                          : ''
                      }${
                        ans
                          ? 'bg-emerald-100 text-emerald-700'
                          : rev
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-slate-100 text-slate-600'
                      }`}
                    >

                      {i + 1}

                      {rev && (
                        <span className="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full bg-amber-500" />
                      )}

                    </button>
                  );
                }
              )}

            </div>

            {/* LEGEND */}

            <div className="mt-5 space-y-2 border-t pt-4 text-xs text-slate-500">

              <Legend
                c="bg-emerald-100"
                t="Answered"
              />

              <Legend
                c="bg-amber-100"
                t="Marked for review"
              />

              <Legend
                c="bg-slate-100"
                t="Not answered"
              />

            </div>

            {/* SUBMIT */}

            <Button
              variant="danger"
              className="mt-5 w-full"
              loading={
                submitting
              }
              onClick={() => {

                if (
                  remaining <=
                    0 ||
                  window.confirm(
                    `You have ${
                      qs.length -
                      answeredCount
                    } unanswered questions. Are you sure you want to submit?`
                  )
                ) {
                  doSubmit(
                    false
                  );
                }

              }}
            >
              Submit assessment
            </Button>

            <p className="mt-3 text-center text-[11px] text-slate-400">
              Started{' '}
              {dateTime(
                attempt.startedAt
              )}
            </p>

          </div>

        </aside>

      </div>

      {/* =================================================
          ERROR / WARNING MESSAGE
      ================================================= */}

      {error && (
        <div className="fixed bottom-5 left-1/2 z-50 max-w-md -translate-x-1/2 rounded-xl bg-red-600 px-4 py-3 text-center text-sm font-medium text-white shadow-xl">
          {error}
        </div>
      )}

    </div>
  );
}

/* =========================================================
   LEGEND
========================================================= */

const Legend = ({
  c,
  t
}) => (
  <div className="flex items-center gap-2">

    <span
      className={`h-3 w-3 rounded ${c}`}
    />

    {t}

  </div>
);