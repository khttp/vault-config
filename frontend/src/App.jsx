import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "./api.js";

const initialProjectForm = { name: "", description: "" };

function Icon({ name, size = 18 }) {
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1.4" /><rect x="14" y="3" width="7" height="7" rx="1.4" /><rect x="3" y="14" width="7" height="7" rx="1.4" /><rect x="14" y="14" width="7" height="7" rx="1.4" /></>,
    layers: <><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 12 9 5 9-5" /><path d="m3 16 9 5 9-5" /></>,
    cube: <><path d="m12 3 9 5v8l-9 5-9-5V8l9-5Z" /><path d="m3.5 8.5 8.5 5 8.5-5M12 13.5V21" /></>,
    key: <><circle cx="8" cy="15" r="5" /><path d="m11.5 11.5 8-8L22 6l-2 2 2 2-3 3-2-2-4 4" /></>,
    copy: <><rect x="8" y="8" width="13" height="13" rx="2" /><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    chevron: <path d="m9 18 6-6-6-6" />,
    down: <path d="m7 10 5 5 5-5" />,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    refresh: <><path d="M20 7v5h-5" /><path d="M4 17v-5h5" /><path d="M5.6 9a7 7 0 0 1 11.8-2L20 12M4 12l2.6 5a7 7 0 0 0 11.8-2" /></>,
    trash: <><path d="M3 6h18M8 6V4h8v2m3 0-1 14H6L5 6m4 4v6m6-6v6" /></>,
    shield: <><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></>,
    close: <><path d="m18 6-12 12M6 6l12 12" /></>,
    eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></>,
  };
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

function SelectField({ label, value, onChange, children, disabled = false }) {
  return (
    <div className="select-field">
      {label && <span>{label}</span>}
      <div className="select-wrap">
        <select value={value} onChange={onChange} disabled={disabled}>
          {children}
        </select>
        <Icon name="down" size={15} />
      </div>
    </div>
  );
}

