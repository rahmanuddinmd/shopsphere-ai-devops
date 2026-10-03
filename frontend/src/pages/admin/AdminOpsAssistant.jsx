import { useState } from 'react';
import { api } from '../../api/client.js';
import ErrorMessage from '../../components/ErrorMessage.jsx';
import { SparkIcon } from '../../components/Icons.jsx';

const SOURCES = ['jenkins', 'kubectl', 'docker', 'terraform', 'aws', 'trivy', 'sonarqube', 'maven'];

const SAMPLE = `$ kubectl get pods -n shopsphere-dev
NAME                       READY   STATUS             RESTARTS   AGE
backend-7d9f6c8b5d-x2k4p   0/1     CrashLoopBackOff   5          4m
$ kubectl logs backend-7d9f6c8b5d-x2k4p -n shopsphere-dev --previous
org.postgresql.util.PSQLException: Connection to postgres:5432 refused.`;

/** AI DevOps Assistant: analyses logs and recommends fixes. It never executes commands. */
export default function AdminOpsAssistant() {
  const [source, setSource] = useState('kubectl');
  const [log, setLog] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function analyze(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      setResult(await api.analyzeLog(source, log));
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div>
        <h1 className="page-title" style={{ fontSize: 36 }}>AI DevOps Assistant</h1>
        <p className="muted">Paste a Jenkins, Docker, Kubernetes, Terraform, Trivy, SonarQube or AWS error. Secrets are redacted before analysis. Nothing is executed.</p>
      </div>
      <form className="panel stack" onSubmit={analyze}>
        <div className="row" style={{ alignItems: 'flex-end' }}>
          <div className="field" style={{ minWidth: 200 }}>
            <label htmlFor="source">Log source</label>
            <select id="source" className="select" value={source} onChange={(e) => setSource(e.target.value)}>
              {SOURCES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => { setSource('kubectl'); setLog(SAMPLE); }}>Load sample</button>
        </div>
        <div className="field">
          <label htmlFor="log">Log / error output</label>
          <textarea id="log" className="textarea" value={log} onChange={(e) => setLog(e.target.value)} required maxLength={60000} />
        </div>
        <button type="submit" className="btn btn-primary" style={{ alignSelf: 'flex-start' }} disabled={busy || !log.trim()}>
          <SparkIcon /> {busy ? 'Analysing…' : 'Analyse'}
        </button>
      </form>
      <ErrorMessage error={error} />
      {result && (
        <section className="panel stack" aria-label="Analysis">
          <div className="row between">
            <h2 style={{ fontSize: 22 }}>{result.problem}</h2>
            <span className="pill pill-gray">AI: {result.provider}</span>
          </div>
          {result.redacted && <div className="alert alert-warn">Secrets were found in the log and redacted before analysis.</div>}
          <div><b>Root cause</b><p>{result.rootCause}</p></div>
          {result.evidence.length > 0 && (
            <div><b>Evidence</b><pre className="mono panel panel-surface" style={{ whiteSpace: 'pre-wrap', margin: '6px 0 0' }}>{result.evidence.join('\n')}</pre></div>
          )}
          <div><b>Recommended fix</b><p>{result.recommendedFix}</p></div>
          {result.commands.length > 0 && (
            <div className="stack-sm">
              <b>Commands (run them yourself)</b>
              {result.commands.map((c) => (
                <div key={c.command} className="panel panel-surface stack-sm" style={{ padding: 12 }}>
                  <code className="mono">{c.command}</code>
                  <span className="muted small">{c.purpose}</span>
                  {c.requiresConfirmation && <span className="pill pill-red" style={{ alignSelf: 'flex-start' }}>Destructive — requires explicit confirmation</span>}
                </div>
              ))}
            </div>
          )}
          <div><b>Risk</b><p>{result.risk}</p></div>
          <div><b>Verification</b><ul style={{ margin: '6px 0 0' }}>{result.verification.map((v) => <li key={v}>{v}</li>)}</ul></div>
          {result.aiExplanation && (
            <div><b>AI explanation</b><pre className="panel panel-surface" style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', margin: '6px 0 0' }}>{result.aiExplanation}</pre></div>
          )}
        </section>
      )}
    </>
  );
}
