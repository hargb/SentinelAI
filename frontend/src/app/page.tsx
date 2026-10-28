"use client";

import { useCallback, useEffect, useState } from "react";

const API_URL = (
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const navigation = [
  "Overview",
  "Policy Playground",
  "Audit Logs",
  "Agent Registry",
] as const;

type PageName = (typeof navigation)[number];
type ApiStatus = "Checking..." | "Connected" | "Unavailable";

type PolicyResult = {
  id?: number;
  agentId: string;
  action: string;
  decision: string;
  reason: string;
};

type AuditLog = {
  id: number;
  agentId: string;
  action: string;
  decision: string;
  reason: string;
  createdAt?: string;
  timestamp?: string;
};

type Approval = {
  id: number;
  agentId: string;
  action: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  requestedAt?: string;
  reviewedAt?: string | null;
};

const agentOptions = ["reader-agent", "analyst-agent", "payment-agent"];

const actionOptions = ["READ_DOCUMENT", "CALCULATE", "MAKE_PAYMENT"];

async function readError(response: Response): Promise<string> {
  if (response.status === 401) {
    return "Authentication failed. Check your API credentials.";
  }

  if (response.status === 403) {
    return "Access denied. Check permissions or CSRF configuration.";
  }

  if (response.status === 404) {
    return "API endpoint not found. Check the Spring Boot route.";
  }

  try {
    const data = await response.json();
    return data.message || data.error || `Request failed (${response.status}).`;
  } catch {
    return `Request failed (${response.status}).`;
  }
}

function formatDate(value?: string | null): string {
  if (!value) return "—";

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function StatusBadge({ value }: { value: string }) {
  const color =
    value === "ALLOW" || value === "APPROVED"
      ? "border-emerald-400/20 bg-emerald-400/10 text-emerald-300"
      : value === "DENY" || value === "REJECTED"
        ? "border-red-400/20 bg-red-400/10 text-red-300"
        : value === "PENDING" || value === "APPROVAL_REQUIRED"
          ? "border-amber-400/20 bg-amber-400/10 text-amber-300"
          : "border-white/10 bg-white/5 text-slate-300";

  return (
    <span
      className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${color}`}
    >
      {value}
    </span>
  );
}

function Panel({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-white/10 bg-[#101622] p-5 sm:p-6">
      <h3 className="text-lg font-semibold">{title}</h3>
      {description && (
        <p className="mt-1 text-sm leading-6 text-slate-400">{description}</p>
      )}
      <div className="mt-5">{children}</div>
    </section>
  );
}

export default function Dashboard() {
  const [active, setActive] = useState<PageName>("Overview");
  const [apiStatus, setApiStatus] = useState<ApiStatus>("Checking...");

  // Enter Base64(username:password), not the raw password.
  const [credentialInput, setCredentialInput] = useState("");
  const [showCredentials, setShowCredentials] = useState(false);

  const [agentId, setAgentId] = useState("reader-agent");
  const [action, setAction] = useState("READ_DOCUMENT");
  const [policyLoading, setPolicyLoading] = useState(false);
  const [policyResult, setPolicyResult] = useState<PolicyResult | null>(null);
  const [policyError, setPolicyError] = useState("");
  const [policySuccess, setPolicySuccess] = useState("");

  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditError, setAuditError] = useState("");

  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [approvalLoading, setApprovalLoading] = useState(false);
  const [approvalError, setApprovalError] = useState("");
  const [approvalMessage, setApprovalMessage] = useState("");
  const [reviewingId, setReviewingId] = useState<number | null>(null);

  const [refreshKey, setRefreshKey] = useState(0);

  const authHeader = credentialInput.trim()
    ? `Basic ${credentialInput.trim().replace(/^Basic\s+/i, "")}`
    : "";

  const makeHeaders = useCallback(
    (json = false): HeadersInit => {
      const headers: Record<string, string> = {};

      if (json) headers["Content-Type"] = "application/json";
      if (authHeader) headers.Authorization = authHeader;

      return headers;
    },
    [authHeader],
  );

  const checkBackend = useCallback(async (signal?: AbortSignal) => {
    try {
      const response = await fetch(`${API_URL}/actuator/health`, {
        signal,
        cache: "no-store",
      });

      if (!response.ok) throw new Error("Health check failed");

      const data: { status?: string } = await response.json();
      setApiStatus(data.status === "UP" ? "Connected" : "Unavailable");
    } catch {
      if (!signal?.aborted) setApiStatus("Unavailable");
    }
  }, []);

  const loadAuditLogs = useCallback(
    async (signal?: AbortSignal) => {
      setAuditLoading(true);
      setAuditError("");

      try {
        const response = await fetch(`${API_URL}/api/v1/audit-logs`, {
          method: "GET",
          headers: makeHeaders(),
          signal,
          cache: "no-store",
        });

        if (!response.ok) throw new Error(await readError(response));

        const data: AuditLog[] | AuditLog | null = await response.json();
        setAuditLogs(Array.isArray(data) ? data : data ? [data] : []);
      } catch (error) {
        if (!signal?.aborted) {
          setAuditError(
            error instanceof Error
              ? error.message
              : "Could not load audit logs.",
          );
        }
      } finally {
        if (!signal?.aborted) setAuditLoading(false);
      }
    },
    [makeHeaders],
  );

  const loadApprovals = useCallback(
    async (signal?: AbortSignal) => {
      setApprovalLoading(true);
      setApprovalError("");

      try {
        const response = await fetch(`${API_URL}/api/v1/approvals`, {
          method: "GET",
          headers: makeHeaders(),
          signal,
          cache: "no-store",
        });

        if (!response.ok) throw new Error(await readError(response));

        const data: Approval[] | Approval | null = await response.json();
        setApprovals(Array.isArray(data) ? data : data ? [data] : []);
      } catch (error) {
        if (!signal?.aborted) {
          setApprovalError(
            error instanceof Error
              ? error.message
              : "Could not load approvals.",
          );
        }
      } finally {
        if (!signal?.aborted) setApprovalLoading(false);
      }
    },
    [makeHeaders],
  );

  useEffect(() => {
    const controller = new AbortController();
    void checkBackend(controller.signal);
    return () => controller.abort();
  }, [checkBackend]);

  useEffect(() => {
    if (active !== "Audit Logs") return;

    const controller = new AbortController();
    void loadAuditLogs(controller.signal);
    return () => controller.abort();
  }, [active, loadAuditLogs, refreshKey]);

  useEffect(() => {
    if (active !== "Overview" && active !== "Agent Registry") return;

    const controller = new AbortController();
    void loadApprovals(controller.signal);
    return () => controller.abort();
  }, [active, loadApprovals, refreshKey]);

  async function evaluatePolicy(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPolicyLoading(true);
    setPolicyError("");
    setPolicySuccess("");
    setPolicyResult(null);

    try {
      const response = await fetch(`${API_URL}/api/v1/policies/evaluate`, {
        method: "POST",
        headers: makeHeaders(true),
        body: JSON.stringify({ agentId, action }),
      });

      if (!response.ok) throw new Error(await readError(response));

      const result: PolicyResult = await response.json();
      setPolicyResult(result);

      if (result.decision === "APPROVAL_REQUIRED") {
        setPolicySuccess(
          "Approval request submitted. Check the approvals list for its status.",
        );
      } else {
        setPolicySuccess("Policy evaluation completed successfully.");
      }

      setRefreshKey((key) => key + 1);
    } catch (error) {
      setPolicyError(
        error instanceof Error ? error.message : "Policy evaluation failed.",
      );
    } finally {
      setPolicyLoading(false);
    }
  }

  async function reviewApproval(id: number, decision: "approve" | "reject") {
    setReviewingId(id);
    setApprovalError("");
    setApprovalMessage("");

    try {
      const response = await fetch(
        `${API_URL}/api/v1/approvals/${id}/${decision}`,
        {
          method: "POST",
          headers: makeHeaders(),
        },
      );

      if (!response.ok) throw new Error(await readError(response));

      const updated: Approval = await response.json();

      setApprovals((current) =>
        current.map((item) => (item.id === id ? updated : item)),
      );

      setApprovalMessage(`Request #${id} ${decision}ed successfully.`);
      setRefreshKey((key) => key + 1);
    } catch (error) {
      setApprovalError(
        error instanceof Error ? error.message : "Could not review approval.",
      );
    } finally {
      setReviewingId(null);
    }
  }

  const isConnected = apiStatus === "Connected";
  const pendingApprovals = approvals.filter(
    (approval) => approval.status === "PENDING",
  ).length;

  const stats = [
    {
      label: "Total Agents",
      value: "3",
      detail: "Configured policy agents",
    },
    {
      label: "Policies",
      value: "3",
      detail: "Permission-based action checks",
    },
    {
      label: "Actions Reviewed",
      value: auditLogs.length.toLocaleString(),
      detail: "Records loaded from backend",
    },
    {
      label: "Pending Approvals",
      value: pendingApprovals.toString().padStart(2, "0"),
      detail: "Live approval requests",
    },
  ];

  return (
    <main className="min-h-screen bg-[#080b12] text-slate-100 md:flex">
      <aside className="border-b border-white/10 bg-[#0c1019] p-5 md:min-h-screen md:w-72 md:border-b-0 md:border-r">
        <div className="mb-9 flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-400 text-lg font-black text-slate-950">
            S
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-wide">SentinelAI</h1>
            <p className="text-xs text-slate-500">Agent Governance</p>
          </div>
        </div>

        <p className="mb-3 text-xs uppercase tracking-[0.2em] text-slate-500">
          Workspace
        </p>

        <nav className="flex flex-wrap gap-2 md:flex-col">
          {navigation.map((item) => (
            <button
              key={item}
              onClick={() => setActive(item)}
              aria-current={active === item ? "page" : undefined}
              className={`rounded-lg px-4 py-3 text-left text-sm transition ${
                active === item
                  ? "bg-cyan-400/10 font-semibold text-cyan-300 ring-1 ring-cyan-400/20"
                  : "text-slate-400 hover:bg-white/5 hover:text-white"
              }`}
            >
              {item}
            </button>
          ))}
        </nav>

        <div className="mt-8 rounded-xl border border-white/10 bg-white/[0.03] p-4">
          <div className="mb-2 flex items-center gap-2">
            <span
              className={`h-2 w-2 rounded-full ${
                apiStatus === "Checking..."
                  ? "bg-amber-400"
                  : isConnected
                    ? "bg-emerald-400"
                    : "bg-red-400"
              }`}
            />
            <span className="text-sm font-medium">API connection</span>
          </div>
          <p
            className={`text-sm font-semibold ${
              isConnected
                ? "text-emerald-400"
                : apiStatus === "Unavailable"
                  ? "text-red-400"
                  : "text-amber-400"
            }`}
          >
            {apiStatus}
          </p>
          <p className="mt-2 text-xs leading-5 text-slate-500">
            {isConnected
              ? "Spring Boot health endpoint is responding."
              : apiStatus === "Checking..."
                ? "Checking backend health..."
                : "Backend health check failed. Check Spring Boot and CORS."}
          </p>
          <button
            onClick={() => void checkBackend()}
            className="mt-3 text-xs font-semibold text-cyan-300 hover:text-cyan-200"
          >
            Check connection again
          </button>
        </div>

        <div className="mt-4 rounded-xl border border-white/10 bg-white/[0.03] p-4">
          <label
            htmlFor="api-credentials"
            className="text-xs font-medium text-slate-300"
          >
            API Basic Auth
          </label>
          <input
            id="api-credentials"
            type={showCredentials ? "text" : "password"}
            value={credentialInput}
            onChange={(event) => setCredentialInput(event.target.value)}
            placeholder="Base64 credentials only"
            autoComplete="off"
            className="mt-3 w-full rounded-lg border border-white/10 bg-[#080b12] px-3 py-2 text-xs outline-none focus:border-cyan-400"
          />
          <label className="mt-3 flex items-center gap-2 text-xs text-slate-400">
            <input
              type="checkbox"
              checked={showCredentials}
              onChange={(event) => setShowCredentials(event.target.checked)}
            />
            Show input
          </label>
          <p className="mt-2 text-xs leading-5 text-slate-500">
            Enter Base64 of username:password. This field is not saved
            persistently.
          </p>
        </div>
      </aside>

      <section className="min-w-0 flex-1 p-5 md:p-9">
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="mb-2 text-sm text-slate-500">Workspace / {active}</p>
            <h2 className="text-3xl font-bold tracking-tight">
              {active === "Overview" ? "Security Overview" : active}
            </h2>
            <p className="mt-2 text-sm text-slate-400">
              Monitor and govern your AI agent operations.
            </p>
          </div>
          <div className="rounded-full border border-white/10 bg-white/[0.03] px-4 py-2 text-sm text-slate-300">
            Admin Workspace
          </div>
        </header>

        {active === "Overview" && (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {stats.map((stat) => (
                <div
                  key={stat.label}
                  className="rounded-2xl border border-white/10 bg-[#101622] p-5"
                >
                  <p className="text-sm text-slate-400">{stat.label}</p>
                  <p className="my-4 text-3xl font-bold tracking-tight">
                    {stat.value}
                  </p>
                  <p className="text-xs text-slate-500">{stat.detail}</p>
                </div>
              ))}
            </div>

            <div className="mt-6 grid gap-5 xl:grid-cols-[1.4fr_1fr]">
              <Panel
                title="Governance workflow"
                description="How SentinelAI evaluates agent actions"
              >
                <div className="space-y-5">
                  {[
                    "Agent requests an action",
                    "Policy engine checks agent permissions",
                    "Decision and reason are recorded",
                    "Sensitive actions wait for human approval",
                  ].map((step, index) => (
                    <div key={step} className="flex items-center gap-4">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-cyan-400/10 text-sm text-cyan-300">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                      <p className="text-sm text-slate-300">{step}</p>
                    </div>
                  ))}
                </div>
                <button
                  onClick={() => setActive("Policy Playground")}
                  className="mt-7 rounded-lg bg-cyan-400 px-4 py-3 text-sm font-semibold text-slate-950 hover:bg-cyan-300"
                >
                  Evaluate a policy →
                </button>
              </Panel>

              <Panel
                title="Quick actions"
                description="Governance workspace shortcuts"
              >
                <div className="space-y-3">
                  {[
                    ["Evaluate agent action", "Policy Playground"],
                    ["View audit records", "Audit Logs"],
                    ["View approvals", "Agent Registry"],
                  ].map(([label, destination]) => (
                    <button
                      key={label}
                      onClick={() => setActive(destination as PageName)}
                      className="flex w-full items-center justify-between rounded-xl border border-white/10 p-4 text-left text-sm hover:border-cyan-400/40 hover:bg-white/[0.03]"
                    >
                      {label}
                      <span className="text-cyan-300">↗</span>
                    </button>
                  ))}
                </div>
              </Panel>
            </div>
          </>
        )}

        {active === "Policy Playground" && (
          <div className="max-w-4xl">
            <Panel
              title="Evaluate an agent action"
              description="Submit a request to the Spring Boot policy engine."
            >
              <form onSubmit={evaluatePolicy} className="space-y-5">
                <div>
                  <label htmlFor="agent" className="mb-2 block text-sm">
                    Agent ID
                  </label>
                  <select
                    id="agent"
                    value={agentId}
                    onChange={(event) => setAgentId(event.target.value)}
                    className="w-full rounded-xl border border-white/10 bg-[#080b12] px-4 py-3 text-sm outline-none focus:border-cyan-400"
                  >
                    {agentOptions.map((agent) => (
                      <option key={agent} value={agent}>
                        {agent}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label htmlFor="action" className="mb-2 block text-sm">
                    Requested action
                  </label>
                  <select
                    id="action"
                    value={action}
                    onChange={(event) => setAction(event.target.value)}
                    className="w-full rounded-xl border border-white/10 bg-[#080b12] px-4 py-3 text-sm outline-none focus:border-cyan-400"
                  >
                    {actionOptions.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  type="submit"
                  disabled={policyLoading}
                  className="rounded-lg bg-cyan-400 px-5 py-3 text-sm font-semibold text-slate-950 hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {policyLoading ? "Evaluating..." : "Evaluate policy"}
                </button>
              </form>

              {policyError && (
                <div
                  role="alert"
                  className="mt-5 rounded-xl border border-red-400/20 bg-red-400/5 p-4 text-sm text-red-300"
                >
                  {policyError}
                </div>
              )}

              {policySuccess && (
                <div
                  role="status"
                  className="mt-5 rounded-xl border border-emerald-400/20 bg-emerald-400/5 p-4 text-sm text-emerald-300"
                >
                  {policySuccess}
                </div>
              )}

              {policyResult && (
                <div className="mt-5 rounded-xl border border-white/10 bg-[#080b12] p-5">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <h4 className="font-semibold">Policy decision</h4>
                    <StatusBadge value={policyResult.decision} />
                  </div>
                  <dl className="mt-5 grid gap-4 sm:grid-cols-2">
                    <div>
                      <dt className="text-xs text-slate-500">Agent</dt>
                      <dd className="mt-1 text-sm">{policyResult.agentId}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-slate-500">Action</dt>
                      <dd className="mt-1 text-sm">{policyResult.action}</dd>
                    </div>
                    <div className="sm:col-span-2">
                      <dt className="text-xs text-slate-500">Reason</dt>
                      <dd className="mt-1 text-sm text-slate-300">
                        {policyResult.reason}
                      </dd>
                    </div>
                  </dl>
                </div>
              )}
            </Panel>

            <div className="mt-5">
              <Panel
                title="Agent permission reference"
                description="Expected permissions based on the current backend configuration."
              >
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[480px] text-left text-sm">
                    <thead className="text-xs text-slate-500">
                      <tr>
                        <th className="pb-3 pr-4">Agent</th>
                        <th className="pb-3">READ_DOCUMENT</th>
                        <th className="pb-3">CALCULATE</th>
                        <th className="pb-3">MAKE_PAYMENT</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-white/5">
                      {[
                        ["reader-agent", true, false, false],
                        ["analyst-agent", true, true, false],
                        ["payment-agent", true, true, true],
                      ].map(([agent, read, calculate, payment]) => (
                        <tr key={String(agent)}>
                          <td className="py-3 pr-4 font-medium">{agent}</td>
                          {[read, calculate, payment].map((allowed, index) => (
                            <td key={index} className="py-3">
                              <span
                                className={
                                  allowed
                                    ? "text-emerald-300"
                                    : "text-slate-600"
                                }
                              >
                                {allowed ? "Allowed" : "Blocked"}
                              </span>
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p className="mt-4 text-xs leading-5 text-slate-500">
                  This table reflects the configured permission map. Actual
                  decisions are returned by the backend.
                </p>
              </Panel>
            </div>
          </div>
        )}

        {active === "Audit Logs" && (
          <Panel
            title="Audit Logs"
            description="Live policy decisions retrieved from the audit API."
          >
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm text-slate-400">
                {auditLoading
                  ? "Loading records..."
                  : `${auditLogs.length} records loaded`}
              </p>
              <button
                onClick={() => setRefreshKey((key) => key + 1)}
                disabled={auditLoading}
                className="rounded-lg border border-white/10 px-4 py-2 text-sm hover:bg-white/5 disabled:opacity-50"
              >
                Refresh
              </button>
            </div>

            {auditError && (
              <div
                role="alert"
                className="mb-4 rounded-xl border border-red-400/20 bg-red-400/5 p-4 text-sm text-red-300"
              >
                {auditError}
              </div>
            )}

            {!auditLoading && !auditError && auditLogs.length === 0 && (
              <p className="rounded-xl border border-white/10 p-5 text-sm text-slate-400">
                No audit records returned by the backend.
              </p>
            )}

            {auditLogs.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[650px] text-left text-sm">
                  <thead className="text-xs text-slate-500">
                    <tr>
                      <th className="pb-3 pr-4">ID</th>
                      <th className="pb-3 pr-4">Agent</th>
                      <th className="pb-3 pr-4">Action</th>
                      <th className="pb-3 pr-4">Decision</th>
                      <th className="pb-3 pr-4">Reason</th>
                      <th className="pb-3">Created</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {auditLogs.map((log, index) => (
                      <tr key={log.id ?? index}>
                        <td className="py-4 pr-4 text-slate-500">
                          {log.id ?? "—"}
                        </td>
                        <td className="py-4 pr-4">{log.agentId}</td>
                        <td className="py-4 pr-4">{log.action}</td>
                        <td className="py-4 pr-4">
                          <StatusBadge value={log.decision} />
                        </td>
                        <td className="max-w-xs py-4 pr-4 text-slate-400">
                          {log.reason}
                        </td>
                        <td className="py-4 text-xs text-slate-400">
                          {formatDate(log.createdAt ?? log.timestamp)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Panel>
        )}

        {active === "Agent Registry" && (
          <div className="space-y-5">
            <Panel
              title="Approval requests"
              description="Review sensitive actions that require human approval."
            >
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-slate-400">
                  {pendingApprovals} pending request(s)
                </p>
                <button
                  onClick={() => setRefreshKey((key) => key + 1)}
                  disabled={approvalLoading}
                  className="rounded-lg border border-white/10 px-4 py-2 text-sm hover:bg-white/5 disabled:opacity-50"
                >
                  Refresh
                </button>
              </div>

              {approvalError && (
                <div
                  role="alert"
                  className="mb-4 rounded-xl border border-red-400/20 bg-red-400/5 p-4 text-sm text-red-300"
                >
                  {approvalError}
                </div>
              )}

              {approvalMessage && (
                <div
                  role="status"
                  className="mb-4 rounded-xl border border-emerald-400/20 bg-emerald-400/5 p-4 text-sm text-emerald-300"
                >
                  {approvalMessage}
                </div>
              )}

              {approvalLoading && (
                <p className="text-sm text-slate-400">Loading approvals...</p>
              )}

              {!approvalLoading && approvals.length === 0 && !approvalError && (
                <p className="rounded-xl border border-white/10 p-5 text-sm text-slate-400">
                  No approval requests returned by the backend.
                </p>
              )}

              <div className="space-y-3">
                {approvals.map((approval) => (
                  <div
                    key={approval.id}
                    className="rounded-xl border border-white/10 bg-[#080b12] p-4"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="font-semibold">
                          Request #{approval.id} · {approval.action}
                        </p>
                        <p className="mt-1 text-sm text-slate-400">
                          Agent: {approval.agentId}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          Requested: {formatDate(approval.requestedAt)}
                        </p>
                      </div>
                      <StatusBadge value={approval.status} />
                    </div>

                    {approval.status === "PENDING" && (
                      <div className="mt-4 flex flex-wrap gap-3">
                        <button
                          onClick={() =>
                            void reviewApproval(approval.id, "approve")
                          }
                          disabled={reviewingId !== null}
                          className="rounded-lg bg-emerald-400 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-emerald-300 disabled:opacity-50"
                        >
                          {reviewingId === approval.id
                            ? "Processing..."
                            : "Approve"}
                        </button>
                        <button
                          onClick={() =>
                            void reviewApproval(approval.id, "reject")
                          }
                          disabled={reviewingId !== null}
                          className="rounded-lg border border-red-400/30 px-4 py-2 text-sm font-semibold text-red-300 hover:bg-red-400/10 disabled:opacity-50"
                        >
                          Reject
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Panel>

            <Panel
              title="Configured agents"
              description="Agents currently represented in the backend permission map."
            >
              <div className="grid gap-3 sm:grid-cols-3">
                {agentOptions.map((agent) => (
                  <div
                    key={agent}
                    className="rounded-xl border border-white/10 p-4"
                  >
                    <p className="font-medium">{agent}</p>
                    <p className="mt-2 text-xs text-slate-500">
                      Policy evaluation enabled
                    </p>
                    <button
                      onClick={() => {
                        setAgentId(agent);
                        setActive("Policy Playground");
                      }}
                      className="mt-4 text-sm font-semibold text-cyan-300 hover:text-cyan-200"
                    >
                      Test agent →
                    </button>
                  </div>
                ))}
              </div>
            </Panel>
          </div>
        )}

        <footer className="mt-10 border-t border-white/10 pt-5 text-xs text-slate-600">
          SentinelAI · AI Agent Security &amp; Governance
        </footer>
      </section>
    </main>
  );
}