function Modal({ title, onClose, children }) {
  useEffect(() => {
    const closeOnEscape = (event) => event.key === "Escape" && onClose();
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="modal-header">
          <h2 id="modal-title">{title}</h2>
          <button className="icon-button" onClick={onClose} aria-label="Close dialog"><Icon name="close" /></button>
        </div>
        {children}
      </section>
    </div>
  );
}

export default function App() {
  const [authLoading, setAuthLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState("");
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [authError, setAuthError] = useState("");
  const [projects, setProjects] = useState([]);
  const [projectId, setProjectId] = useState("");
  const [environments, setEnvironments] = useState([]);
  const [environmentId, setEnvironmentId] = useState("");
  const [services, setServices] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [serviceId, setServiceId] = useState("");
  const [entries, setEntries] = useState([]);
  const [loadingProjects, setLoadingProjects] = useState(false);
  const [loadingWorkspace, setLoadingWorkspace] = useState(false);
  const [loadingConfig, setLoadingConfig] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [modal, setModal] = useState("");
  const [projectForm, setProjectForm] = useState(initialProjectForm);
  const [environmentName, setEnvironmentName] = useState("");
  const [serviceForm, setServiceForm] = useState({ name: "", description: "" });
  const [configForm, setConfigForm] = useState({ key: "", value: "", secret: true });
  const [editingKey, setEditingKey] = useState("");
  const [replacementValue, setReplacementValue] = useState("");
  const [historyEntry, setHistoryEntry] = useState(null);
  const [versions, setVersions] = useState([]);
  const [query, setQuery] = useState("");

  const project = projects.find((item) => item.id === projectId);
  const environment = environments.find((item) => item.id === environmentId);
  const service = services.find((item) => item.id === serviceId);
  const assignedIds = useMemo(() => new Set(assignments.map((item) => item.serviceId)), [assignments]);
  const visibleEntries = entries.filter((entry) =>
    entry.key.toLowerCase().includes(query.trim().toLowerCase()),
  );

  useEffect(() => {
    let active = true;
    api.session()
      .then((user) => {
        if (!active) return;
        setCurrentUser(user.username);
        setAuthenticated(true);
      })
      .catch(() => {
        if (active) setAuthenticated(false);
      })
      .finally(() => active && setAuthLoading(false));
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const requireLogin = () => {
      setAuthenticated(false);
      setCurrentUser("");
      setAuthError("Your session expired. Please sign in again.");
    };
    window.addEventListener("configvault:unauthorized", requireLogin);
    return () => window.removeEventListener("configvault:unauthorized", requireLogin);
  }, []);

  const loadProjects = useCallback(async (preferredId = "") => {
    setLoadingProjects(true);
    try {
      const result = await api.projects();
      setProjects(result);
      setProjectId((current) => {
        const wanted = preferredId || current;
        return result.some((item) => item.id === wanted) ? wanted : result[0]?.id || "";
      });
      setError("");
    } catch (caught) {
      setError(caught.message);
    } finally {
      setLoadingProjects(false);
    }
  }, []);

  useEffect(() => {
    if (authenticated) loadProjects();
  }, [authenticated, loadProjects]);

  useEffect(() => {
    let active = true;
    if (!projectId) {
      setEnvironments([]);
      setServices([]);
      setAssignments([]);
      setEnvironmentId("");
      setServiceId("");
      setEntries([]);
      return () => { active = false; };
    }

    setLoadingWorkspace(true);
    Promise.all([api.environments(projectId), api.services(projectId)])
      .then(([nextEnvironments, nextServices]) => {
        if (!active) return;
        setEnvironments(nextEnvironments);
        setServices(nextServices);
        setEnvironmentId((current) =>
          nextEnvironments.some((item) => item.id === current)
            ? current
            : nextEnvironments[0]?.id || "",
        );
        setServiceId("");
        setAssignments([]);
        setEntries([]);
        setError("");
      })
      .catch((caught) => active && setError(caught.message))
      .finally(() => active && setLoadingWorkspace(false));

    return () => { active = false; };
  }, [projectId]);

  useEffect(() => {
    let active = true;
    if (!projectId || !environmentId) {
      setAssignments([]);
      setServiceId("");
      setEntries([]);
      return () => { active = false; };
    }
    api.assignments(projectId, environmentId)
      .then((result) => {
        if (!active) return;
        setAssignments(result);
        setServiceId((current) =>
          result.some((item) => item.serviceId === current)
            ? current
            : result[0]?.serviceId || "",
        );
        setError("");
      })
      .catch((caught) => active && setError(caught.message));
    return () => { active = false; };
  }, [projectId, environmentId]);

  const refreshConfig = useCallback(async () => {
    if (!projectId || !environmentId || !serviceId) {
      setEntries([]);
      return;
    }
    setLoadingConfig(true);
    try {
      const result = await api.config(projectId, environmentId, serviceId);
      setEntries(result);
      setError("");
    } catch (caught) {
      setError(caught.message);
    } finally {
      setLoadingConfig(false);
    }
  }, [projectId, environmentId, serviceId]);

  useEffect(() => {
    refreshConfig();
  }, [refreshConfig]);

  const runAction = async (action, successMessage, after = () => {}) => {
    setError("");
    setNotice("");
    try {
      await action();
      setNotice(successMessage);
      after();
    } catch (caught) {
      setError(caught.message);
    }
  };

  const signIn = async (event) => {
    event.preventDefault();
    setAuthError("");
    try {
      const user = await api.login(loginForm);
      setCurrentUser(user.username);
      setLoginForm((current) => ({ ...current, password: "" }));
      setAuthenticated(true);
    } catch (caught) {
      setAuthError(caught.message);
    }
  };

  const signOut = async () => {
    try {
      await api.logout();
    } catch (caught) {
      setAuthError(caught.message);
    } finally {
      setAuthenticated(false);
      setCurrentUser("");
      setProjects([]);
      setEntries([]);
      setError("");
      setNotice("");
    }
  };

  const createProject = async (event) => {
    event.preventDefault();
    await runAction(async () => {
      const created = await api.createProject(projectForm);
      setProjectForm(initialProjectForm);
      setModal("");
      await loadProjects(created.id);
    }, "Project created.");
  };

  const createEnvironment = async (event) => {
    event.preventDefault();
    const name = environmentName.trim();
    await runAction(async () => {
      await api.createEnvironment(projectId, { name });
      setEnvironmentName("");
      setModal("");
      const result = await api.environments(projectId);
      setEnvironments(result);
      const created = result.find((item) => item.name === name);
      if (created) setEnvironmentId(created.id);
    }, "Environment created.");
  };

  const createService = async (event) => {
    event.preventDefault();
    await runAction(async () => {
      const created = await api.createService(projectId, serviceForm);
      setModal("");
      setServiceForm({ name: "", description: "" });
      setServices((current) => [...current, created]);
    }, "Service created.");
  };

  const assignProjectService = async (projectServiceId) => {
    const target = services.find((item) => item.id === projectServiceId);
    await runAction(async () => {
      await api.assignService(projectId, environmentId, projectServiceId);
      setAssignments(await api.assignments(projectId, environmentId));
      setServiceId(projectServiceId);
    }, `${target?.name || "Service"} assigned to ${environment?.name || "environment"}.`);
  };

  const copyConfigUrl = async (entry) => {
    const path = `/api/projects/${projectId}/environments/${environmentId}/services/${serviceId}/config/${encodeURIComponent(entry.key)}`;
    await runAction(
      () => navigator.clipboard.writeText(`${window.location.origin}${path}`),
      `URL for ${entry.key} copied.`,
    );
  };

  const removeService = async (assignedServiceId) => {
    const target = services.find((item) => item.id === assignedServiceId);
    if (!window.confirm(`Remove ${target?.name || "this service"} from ${environment?.name || "this environment"}? Its config entries will be deleted.`)) return;
    await runAction(async () => {
      await api.removeAssignment(projectId, environmentId, assignedServiceId);
      setAssignments(await api.assignments(projectId, environmentId));
      if (serviceId === assignedServiceId) setEntries([]);
    }, "Service removed from environment.");
  };

  const createConfig = async (event) => {
    event.preventDefault();
    await runAction(async () => {
      await api.createConfig(projectId, environmentId, serviceId, configForm);
      setConfigForm({ key: "", value: "", secret: true });
      setModal("");
      await refreshConfig();
    }, "Configuration entry created.");
  };

  const updateConfig = async (event) => {
    event.preventDefault();
    await runAction(async () => {
      await api.updateConfig(projectId, environmentId, serviceId, editingKey, {
        value: replacementValue,
      });
      setEditingKey("");
      setReplacementValue("");
      await refreshConfig();
    }, "New version saved.");
  };

  const showHistory = async (entry) => {
    setHistoryEntry(entry);
    setVersions([]);
    setModal("history");
    try {
      setVersions(await api.versions(projectId, environmentId, serviceId, entry.key));
    } catch (caught) {
      setError(caught.message);
    }
  };

  const rollback = async (version) => {
    if (!window.confirm(`Restore version ${version.version} of ${historyEntry.key}? This creates a new version.`)) return;
    await runAction(async () => {
      await api.rollback(
        projectId,
        environmentId,
        serviceId,
        historyEntry.key,
        version.version,
      );
      setModal("");
      await refreshConfig();
    }, `Version ${version.version} restored as a new version.`);
  };

  if (authLoading) {
    return <main className="login-page"><div className="login-card"><span className="brand-mark"><Icon name="layers" size={21} /></span><p>Checking your session…</p></div></main>;
  }

  if (!authenticated) {
    return (
      <main className="login-page">
        <form className="login-card" onSubmit={signIn}>
          <span className="brand-mark"><Icon name="layers" size={21} /></span>
          <div className="eyebrow"><span className="eyebrow-line" /> CONFIGURATION WORKSPACE</div>
          <h1>Welcome back<span className="title-period">.</span></h1>
          <p>Sign in to manage your ConfigVault workspace.</p>
          {authError && <div className="login-error" role="alert">{authError}</div>}
          <label>Username<input autoFocus autoComplete="username" value={loginForm.username} onChange={(event) => setLoginForm({ ...loginForm, username: event.target.value })} required /></label>
          <label>Password<input type="password" autoComplete="current-password" value={loginForm.password} onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })} required /></label>
          <button className="button button-primary" type="submit">Sign in</button>
        </form>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="ConfigVault home">
          <span className="brand-mark"><Icon name="layers" size={21} /></span>
          <span className="brand-copy"><strong>config<span>vault</span></strong><small>CONFIG CONTROL PLANE</small></span>
        </a>
        <div className="side-section-label">WORKSPACE</div>
        <button className="nav-link active"><Icon name="grid" /><span>Overview</span><span className="nav-indicator" /></button>
        <div className="sidebar-bottom">
          <div className="local-status"><span className="status-dot" /><div><strong>Local API</strong><span>Connected through proxy</span></div></div>
          <a className="api-docs-link" href="/swagger-ui/index.html" target="_blank" rel="noreferrer">API documentation <Icon name="chevron" size={15} /></a>
          <div className="sidebar-version">CONFIGVAULT <span>0.1.0</span></div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><Icon name="chevron" size={14} /><strong>{project?.name || "Overview"}</strong></div>
          <div className="topbar-right"><span className="local-badge"><span className="status-dot" /> LOCAL</span><span className="avatar" title={currentUser}>{currentUser.slice(0, 2).toUpperCase()}</span><button className="text-button" onClick={signOut}>Sign out</button></div>
        </header>

        <div className="content">
          <section className="page-heading">
            <div>
              <div className="eyebrow"><span className="eyebrow-line" /> CONFIGURATION WORKSPACE</div>
              <h1>Overview<span className="title-period">.</span></h1>
              <p>Manage project services and their environment configuration.</p>
            </div>
            <button className="button button-primary" onClick={() => setModal("project")}><Icon name="plus" size={17} /> New project</button>
          </section>

          {(error || notice) && (
            <div className={`toast ${error ? "toast-error" : "toast-success"}`} role="status">
              <span>{error || notice}</span>
              <button className="icon-button" onClick={() => { setError(""); setNotice(""); }} aria-label="Dismiss message"><Icon name="close" size={16} /></button>
            </div>
          )}

          <section className="selector-panel">
            <div className="selector-heading">
              <div className="section-kicker">SELECT SCOPE</div>
              <span className="scope-hint">Choose where to view configuration</span>
            </div>
            <div className="selectors">
              <SelectField label="PROJECT" value={projectId} onChange={(event) => {
                setProjectId(event.target.value);
                setEnvironmentId("");
                setServiceId("");
                setAssignments([]);
                setEntries([]);
              }} disabled={loadingProjects || !projects.length}>
                {!projects.length && <option value="">No projects yet</option>}
                {projects.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </SelectField>
              <SelectField label="ENVIRONMENT" value={environmentId} onChange={(event) => {
                setEnvironmentId(event.target.value);
                setServiceId("");
                setAssignments([]);
                setEntries([]);
              }} disabled={!projectId || loadingWorkspace || !environments.length}>
                {!environments.length && <option value="">No environments</option>}
                {environments.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </SelectField>
              <SelectField label="SERVICE" value={serviceId} onChange={(event) => setServiceId(event.target.value)} disabled={!environmentId || !assignments.length}>
                {!assignments.length && <option value="">No assigned services</option>}
                {assignments.map((assignment) => {
                  const item = services.find((serviceItem) => serviceItem.id === assignment.serviceId);
                  return item ? <option key={item.id} value={item.id}>{item.name}</option> : null;
                })}
              </SelectField>
            </div>
          </section>

          <section className="stat-grid">
            <div className="stat-card"><div className="stat-icon"><Icon name="layers" /></div><div className="stat-meta">PROJECTS</div><div className="stat-value">{loadingProjects ? "—" : projects.length}</div><div className="stat-foot">in this workspace</div></div>
            <div className="stat-card"><div className="stat-icon"><Icon name="grid" /></div><div className="stat-meta">ENVIRONMENTS</div><div className="stat-value">{environments.length}</div><div className="stat-foot">{project?.name || "selected project"}</div></div>
            <div className="stat-card"><div className="stat-icon"><Icon name="cube" /></div><div className="stat-meta">RUNNING SERVICES</div><div className="stat-value">{assignments.length}</div><div className="stat-foot">{environment?.name || "selected environment"}</div></div>
            <div className="stat-card"><div className="stat-icon"><Icon name="key" /></div><div className="stat-meta">CONFIG ENTRIES</div><div className="stat-value">{entries.length}</div><div className="stat-foot">{service?.name || "selected service"}</div></div>
          </section>

          <div className="workspace-grid">
            <section className="card config-card">
              <div className="card-heading">
                <div>
                  <div className="section-kicker">SERVICE CONFIGURATION</div>
                  <h2>{service ? service.name : "Select a service"}<span className="heading-separator">/</span><span className="heading-muted">{environment?.name || "environment"}</span></h2>
                </div>
                <div className="card-actions">
                  <button className="button button-quiet button-square" onClick={refreshConfig} disabled={!serviceId || loadingConfig} aria-label="Refresh configuration"><Icon name="refresh" /></button>
                  <button className="button button-primary button-small" onClick={() => setModal("config")} disabled={!serviceId}><Icon name="plus" size={16} /> Add config</button>
                </div>
              </div>
              <div className="table-toolbar">
                <span>{service ? `${entries.length} entries` : "Pick a project, environment, and assigned service"}</span>
                <label className="search-box"><Icon name="search" size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Filter keys" aria-label="Filter config keys" /></label>
              </div>
              <div className="config-table-wrap">
                <table className="config-table">
                  <thead><tr><th>KEY</th><th>VALUE</th><th>TYPE</th><th>VERSION</th><th /></tr></thead>
                  <tbody>
                    {loadingConfig && <tr><td className="table-empty" colSpan="5"><span className="spinner" /> Loading configuration…</td></tr>}
                    {!loadingConfig && !visibleEntries.length && (
                      <tr><td className="table-empty" colSpan="5">
                        <div className="empty-icon"><Icon name="key" size={21} /></div>
                        <strong>{service ? "No configuration yet" : "No service selected"}</strong>
                        <span>{service ? "Add the first key-value pair for this service." : "Assign a service to this environment to begin."}</span>
                      </td></tr>
                    )}
                    {!loadingConfig && visibleEntries.map((entry) => (
                      <tr key={entry.id}>
                        <td><span className="config-key-cell"><code className="config-key">{entry.key}</code><button className="copy-url-button" onClick={() => copyConfigUrl(entry)} title={`Copy URL for ${entry.key}`} aria-label={`Copy URL for ${entry.key}`}><Icon name="copy" size={14} /></button></span></td>
                        <td>
                          {editingKey === entry.key ? (
                            <form className="inline-edit" onSubmit={updateConfig}>
                              <input autoFocus type="password" value={replacementValue} onChange={(event) => setReplacementValue(event.target.value)} placeholder={entry.secret ? "Enter replacement secret" : "New value"} required />
                              <button className="button button-primary button-small" type="submit">Save</button>
                              <button className="text-button" type="button" onClick={() => { setEditingKey(""); setReplacementValue(""); }}>Cancel</button>
                            </form>
                          ) : (
                            <span className={`config-value ${entry.secret ? "is-secret" : ""}`}>
                              {entry.secret && <Icon name="shield" size={14} />}
                              {entry.value}
                            </span>
                          )}
                        </td>
                        <td><span className={`type-pill ${entry.secret ? "type-secret" : "type-plain"}`}>{entry.secret ? "SECRET" : "PLAIN"}</span></td>
                        <td><button className="version-link" onClick={() => showHistory(entry)}>v{entry.version} <Icon name="clock" size={14} /></button></td>
                        <td className="row-actions">{editingKey !== entry.key && <button className="text-button" onClick={() => { setEditingKey(entry.key); setReplacementValue(""); }}>Update</button>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="table-footer"><span><span className="secure-dot" /> Values encrypted at rest</span><span>{environment?.name && service ? `${environment.name} · ${service.name}` : "—"}</span></div>
            </section>

            <aside className="right-column">
              <section className="card environment-card">
                <div className="card-heading compact-heading"><div><div className="section-kicker">ENVIRONMENT</div><h2>{environment?.name || "No environment selected"}</h2></div><span className="environment-mark"><Icon name="grid" size={18} /></span></div>
                <div className="environment-details">
                  <div className="detail-row"><span>Project</span><strong>{project?.name || "—"}</strong></div>
                  <div className="detail-row"><span>Services running</span><strong>{assignments.length}</strong></div>
                  <div className="detail-row"><span>Configuration keys</span><strong>{entries.length}</strong></div>
                </div>
                <div className="assigned-heading"><span className="section-kicker">PROJECT SERVICES</span><button className="text-button add-link" onClick={() => setModal("service")} disabled={!projectId}><Icon name="plus" size={14} /> Register</button></div>
                <div className="assigned-list">
                  {!services.length && <div className="assigned-empty">No services in this project. Register one to get started.</div>}
                  {services.map((item) => {
                    const isAssigned = assignedIds.has(item.id);
                    return <div key={item.id} className={`assigned-item ${isAssigned && serviceId === item.id ? "selected" : ""}`}>
                      <button className="service-select" onClick={() => isAssigned && setServiceId(item.id)} disabled={!isAssigned} aria-label={isAssigned ? `Select ${item.name}` : `${item.name} is not assigned to this environment`}>
                        <span className="service-mini-icon"><Icon name="cube" size={16} /></span><span className="assigned-name">{item.name}</span>
                      </button>
                      {isAssigned ? <>
                        <span className="service-running"><i /> ASSIGNED</span>
                        <button className="remove-assignment visible-action" onClick={() => removeService(item.id)} aria-label={`Remove ${item.name} from ${environment?.name}`}><Icon name="trash" size={14} /></button>
                      </> : <>
                        <span className="service-unassigned">NOT ASSIGNED</span>
                        <button className="button button-quiet button-small assign-button" onClick={() => assignProjectService(item.id)} disabled={!environmentId}>Assign</button>
                      </>}
                    </div>;
                  })}
                </div>
              </section>
              <section className="card quick-card">
                <div className="section-kicker">QUICK ACTIONS</div>
                <button className="quick-action" onClick={() => setModal("environment")} disabled={!projectId}><span className="quick-icon"><Icon name="plus" size={16} /></span><span><strong>New environment</strong><small>Add an environment to this project</small></span><Icon name="chevron" size={16} /></button>
                <button className="quick-action" onClick={() => setModal("service")} disabled={!projectId}><span className="quick-icon"><Icon name="cube" size={16} /></span><span><strong>Register service</strong><small>Add a service to this project</small></span><Icon name="chevron" size={16} /></button>
                <button className="quick-action" onClick={() => window.open("/swagger-ui/index.html", "_blank", "noopener,noreferrer")}><span className="quick-icon"><Icon name="eye" size={16} /></span><span><strong>Explore API</strong><small>Open interactive API docs</small></span><Icon name="chevron" size={16} /></button>
              </section>
              <div className="security-note"><span className="security-icon"><Icon name="shield" size={17} /></span><p><strong>Secret values are protected</strong><span>Secrets are encrypted at rest and masked in this dashboard. This local API is not authenticated yet.</span></p></div>
            </aside>
          </div>
          <footer className="page-footer"><span>CONFIGVAULT <b>·</b> LOCAL CONTROL PLANE</span><span>Changes apply to the selected service and environment.</span></footer>
        </div>
      </main>

      {modal === "project" && <Modal title="Create project" onClose={() => setModal("")}>
        <form className="modal-form" onSubmit={createProject}>
          <p className="modal-description">Projects contain the services and environments for one application.</p>
          <label>Project name<input autoFocus value={projectForm.name} onChange={(event) => setProjectForm({ ...projectForm, name: event.target.value })} placeholder="e.g. commerce" required maxLength="120" /></label>
          <label>Description <span className="optional">OPTIONAL</span><textarea value={projectForm.description} onChange={(event) => setProjectForm({ ...projectForm, description: event.target.value })} placeholder="What does this project contain?" maxLength="2000" rows="3" /></label>
          <div className="modal-actions"><button type="button" className="button button-quiet" onClick={() => setModal("")}>Cancel</button><button className="button button-primary" type="submit"><Icon name="plus" size={16} /> Create project</button></div>
        </form>
      </Modal>}

      {modal === "environment" && <Modal title="Add environment" onClose={() => setModal("")}>
        <form className="modal-form" onSubmit={createEnvironment}>
          <p className="modal-description">Create an environment under <strong>{project?.name}</strong>. Services can be assigned to it afterward.</p>
          <label>Environment name<input autoFocus value={environmentName} onChange={(event) => setEnvironmentName(event.target.value)} placeholder="e.g. staging" required maxLength="80" /></label>
          <div className="modal-actions"><button type="button" className="button button-quiet" onClick={() => setModal("")}>Cancel</button><button className="button button-primary" type="submit"><Icon name="plus" size={16} /> Create environment</button></div>
        </form>
      </Modal>}

      {modal === "service" && <Modal title="Register service" onClose={() => setModal("")}>
        <form className="modal-form" onSubmit={createService}>
          <p className="modal-description">Register a service within <strong>{project?.name}</strong>, then assign it to one or more environments.</p>
          <label>Service name<input autoFocus value={serviceForm.name} onChange={(event) => setServiceForm({ ...serviceForm, name: event.target.value })} placeholder="e.g. payments" required maxLength="120" /></label>
          <label>Description <span className="optional">OPTIONAL</span><textarea value={serviceForm.description} onChange={(event) => setServiceForm({ ...serviceForm, description: event.target.value })} placeholder="What does this service do?" maxLength="2000" rows="3" /></label>
          <div className="modal-actions"><button type="button" className="button button-quiet" onClick={() => setModal("")}>Cancel</button><button className="button button-primary" type="submit"><Icon name="plus" size={16} /> Register service</button></div>
        </form>
      </Modal>}

      {modal === "config" && <Modal title="Add configuration entry" onClose={() => setModal("")}>
        <form className="modal-form" onSubmit={createConfig}>
          <p className="modal-description">Add a setting to <strong>{service?.name}</strong> in <strong>{environment?.name}</strong>. Secret values are encrypted before storage.</p>
          <label>Configuration key<input autoFocus value={configForm.key} onChange={(event) => setConfigForm({ ...configForm, key: event.target.value })} placeholder="e.g. PAYMENT_API_KEY" required maxLength="255" pattern="[A-Za-z_][A-Za-z0-9_.-]*" /></label>
          <label>Value<input type={configForm.secret ? "password" : "text"} value={configForm.value} onChange={(event) => setConfigForm({ ...configForm, value: event.target.value })} placeholder={configForm.secret ? "Enter secret value" : "Enter config value"} required maxLength="65536" /></label>
          <label className="checkbox-row"><input type="checkbox" checked={configForm.secret} onChange={(event) => setConfigForm({ ...configForm, secret: event.target.checked })} /><span><strong>Mark as secret</strong><small>Mask in responses and protect as sensitive data</small></span><Icon name="shield" size={17} /></label>
          <div className="modal-actions"><button type="button" className="button button-quiet" onClick={() => setModal("")}>Cancel</button><button className="button button-primary" type="submit"><Icon name="plus" size={16} /> Save config</button></div>
        </form>
      </Modal>}

      {modal === "history" && <Modal title={`Version history · ${historyEntry?.key || ""}`} onClose={() => setModal("")}>
        <div className="history-list">
          <p className="modal-description">Stored values are not shown here. Restoring a version creates a new current version.</p>
          {!versions.length && <div className="history-empty">No version history available.</div>}
          {versions.map((version) => <div className="history-row" key={version.id}><span className="history-version">v{version.version}</span><span className="history-details"><strong>{version.createdBy}</strong><small>{new Date(version.createdAt).toLocaleString()}</small></span><button className="button button-quiet button-small" disabled={version.version === historyEntry?.version} onClick={() => rollback(version)}>{version.version === historyEntry?.version ? "Current" : "Restore"}</button></div>)}
        </div>
      </Modal>}
    </div>
  );
}
