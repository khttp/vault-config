async function request(path, options = {}, notifyUnauthorized = true) {
  const method = (options.method || "GET").toUpperCase();
  const headers = { ...options.headers };
  if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)
      && !headers["X-XSRF-TOKEN"]) {
    const csrfResponse = await fetch("/api/auth/csrf");
    if (!csrfResponse.ok) {
      throw new Error(`Unable to prepare secure request (${csrfResponse.status})`);
    }
    const { token } = await csrfResponse.json();
    headers["X-XSRF-TOKEN"] = token;
  }

  const response = await fetch(path, {
    ...options,
    credentials: "same-origin",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
  });

  if (!response.ok) {
    if (response.status === 401 && notifyUnauthorized) {
      window.dispatchEvent(new Event("configvault:unauthorized"));
    }
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.message || body.error || message;
    } catch {
      // Keep the HTTP status message when the server has no JSON error body.
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

const json = (method, body) => ({
  method,
  body: JSON.stringify(body),
});

export const api = {
  csrf: () => request("/api/auth/csrf", {}, false),
  session: () => request("/api/auth/session", {}, false),
  login: async (credentials) => {
    const { token } = await api.csrf();
    return request(
      "/api/auth/login",
      { ...json("POST", credentials), headers: { "X-XSRF-TOKEN": token } },
      false,
    );
  },
  logout: async () => {
    const { token } = await api.csrf();
    return request(
      "/api/auth/logout",
      { method: "POST", headers: { "X-XSRF-TOKEN": token } },
    );
  },
  projects: () => request("/api/projects"),
  createProject: (body) => request("/api/projects", json("POST", body)),
  environments: (projectId) =>
    request(`/api/projects/${projectId}/environments`),
  createEnvironment: (projectId, body) =>
    request(`/api/projects/${projectId}/environments`, json("POST", body)),
  services: (projectId) => request(`/api/projects/${projectId}/services`),
  createService: (projectId, body) =>
    request(`/api/projects/${projectId}/services`, json("POST", body)),
  assignments: (projectId, environmentId) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services`,
    ),
  assignService: (projectId, environmentId, serviceId) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services`,
      json("POST", { serviceId }),
    ),
  removeAssignment: (projectId, environmentId, serviceId) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services/${serviceId}`,
      { method: "DELETE" },
    ),
  config: (projectId, environmentId, serviceId) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services/${serviceId}/config`,
    ),
  createConfig: (projectId, environmentId, serviceId, body) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services/${serviceId}/config`,
      json("POST", body),
    ),
  updateConfig: (projectId, environmentId, serviceId, key, body) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services/${serviceId}/config/${encodeURIComponent(key)}`,
      json("PUT", body),
    ),
  versions: (projectId, environmentId, serviceId, key) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services/${serviceId}/config/${encodeURIComponent(key)}/versions`,
    ),
  rollback: (projectId, environmentId, serviceId, key, version) =>
    request(
      `/api/projects/${projectId}/environments/${environmentId}/services/${serviceId}/config/${encodeURIComponent(key)}/rollback`,
      json("POST", { version }),
    ),
};
