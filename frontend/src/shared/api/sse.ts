import { env } from "@/shared/config/env";
import { getAccessToken } from "@/shared/api/token-storage";

export type TaskEventHandler = (eventName: string, payload: unknown) => void;

export async function subscribeTask(taskId: string, onEvent: TaskEventHandler) {
  const accessToken = getAccessToken();
  const response = await fetch(`${env.apiBaseUrl}/api/tasks/subscribe/${taskId}`, {
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
  });

  if (!response.ok || !response.body) {
    throw new Error("작업 이벤트 스트림 연결에 실패했습니다.");
  }

  const reader = response.body.getReader();
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
      const eventLine = chunk
        .split("\n")
        .find((line) => line.startsWith("event:"));
      const dataLine = chunk
        .split("\n")
        .find((line) => line.startsWith("data:"));

      if (!eventLine || !dataLine) {
        continue;
      }

      const eventName = eventLine.replace("event:", "").trim();
      const payload = JSON.parse(dataLine.replace("data:", "").trim());
      onEvent(eventName, payload);
    }
  }
}
