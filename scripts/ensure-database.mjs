import { spawnSync } from "node:child_process";
import net from "node:net";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(fileURLToPath(new URL(".", import.meta.url)), "..");
const composeFile = resolve(root, "backend", "docker-compose.yml");
const defaultDatabaseUrl = "jdbc:postgresql://localhost:5433/doxa_crm";

function getDatabaseTarget() {
  const jdbcUrl = process.env.SPRING_DATASOURCE_URL ?? defaultDatabaseUrl;
  const url = new URL(jdbcUrl.replace(/^jdbc:/, ""));
  return {
    host: url.hostname,
    port: Number(url.port || 5432),
    useLocalCompose: ["localhost", "127.0.0.1", "::1"].includes(url.hostname) && Number(url.port || 5432) === 5433,
  };
}

function canConnect(host, port) {
  return new Promise((resolveConnection) => {
    const socket = net.createConnection({ host, port });
    socket.setTimeout(1000);
    socket.once("connect", () => {
      socket.destroy();
      resolveConnection(true);
    });
    socket.once("error", () => resolveConnection(false));
    socket.once("timeout", () => {
      socket.destroy();
      resolveConnection(false);
    });
  });
}

function runDocker(args, stdio = "inherit") {
  const result = spawnSync("docker", args, { cwd: root, stdio });
  if (result.error) {
    throw new Error(`Docker is required to start PostgreSQL: ${result.error.message}`);
  }
  if (result.status !== 0) {
    throw new Error(`Docker Compose exited with code ${result.status ?? 1}.`);
  }
}

async function waitForConnection(host, port, timeoutMs = 60_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await canConnect(host, port)) return;
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 500));
  }
  throw new Error(`PostgreSQL did not become available at ${host}:${port} within ${timeoutMs / 1000} seconds.`);
}

async function waitForLocalPostgres(host, port, timeoutMs = 60_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const result = spawnSync(
      "docker",
      ["compose", "-f", composeFile, "exec", "-T", "postgres", "pg_isready", "-U", "doxa", "-d", "doxa_crm"],
      { cwd: root, stdio: "ignore" },
    );
    if (result.error) {
      throw new Error(`Could not check PostgreSQL readiness: ${result.error.message}`);
    }
    if (result.status === 0 && await canConnect(host, port)) return;
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 500));
  }
  throw new Error(`The local PostgreSQL container did not become ready within ${timeoutMs / 1000} seconds.`);
}

export async function ensureDatabase() {
  const { host, port, useLocalCompose } = getDatabaseTarget();
  if (await canConnect(host, port)) return;

  if (useLocalCompose) {
    console.log("[PostgreSQL] Starting the local database with Docker Compose...");
    runDocker(["compose", "-f", composeFile, "up", "-d"]);
    await waitForLocalPostgres(host, port);
    return;
  }

  await waitForConnection(host, port);
}
