/**
 * @FileName : DepartmentTeamSelector.js
 * @Description : 사내 AI 포털 사용자 본부 - 팀 - 사원 선택 컴포넌트
 * @Author : 송혜진
 * @Date : 2026. 05. 14
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.14    송혜진        최초 생성
 */


import React, { useEffect, useMemo, useState } from "react";
import { getDepartmentTree, unwrapApiData } from "../api/aiSecretaryApi";
import { C } from "../styles/aiSecretaryTheme";

const selectStyle = {
  width: "100%",
  height: 42,
  borderRadius: 10,
  border: `1px solid ${C.border}`,
  padding: "0 12px",
  boxSizing: "border-box",
  fontSize: 14,
  color: C.text,
  background: "#fff",
  outline: "none",
};

const labelStyle = {
  fontSize: 13,
  fontWeight: 800,
  color: C.text,
  marginBottom: 8,
};

const noteStyle = {
  fontSize: 12,
  color: C.sub,
  lineHeight: 1.5,
};

const teamWrapStyle = {
  display: "flex",
  gap: 8,
  flexWrap: "wrap",
  paddingTop: 2,
};

const teamChipStyle = (active) => ({
  height: 34,
  padding: "0 12px",
  borderRadius: 999,
  border: `1px solid ${active ? C.accent : C.border}`,
  background: active ? C.accentBg : "#fff",
  color: active ? C.accent : C.text,
  fontSize: 13,
  fontWeight: 700,
  cursor: "pointer",
});

function getDeptId(dept) {
  return dept?.deptId ?? dept?.id ?? dept?.departmentId ?? "";
}

function getDeptName(dept) {
  return dept?.deptName ?? dept?.name ?? dept?.departmentName ?? "";
}

function getDeptChildren(dept) {
  return (
    dept?.children ??
    dept?.childDepartments ??
    dept?.departments ??
    dept?.items ??
    []
  );
}

function normalizeDepartments(list) {
  return Array.isArray(list) ? list : [];
}

function normalizeDeptName(value) {
  return String(value || "")
    .split(">")
    .pop()
    .trim();
}

function splitTeamNames(value) {
  return String(value || "")
    .split(",")
    .map((part) => part.trim())
    .map((part) => normalizeDeptName(part))
    .filter(Boolean);
}

function normalizeSelection(value) {
  if (!value) {
    return {
      headquarterId: "",
      headquarterName: "",
      teamIds: [],
      teamNames: [],
      displayName: "",
    };
  }

  if (typeof value === "string") {
    const teamNames = splitTeamNames(value);
    return {
      headquarterId: "",
      headquarterName: "",
      teamIds: [],
      teamNames,
      displayName: teamNames.join(", "),
    };
  }

  const teamNames = Array.isArray(value.teamNames)
    ? value.teamNames.map((item) => normalizeDeptName(item)).filter(Boolean)
    : splitTeamNames(value.displayName);

  const teamIds = Array.isArray(value.teamIds)
    ? value.teamIds.map((item) => String(item || "")).filter(Boolean)
    : [];

  return {
    headquarterId: String(value.headquarterId || ""),
    headquarterName: String(value.headquarterName || ""),
    teamIds,
    teamNames,
    displayName: teamNames.join(", ") || String(value.displayName || "").trim(),
  };
}

