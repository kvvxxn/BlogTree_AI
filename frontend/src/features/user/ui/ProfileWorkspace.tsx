import { useEffect, useState, type FormEvent } from "react";
import { getMyProfile, updateMyProfile } from "@/features/user/api/user.api";
import { ApiError } from "@/shared/api/http";
import { logger } from "@/shared/lib/logger";
import type { UserProfile } from "@/shared/types/api";

type LoadStatus = "loading" | "success" | "error";

type ProfileDraft = {
  name: string;
  careerGoal: string;
};

const EMPTY_DRAFT: ProfileDraft = {
  name: "",
  careerGoal: "",
};

export function ProfileWorkspace() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [draft, setDraft] = useState<ProfileDraft>(EMPTY_DRAFT);
  const [status, setStatus] = useState<LoadStatus>("loading");
  const [reloadToken, setReloadToken] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [loadErrorMessage, setLoadErrorMessage] = useState<string | null>(null);
  const [saveErrorMessage, setSaveErrorMessage] = useState<string | null>(null);
  const [saveSuccessMessage, setSaveSuccessMessage] = useState<string | null>(null);
  const [avatarFailed, setAvatarFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadProfile() {
      try {
        logger.info("user", "내 프로필 조회를 시작합니다.");
        setStatus("loading");
        setLoadErrorMessage(null);
        setSaveErrorMessage(null);
        setSaveSuccessMessage(null);

        const response = await getMyProfile();

        if (cancelled) {
          return;
        }

        logger.info("user", "내 프로필 조회에 성공했습니다.", { userId: response.id });
        setProfile(response);
        setDraft({
          name: response.name ?? "",
          careerGoal: response.careerGoal ?? "",
        });
        setAvatarFailed(false);
        setStatus("success");
      } catch (error) {
        if (cancelled) {
          return;
        }

        logger.error("user", "내 프로필 조회에 실패했습니다.", error);
        setProfile(null);
        setDraft(EMPTY_DRAFT);
        setStatus("error");
        setLoadErrorMessage(
          getErrorMessage(error, "프로필 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."),
        );
      }
    }

    void loadProfile();

    return () => {
      cancelled = true;
    };
  }, [reloadToken]);

  const trimmedName = draft.name.trim();
  const currentName = profile?.name ?? "";
  const currentCareerGoal = profile?.careerGoal ?? "";
  const hasChanges = trimmedName !== currentName || draft.careerGoal !== currentCareerGoal;
  const validationMessage = getValidationMessage(trimmedName, draft.careerGoal);
  const canSubmit = status === "success" && !isSaving && hasChanges && !validationMessage;
  const visibleErrorMessage = saveErrorMessage ?? validationMessage;
  const avatarLabel = getAvatarLabel(profile?.name ?? draft.name);
  const shouldShowAvatarImage = !!profile?.profileImageUrl && !avatarFailed;

  function handleRetry() {
    setProfile(null);
    setDraft(EMPTY_DRAFT);
    setStatus("loading");
    setLoadErrorMessage(null);
    setSaveErrorMessage(null);
    setSaveSuccessMessage(null);
    setAvatarFailed(false);
    setReloadToken((current) => current + 1);
  }

  function handleFieldChange(field: keyof ProfileDraft, value: string) {
    setDraft((current) => ({
      ...current,
      [field]: value,
    }));
    setSaveErrorMessage(null);
    setSaveSuccessMessage(null);
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!profile) {
      return;
    }

    if (validationMessage) {
      setSaveErrorMessage(validationMessage);
      setSaveSuccessMessage(null);
      return;
    }

    if (!hasChanges) {
      setSaveErrorMessage(null);
      setSaveSuccessMessage("변경된 내용이 없습니다.");
      return;
    }

    try {
      logger.info("user", "내 프로필 수정을 시작합니다.", { userId: profile.id });
      setIsSaving(true);
      setSaveErrorMessage(null);
      setSaveSuccessMessage(null);

      const response = await updateMyProfile({
        name: trimmedName,
        careerGoal: draft.careerGoal,
      });

      logger.info("user", "내 프로필 수정에 성공했습니다.", { userId: response.id });
      setProfile(response);
      setDraft({
        name: response.name ?? "",
        careerGoal: response.careerGoal ?? "",
      });
      setAvatarFailed(false);
      setSaveSuccessMessage("프로필이 저장되었습니다.");
    } catch (error) {
      logger.error("user", "내 프로필 수정에 실패했습니다.", error);
      setSaveErrorMessage(
        getErrorMessage(error, "프로필을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요."),
      );
    } finally {
      setIsSaving(false);
    }
  }

  if (status === "loading") {
    return (
      <section className="page-stack">
        <article className="card">
          <span className="section-label">Profile</span>
          <h2>프로필 정보를 불러오는 중입니다</h2>
          <div className="dashboard-inline-status">
            <strong>계정 정보를 동기화하고 있습니다</strong>
            <span>이름, 이메일, 커리어 목표를 최신 상태로 불러오고 있습니다.</span>
          </div>
        </article>
      </section>
    );
  }

  if (status === "error") {
    return (
      <section className="page-stack">
        <article className="card">
          <span className="section-label">Profile</span>
          <h2>프로필 정보를 불러오지 못했습니다</h2>
          <div className="dashboard-inline-status dashboard-inline-status--error">
            <strong>조회에 실패했습니다</strong>
            <span>{loadErrorMessage ?? "잠시 후 다시 시도해 주세요."}</span>
            <button className="button button--primary" type="button" onClick={handleRetry}>
              다시 시도
            </button>
          </div>
        </article>
      </section>
    );
  }

  return (
    <section className="page-stack">
      <section className="content-grid content-grid--wide">
        <article className="card profile-overview-card">
          <div className="profile-overview-card__header">
            <div>
              <span className="section-label">Account</span>
              <h2>기본 정보</h2>
            </div>
            <button className="button button--ghost" type="button" onClick={handleRetry}>
              새로고침
            </button>
          </div>

          <div className="profile-card">
            <div className="avatar" aria-hidden="true">
              {shouldShowAvatarImage ? (
                <img
                  className="avatar__image"
                  src={profile?.profileImageUrl ?? ""}
                  alt=""
                  onError={() => setAvatarFailed(true)}
                />
              ) : (
                <span>{avatarLabel}</span>
              )}
            </div>

            <div className="profile-info">
              <strong>{profile?.name ?? "-"}</strong>
              <span className="profile-goal">
                {profile?.careerGoal?.trim() || "커리어 목표를 아직 설정하지 않았습니다."}
              </span>
              <p>{profile?.email ?? "-"}</p>
            </div>
          </div>

          <div className="profile-overview-card__meta">
            <span>저장은 이름과 커리어 목표를 함께 반영합니다.</span>
            <span>{hasChanges ? "저장되지 않은 변경 사항이 있습니다." : "변경 사항이 없습니다."}</span>
          </div>

          {visibleErrorMessage ? (
            <div className="summary-status summary-status--error">
              <strong>{saveErrorMessage ? "저장에 실패했습니다" : "입력값을 확인해 주세요"}</strong>
              <span>{visibleErrorMessage}</span>
            </div>
          ) : null}

          {saveSuccessMessage ? (
            <div className="summary-status summary-status--success">
              <strong>저장 완료</strong>
              <span>{saveSuccessMessage}</span>
            </div>
          ) : null}
        </article>

        <article className="card">
          <span className="section-label">Edit Name</span>
          <h2>이름 수정</h2>
          <form className="form-layout" onSubmit={handleSave}>
            <label className="field">
              <span>이름</span>
              <input
                className="input"
                type="text"
                placeholder="이름을 입력하세요"
                value={draft.name}
                onChange={(event) => handleFieldChange("name", event.target.value)}
                maxLength={100}
                disabled={isSaving}
              />
            </label>

            <div className="profile-form-note">
              <span>이름은 필수 항목이며 최대 100자까지 입력할 수 있습니다.</span>
            </div>

            <div className="button-row">
              <button className="button button--primary" type="submit" disabled={!canSubmit}>
                {isSaving ? "저장 중..." : hasChanges ? "프로필 저장" : "변경 사항 없음"}
              </button>
            </div>
          </form>
        </article>

        <article className="card">
          <span className="section-label">Edit Career Goal</span>
          <h2>커리어 목표 수정</h2>
          <form className="form-layout" onSubmit={handleSave}>
            <label className="field">
              <span>커리어 목표</span>
              <textarea
                className="textarea"
                placeholder="예: Backend Developer"
                value={draft.careerGoal}
                onChange={(event) => handleFieldChange("careerGoal", event.target.value)}
                maxLength={1000}
                rows={6}
                disabled={isSaving}
              />
            </label>

            <div className="profile-form-note profile-form-note--split">
              <span>커리어 목표는 비워둘 수 있지만, 요약과 추천 기능 사용 전 설정하는 편이 좋습니다.</span>
              <span>{draft.careerGoal.length}/1000</span>
            </div>

            <div className="button-row">
              <button className="button button--primary" type="submit" disabled={!canSubmit}>
                {isSaving ? "저장 중..." : hasChanges ? "프로필 저장" : "변경 사항 없음"}
              </button>
            </div>
          </form>
        </article>
      </section>
    </section>
  );
}

function getValidationMessage(name: string, careerGoal: string) {
  if (!name) {
    return "이름은 비어 있을 수 없습니다.";
  }

  if (name.length > 100) {
    return "이름은 100자를 초과할 수 없습니다.";
  }

  if (careerGoal.length > 1000) {
    return "커리어 목표는 1000자를 초과할 수 없습니다.";
  }

  return null;
}

function getAvatarLabel(name: string) {
  const trimmedName = name.trim();

  if (!trimmedName) {
    return "ME";
  }

  return trimmedName.slice(0, 2).toUpperCase();
}

function getErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof ApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallbackMessage;
}
