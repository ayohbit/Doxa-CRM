import { spawn } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";

for (const name of [".env", ".env.local"]) {
  const file = new URL(`../${name}`, import.meta.url);
  if (!existsSync(file)) continue;
  for (const line of readFileSync(file, "utf8").split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/);
    if (!match || match[1] in process.env) continue;
    process.env[match[1]] = match[2].replace(/^(['"])(.*)\1$/, "$2");
  }
}

const command = process.platform === "win32" ? "next.cmd" : "next";
const args = [process.argv[2] ?? "dev"];
if (args[0] === "dev" || args[0] === "start") {
  args.push("-p", process.env.DEVELOPER_PORTAL_PORT ?? "3001");
}

const child = spawn(command, args, { stdio: "inherit", shell: process.platform === "win32" });
child.on("exit", (code) => process.exit(code ?? 1));