export default function DepartmentTeamSelector({
  value = null,
  onChange,
  disabled = false,
}) {
  const [departmentTree, setDepartmentTree] = useState([]);
  const [selectedHeadquarterId, setSelectedHeadquarterId] = useState("");
  const [selectedTeamIds, setSelectedTeamIds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const headquarters = useMemo(
    () => normalizeDepartments(departmentTree),
    [departmentTree]
  );

  const selectedHeadquarter = useMemo(
    () =>
      headquarters.find(
        (dept) => String(getDeptId(dept)) === String(selectedHeadquarterId)
      ) || null,
    [headquarters, selectedHeadquarterId]
  );

  const teamOptions = useMemo(() => {
    if (!selectedHeadquarter) {
      return [];
    }

    const children = normalizeDepartments(getDeptChildren(selectedHeadquarter));
    if (children.length > 0) {
      return children;
    }

    return [selectedHeadquarter];
  }, [selectedHeadquarter]);

  useEffect(() => {
    const loadTree = async () => {
      setLoading(true);
      setError("");

      try {
        const response = await getDepartmentTree();
        const data = unwrapApiData(response) ?? [];
        setDepartmentTree(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("부서 트리 조회 실패", err);
        setDepartmentTree([]);
        setError("부서 목록을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };

    loadTree();
  }, []);

  useEffect(() => {
    const next = normalizeSelection(value);

    setSelectedHeadquarterId(next.headquarterId);
    setSelectedTeamIds(next.teamIds);
  }, [value]);

  const emitSelection = (headquarter, teamIds) => {
    if (!headquarter) {
      onChange?.({
        headquarterId: "",
        headquarterName: "",
        teamIds: [],
        teamNames: [],
        displayName: "",
      });
      return;
    }

    const teamNameMap = new Map(
      teamOptions.map((team) => [String(getDeptId(team)), getDeptName(team)])
    );

    const normalizedTeamIds = (Array.isArray(teamIds) ? teamIds : [])
      .map((item) => String(item || ""))
      .filter(Boolean);

    const teamNames = normalizedTeamIds
      .map((teamId) => teamNameMap.get(teamId) || "")
      .filter(Boolean);

    onChange?.({
      headquarterId: String(getDeptId(headquarter) || ""),
      headquarterName: getDeptName(headquarter),
      teamIds: normalizedTeamIds,
      teamNames,
      displayName: teamNames.join(", "),
    });
  };

  const handleHeadquarterChange = (nextHeadquarterId) => {
    setSelectedHeadquarterId(nextHeadquarterId);
    setSelectedTeamIds([]);

    if (!nextHeadquarterId) {
      emitSelection(null, []);
      return;
    }

    const matchedHeadquarter =
      headquarters.find(
        (dept) => String(getDeptId(dept)) === String(nextHeadquarterId)
      ) || null;

    if (!matchedHeadquarter) {
      emitSelection(null, []);
      return;
    }

    emitSelection(matchedHeadquarter, []);
  };

  const toggleTeam = (teamId) => {
    if (!selectedHeadquarter) {
      return;
    }

    const normalizedTeamId = String(teamId || "");
    if (!normalizedTeamId) {
      return;
    }

    const nextTeamIds = selectedTeamIds.includes(normalizedTeamId)
      ? selectedTeamIds.filter((item) => item !== normalizedTeamId)
      : [...selectedTeamIds, normalizedTeamId];

    setSelectedTeamIds(nextTeamIds);
    emitSelection(selectedHeadquarter, nextTeamIds);
  };

  return (
    <div style={{ display: "grid", gap: 12 }}>
      <div>
        <div style={labelStyle}>대상 본부</div>
        <select
          disabled={disabled || loading}
          value={selectedHeadquarterId}
          onChange={(e) => handleHeadquarterChange(e.target.value)}
          style={selectStyle}
        >
          <option value="">
            {loading ? "부서 목록을 불러오는 중..." : "대상 본부를 선택하세요"}
          </option>
          {headquarters.map((dept) => {
            const deptId = String(getDeptId(dept));
            const deptName = getDeptName(dept);

            return (
              <option key={deptId || deptName} value={deptId}>
                {deptName}
              </option>
            );
          })}
        </select>
      </div>

      <div>
        <div style={labelStyle}>팀</div>
        {!selectedHeadquarter ? (
          <div style={noteStyle}>본부를 선택하면 대상 팀이 표시됩니다.</div>
        ) : (
          <>
            <div style={teamWrapStyle}>
              {teamOptions.map((dept) => {
                const teamId = String(getDeptId(dept));
                const teamName = getDeptName(dept);
                const active = selectedTeamIds.includes(teamId);

                return (
                  <button
                    key={teamId || teamName}
                    type="button"
                    disabled={disabled}
                    onClick={() => toggleTeam(teamId)}
                    style={teamChipStyle(active)}
                  >
                    {teamName}
                  </button>
                );
              })}
            </div>

            <div style={{ ...noteStyle, marginTop: 8 }}>
              {selectedTeamIds.length > 0
                ? `선택 팀: ${normalizeSelection({
                    headquarterId: selectedHeadquarterId,
                    headquarterName: getDeptName(selectedHeadquarter),
                    teamIds: selectedTeamIds,
                    teamNames: selectedTeamIds.map((teamId) => {
                      const matched = teamOptions.find(
                        (dept) => String(getDeptId(dept)) === String(teamId)
                      );
                      return getDeptName(matched);
                    }),
                    displayName: "",
                  }).displayName}`
                : "대상 본부를 선택하면 선택한 팀이 표시됩니다."}
            </div>
          </>
        )}
      </div>

      {error && <div style={{ ...noteStyle, color: "#DC2626" }}>{error}</div>}
    </div>
  );
}
