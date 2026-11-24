import { spawn } from "node:child_process";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { ensureDatabase } from "./ensure-database.mjs";

const windows = process.platform === "win32";
const wrapper = windows ? "mvnw.cmd" : "./mvnw";
const backendDirectory = resolve(fileURLToPath(new URL(".", import.meta.url)), "..", "backend");

try {
  await ensureDatabase();
} catch (error) {
  console.error(`[PostgreSQL] ${error.message}`);
  process.exit(1);
}

const child = spawn(wrapper, ["spring-boot:run"], {
  cwd: backendDirectory,
  env: process.env,
  stdio: "inherit",
  shell: windows,
});
let stopping = false;
child.on("error", (error) => {
  console.error(`[Backend API] ${error.message}`);
  shutdown(1);
});
child.on("exit", (code) => {
  if (!stopping) process.exitCode = code === 0 ? 1 : code ?? 1;
});

function shutdown(code) {
  if (stopping) return;
  stopping = true;
  if (child.exitCode === null) child.kill();
  process.exitCode = code;
}

process.on("SIGINT", () => shutdown(0));
process.on("SIGTERM", () => shutdown(0));
