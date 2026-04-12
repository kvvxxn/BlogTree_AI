type LogLevel = "debug" | "info" | "warn" | "error";

function writeLog(level: LogLevel, scope: string, message: string, meta?: unknown) {
  const prefix = `[${scope}] ${message}`;

  if (meta === undefined) {
    console[level](prefix);
    return;
  }

  console[level](prefix, meta);
}

export const logger = {
  debug(scope: string, message: string, meta?: unknown) {
    writeLog("debug", scope, message, meta);
  },
  info(scope: string, message: string, meta?: unknown) {
    writeLog("info", scope, message, meta);
  },
  warn(scope: string, message: string, meta?: unknown) {
    writeLog("warn", scope, message, meta);
  },
  error(scope: string, message: string, meta?: unknown) {
    writeLog("error", scope, message, meta);
  },
};
