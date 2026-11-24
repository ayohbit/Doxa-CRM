import { spawn } from "node:child_process";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { ensureDatabase } from "./ensure-database.mjs";

const root = resolve(fileURLToPath(new URL(".", import.meta.url)), "..");
const windows = process.platform === "win32";
const npm = windows ? "npm.cmd" : "npm";
const mavenWrapper = windows ? "mvnw.cmd" : "./mvnw";
const services = [
  { name: "Backend API", command: mavenWrapper, args: ["spring-boot:run"], cwd: resolve(root, "backend") },
  { name: "CRM frontend", command: npm, args: ["run", "dev"], cwd: resolve(root, "frontend") },
  { name: "Developer portal", command: npm, args: ["run", "dev"], cwd: resolve(root, "developer-portal") },
];

let stopping = false;
try {
  await ensureDatabase();
} catch (error) {
  console.error(`[PostgreSQL] ${error.message}`);
  process.exit(1);
}

const children = services.map((service) => {
  const child = spawn(service.command, service.args, {
    cwd: service.cwd,
    env: process.env,
    stdio: "inherit",
    shell: windows,
  });
  child.on("error", (error) => {
    console.error(`[${service.name}] ${error.message}`);
    shutdown(1);
  });
  child.on("exit", (code) => {
    if (!stopping) {
      console.error(`[${service.name}] exited unexpectedly with code ${code ?? 1}`);
      shutdown(code === 0 ? 1 : code ?? 1);
    }
  });
  return child;
});

function shutdown(code = 0) {
  if (stopping) return;
  stopping = true;
  for (const child of children) {
    if (child.exitCode === null) child.kill();
  }
  process.exitCode = code;
}

process.on("SIGINT", () => shutdown(0));
process.on("SIGTERM", () => shutdown(0));
