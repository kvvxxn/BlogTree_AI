import { env } from "@/shared/config/env";
import { clearAuthTokens, getAccessToken } from "@/shared/api/token-storage";
import { reissueAccessToken } from "@/shared/api/http";
import { saveRedirectAfterLogin } from "@/features/auth/lib/auth-redirect";
import { logger } from "@/shared/lib/logger";

export type TaskEventHandler = (eventName: string, payload: unknown) => void;

export type TaskSubscription = {
  close: () => void;
  completed: Promise<void>;
};

type SubscribeTaskOptions = {
  onOpen?: () => void;
};

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError";
}

function redirectToLoginAfterSessionExpired() {
  clearAuthTokens();

  if (window.location.pathname !== "/login") {
    saveRedirectAfterLogin(
      `${window.location.pathname}${window.location.search}${window.location.hash}`,
    );
    window.location.href = "/login?reason=session-expired";
  }
}

async function openTaskStream(taskId: string, signal: AbortSignal, allowRetry = true) {
  const accessToken = getAccessToken();
  const response = await fetch(`${env.apiBaseUrl}/api/tasks/subscribe/${taskId}`, {
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
    credentials: "include",
    signal,
  });

  if (response.status === 401 && allowRetry) {
    logger.warn("task-sse", "SSE 구독이 401을 반환해 토큰 재발급을 시도합니다.", { taskId });
    const reissued = await reissueAccessToken();

    if (!reissued) {
      logger.warn("task-sse", "SSE 구독용 토큰 재발급에 실패했습니다.");
      redirectToLoginAfterSessionExpired();
      throw new Error("인증이 만료되었습니다.");
    }

    return openTaskStream(taskId, signal, false);
  }

  if (!response.ok || !response.body) {
    throw new Error("작업 이벤트 스트림 연결에 실패했습니다.");
  }

  return response;
}

export function subscribeTask(
  taskId: string,
  onEvent: TaskEventHandler,
  options: SubscribeTaskOptions = {},
): TaskSubscription {
  const controller = new AbortController();

  const completed = (async () => {
    try {
      const response = await openTaskStream(taskId, controller.signal);
      options.onOpen?.();
      const responseBody = response.body;

      if (!responseBody) {
        throw new Error("작업 이벤트 스트림 본문을 읽을 수 없습니다.");
      }

      const reader = responseBody.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          break;
        }

        buffer += decoder.decode(value, { stream: true });

        const chunks = buffer.split("\n\n");
        buffer = chunks.pop() ?? "";

        for (const chunk of chunks) {
          const lines = chunk.split("\n");
          const eventLine = lines.find((line) => line.startsWith("event:"));
          const dataLine = lines.find((line) => line.startsWith("data:"));

          if (!eventLine || !dataLine) {
            continue;
          }

          const eventName = eventLine.replace("event:", "").trim();
          const rawPayload = dataLine.replace("data:", "").trim();

          try {
            const payload = JSON.parse(rawPayload);
            onEvent(eventName, payload);
          } catch (error) {
            logger.warn("task-sse", "SSE payload JSON 파싱에 실패했습니다.", {
              taskId,
              eventName,
              error,
            });
          }
        }
      }
    } catch (error) {
      if (isAbortError(error)) {
        return;
      }

      throw error;
    }
  })();

  return {
    close() {
      controller.abort();
    },
    completed,
  };
}
