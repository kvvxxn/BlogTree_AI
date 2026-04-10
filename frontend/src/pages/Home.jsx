import { useEffect, useMemo, useState } from "react";
import {
	getApiBaseUrl,
	loginWithGoogleCode,
	logoutWithAccessToken,
	reissueToken
} from "../api/blogApi";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";
const REDIRECT_URI =
	import.meta.env.VITE_GOOGLE_REDIRECT_URI ||
	`${window.location.origin}/oauth/google/callback`;

const GOOGLE_SCOPE = "openid email profile";

function buildGoogleAuthorizeUrl({ clientId, redirectUri, state }) {
	const params = new URLSearchParams({
		client_id: clientId,
		redirect_uri: redirectUri,
		response_type: "code",
		scope: GOOGLE_SCOPE,
		access_type: "offline",
		prompt: "consent",
		state
	});

	return `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;
}

function createOAuthState() {
	return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export default function Home() {
	const [loading, setLoading] = useState(false);
	const [status, setStatus] = useState({ type: "warn", text: "구글 로그인 테스트 준비됨" });
	const [tokens, setTokens] = useState({
		accessToken: "",
		refreshToken: "",
		message: ""
	});

	const callbackActive = useMemo(
		() => window.location.pathname.includes("/oauth/google/callback"),
		[]
	);

	useEffect(() => {
		const params = new URLSearchParams(window.location.search);
		const code = params.get("code");
		const state = params.get("state");
		const savedState = sessionStorage.getItem("google_oauth_state");

		if (!code) {
			return;
		}

		if (!state || !savedState || state !== savedState) {
			setStatus({ type: "err", text: "state 검증에 실패했습니다. 다시 시도해 주세요." });
			return;
		}

		const exchangeLockKey = `google_oauth_exchanged_${code}`;
		if (sessionStorage.getItem(exchangeLockKey) === "1") {
			return;
		}
		sessionStorage.setItem(exchangeLockKey, "1");

		setLoading(true);
		setStatus({ type: "warn", text: "인가 코드를 백엔드로 전송 중..." });

		loginWithGoogleCode({ authorizationCode: code, redirectUri: REDIRECT_URI })
			.then((result) => {
				setTokens({
					accessToken: result.accessToken || "",
					refreshToken: result.refreshToken || "",
					message: result.message || "로그인 성공"
				});
				setStatus({ type: "ok", text: "로그인 성공: 토큰 발급 완료" });
				sessionStorage.removeItem("google_oauth_state");

				// 콜백 쿼리스트링(code/state)은 노출하지 않도록 정리
				window.history.replaceState({}, "", "/");
			})
			.catch((error) => {
				sessionStorage.removeItem(exchangeLockKey);
				setStatus({
					type: "err",
					text: `로그인 실패: ${error.message || "원인을 확인해 주세요."}`
				});
			})
			.finally(() => {
				setLoading(false);
			});
	}, []);

	function handleStartGoogleLogin() {
		if (!GOOGLE_CLIENT_ID) {
			setStatus({ type: "err", text: "VITE_GOOGLE_CLIENT_ID가 설정되지 않았습니다." });
			return;
		}

		const state = createOAuthState();
		sessionStorage.setItem("google_oauth_state", state);

		const authUrl = buildGoogleAuthorizeUrl({
			clientId: GOOGLE_CLIENT_ID,
			redirectUri: REDIRECT_URI,
			state
		});

		window.location.assign(authUrl);
	}

	async function handleReissue() {
		if (!tokens.refreshToken) {
			setStatus({ type: "warn", text: "먼저 로그인하여 refreshToken을 발급받아 주세요." });
			return;
		}

		setLoading(true);
		try {
			const result = await reissueToken(tokens.refreshToken);
			setTokens({
				accessToken: result.accessToken || "",
				refreshToken: result.refreshToken || "",
				message: result.message || "재발급 성공"
			});
			setStatus({ type: "ok", text: "재발급 성공" });
		} catch (error) {
			setStatus({
				type: "err",
				text: `재발급 실패: ${error.message || "원인을 확인해 주세요."}`
			});
		} finally {
			setLoading(false);
		}
	}

	async function handleLogout() {
		if (!tokens.accessToken) {
			setStatus({ type: "warn", text: "먼저 로그인하여 accessToken을 발급받아 주세요." });
			return;
		}

		setLoading(true);
		try {
			const message = await logoutWithAccessToken(tokens.accessToken);
			setTokens({
				accessToken: "",
				refreshToken: "",
				message: ""
			});
			setStatus({ type: "ok", text: message || "로그아웃 성공" });
		} catch (error) {
			setStatus({
				type: "err",
				text: `로그아웃 실패: ${error.message || "원인을 확인해 주세요."}`
			});
		} finally {
			setLoading(false);
		}
	}

	return (
		<main>
			<h1>Google OAuth Login Testbench</h1>
			<p>
				BlogTree_AI 구글 소셜 로그인 테스트 페이지입니다. 버튼 클릭 후 callback에서 code를 수신하면
				자동으로 백엔드 로그인 API를 호출합니다.
			</p>

			<div className="meta">
				<span className="chip">API: {getApiBaseUrl()}</span>
				<span className="chip">Redirect URI: {REDIRECT_URI}</span>
				{callbackActive ? <span className="chip">Callback Mode</span> : null}
			</div>

			<div className="grid">
				<section className="panel">
					<h2>OAuth Action</h2>
					<p>Google 인증 페이지로 이동해 인가코드를 발급받습니다.</p>

					<div className="actions">
						<button
							type="button"
							className="btn-primary"
							onClick={handleStartGoogleLogin}
							disabled={loading}
						>
							{loading ? "처리 중..." : "Google 로그인 시작"}
						</button>
						<button
							type="button"
							className="btn-secondary"
							onClick={handleReissue}
							disabled={loading}
						>
							Access/Refresh 재발급 테스트
						</button>
						<button
							type="button"
							className="btn-secondary"
							onClick={handleLogout}
							disabled={loading}
						>
							로그아웃
						</button>
					</div>

					<div className={`status ${status.type}`}>{status.text}</div>
				</section>

				<section className="panel">
					<h2>Token Output</h2>
					<label htmlFor="message">message</label>
					<input id="message" readOnly value={tokens.message} />

					<label htmlFor="access-token">accessToken</label>
					<textarea id="access-token" readOnly value={tokens.accessToken} />

					<label htmlFor="refresh-token">refreshToken</label>
					<textarea id="refresh-token" readOnly value={tokens.refreshToken} />
				</section>
			</div>
		</main>
	);
}
