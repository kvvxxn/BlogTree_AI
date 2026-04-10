const DEFAULT_API_BASE_URL =
	import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

async function parseError(response) {
	try {
		const data = await response.json();
		if (typeof data?.message === "string") {
			return data.message;
		}
		return JSON.stringify(data);
	} catch {
		return response.statusText || "요청 처리 중 오류가 발생했습니다.";
	}
}

export async function loginWithGoogleCode({ authorizationCode, redirectUri }) {
	const response = await fetch(`${DEFAULT_API_BASE_URL}/api/auth/google`, {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify({
			authorizationCode,
			redirectUri
		})
	});

	if (!response.ok) {
		const message = await parseError(response);
		throw new Error(message);
	}

	return response.json();
}

export async function reissueToken(refreshToken) {
	const response = await fetch(`${DEFAULT_API_BASE_URL}/api/auth/reissue`, {
		method: "POST",
		headers: {
			"Refresh-Token": refreshToken
		}
	});

	if (!response.ok) {
		const message = await parseError(response);
		throw new Error(message);
	}

	return response.json();
}

export async function logoutWithAccessToken(accessToken) {
	const response = await fetch(`${DEFAULT_API_BASE_URL}/api/auth/logout`, {
		method: "POST",
		headers: {
			Authorization: `Bearer ${accessToken}`
		}
	});

	if (!response.ok) {
		const message = await parseError(response);
		throw new Error(message);
	}

	return response.text();
}

export function getApiBaseUrl() {
	return DEFAULT_API_BASE_URL;
}
