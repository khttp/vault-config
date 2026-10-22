async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
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
