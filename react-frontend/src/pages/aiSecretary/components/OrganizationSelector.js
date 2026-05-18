/**
 * @FileName : OrganizationSelector.js
 * @Description : 사내 AI 포털 관리자 본부 - 팀 - 사원 선택 컴포넌트
 * @Author : 송혜진
 * @Date : 2026. 05. 14
 * @Modification_History
 * @
 * @ 수정일         수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.14    송혜진        최초 생성
 */


import React, { useEffect, useMemo, useRef, useState } from "react";
import Chip from "./Chip";
import { C } from "../styles/aiSecretaryTheme";
import {
  getDepartmentTree,
  getEmployeesByDepartment,
} from "../api/aiSecretaryApi";

const shellStyle = {
  border: `1px solid ${C.border}`,
  borderRadius: 14,
  background: "#fff",
  padding: 14,
};

const labelStyle = {
  fontSize: 13,
  fontWeight: 800,
  color: C.text,
  marginBottom: 8,
};

const controlStyle = {
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

const textareaStyle = {
  width: "100%",
  height: 82,
  minHeight: 82,
  borderRadius: 10,
  border: `1px solid ${C.border}`,
  padding: "10px 12px",
  boxSizing: "border-box",
  fontSize: 14,
  color: C.text,
  background: "#fff",
  outline: "none",
  resize: "vertical",
  lineHeight: 1.5,
};

const employeeWrapStyle = {
  display: "flex",
  gap: 8,
  flexWrap: "wrap",
  maxHeight: 126,
  overflowY: "auto",
  paddingRight: 4,
};

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

const normalizeDeptLabel = (value) => {
  if (!value) {
    return "";
  }

  if (Array.isArray(value)) {
    return value
      .filter(Boolean)
      .map((item) => String(item).trim())
      .filter(Boolean)
      .join(", ");
  }

  if (typeof value === "object") {
    if (value.displayName) {
      return String(value.displayName).trim();
    }

    if (Array.isArray(value.teamNames) && value.teamNames.length > 0) {
      return value.teamNames
        .filter(Boolean)
        .map((item) => String(item).trim())
        .filter(Boolean)
        .join(", ");
    }

    if (value.teamName) return String(value.teamName).trim();
    if (value.deptName) return String(value.deptName).trim();
    if (value.name) return String(value.name).trim();
  }

  return String(value).trim();
};

function normalizeInitialOrganizationSeed(seed) {
  if (!seed) {
    return null;
  }

  if (typeof seed === "string") {
    const deptText = normalizeDeptLabel(seed);
    return deptText ? { deptText } : null;
  }

  if (typeof seed !== "object") {
    return null;
  }

  const headquarterId = String(
    seed.headquarterId ||
      seed.headquarter?.deptId ||
      seed.headquarter?.id ||
      seed.headquarter?.dept_id ||
      ""
  ).trim();
  const headquarterName = normalizeDeptLabel(
    seed.headquarterName ||
      seed.headquarter?.deptName ||
      seed.headquarter?.name ||
      seed.headquarter?.displayName ||
      ""
  );
  const teamIds = Array.isArray(seed.teamIds)
    ? seed.teamIds.map((teamId) => String(teamId || "").trim()).filter(Boolean)
    : [];
  const teamNames = Array.isArray(seed.teamNames)
    ? seed.teamNames.map((name) => normalizeDeptLabel(name)).filter(Boolean)
    : [];
  const displayName = normalizeDeptLabel(
    seed.displayName || teamNames.join(", ") || seed.deptText || ""
  );
  const deptText = normalizeDeptLabel(seed.deptText || displayName);

  if (
    !headquarterId &&
    !headquarterName &&
    teamIds.length === 0 &&
    teamNames.length === 0 &&
    !displayName &&
    !deptText
  ) {
    return null;
  }

  return {
    headquarterId,
    headquarterName,
    teamIds,
    teamNames,
    displayName,
    deptText,
  };
}

function uniquePositions(employees) {
  const map = new Map();

  employees.forEach((employee) => {
    if (employee?.positionId === null || employee?.positionId === undefined) {
      return;
    }

    const key = String(employee.positionId);

    if (!map.has(key)) {
      map.set(key, {
        positionId: employee.positionId,
        positionName: employee.positionName || "",
      });
    }
  });

  return Array.from(map.values()).sort(
    (left, right) => Number(left.positionId || 0) - Number(right.positionId || 0)
  );
}

function normalizePositionName(name) {
  return String(name || "").replace(/\s*?댁긽$/, "");
}

function buildAllEmployeeNos(employees) {
  return (Array.isArray(employees) ? employees : [])
    .map((employee) => String(employee?.empNo || ""))
    .filter(Boolean);
}

function getEmployeeTeamName(employee) {
  return (
    employee?.teamName ??
    employee?.deptName ??
    employee?.departmentName ??
    employee?.department ??
    ""
  );
}

function getEmployeeDisplayName(employee) {
  const name = String(employee?.name || employee?.empName || "").trim();
  const teamName = String(getEmployeeTeamName(employee) || "").trim();

  if (!name) {
    return teamName || "";
  }

  if (!teamName) {
    return name;
  }

  return `${name}(${teamName})`;
}

function mergeEmployeesByEmpNo(groups) {
  const map = new Map();

  groups.flat().forEach((employee) => {
    const key = String(employee?.empNo || "");
    if (!key) {
      return;
    }

    if (!map.has(key)) {
      map.set(key, employee);
      return;
    }

    const current = map.get(key) || {};
    const currentTeamName = getEmployeeTeamName(current);
    const nextTeamName = getEmployeeTeamName(employee);

    map.set(key, {
      ...current,
      ...employee,
      teamName: currentTeamName || nextTeamName || current.teamName || employee.teamName,
      deptName: current.deptName || employee.deptName,
    });
  });

  return Array.from(map.values());
}

export default function OrganizationSelector({
  formType,
  audience,
  targets,
  initialOrganizationSeed = null,
  onChangeFormData,
}) {
  const safeFormType =
    formType === "REPORT" || formType === "MINUTES" || formType === "APPROVAL"
      ? formType
      : "REPORT";

  const [departmentTree, setDepartmentTree] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [selectedHeadquarterId, setSelectedHeadquarterId] = useState("");
  const [selectedTeamIds, setSelectedTeamIds] = useState([]);
  const [selectedPositionId, setSelectedPositionId] = useState("");
  const [selectedEmpNos, setSelectedEmpNos] = useState([]);
  const [referenceNote, setReferenceNote] = useState("");
  const [seedDeptText, setSeedDeptText] = useState("");
  const [loadingDepartments, setLoadingDepartments] = useState(false);
  const [loadingEmployees, setLoadingEmployees] = useState(false);
  const [error, setError] = useState("");
  const lastSyncRef = useRef("");
  const normalizedInitialOrganizationSeed = useMemo(
    () => normalizeInitialOrganizationSeed(initialOrganizationSeed),
    [initialOrganizationSeed]
  );

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

  const selectedTeams = useMemo(
    () =>
      teamOptions.filter((dept) =>
        selectedTeamIds.includes(String(getDeptId(dept)))
      ),
    [selectedTeamIds, teamOptions]
  );

  const positions = useMemo(() => uniquePositions(employees), [employees]);

  const selectedPosition = useMemo(
    () => {
      if (selectedPositionId === "ALL") {
        return {
          positionId: "ALL",
          positionName: "전원",
        };
      }

      return (
        positions.find(
          (item) => String(item.positionId) === String(selectedPositionId)
        ) || null
      );
    },
    [positions, selectedPositionId]
  );

  const visibleEmployees = useMemo(() => {
    if (selectedTeamIds.length === 0 || !selectedPositionId) {
      return [];
    }

    if (selectedPositionId === "ALL") {
      return employees
        .slice()
        .sort((left, right) => {
          const diff =
            Number(left.positionId || 0) - Number(right.positionId || 0);
          if (diff !== 0) {
            return diff;
          }

          return getEmployeeDisplayName(left).localeCompare(
            getEmployeeDisplayName(right),
            "ko"
          );
        });
    }

    return employees
      .filter(
        (employee) =>
          String(employee.positionId) === String(selectedPositionId)
      )
      .sort((left, right) => {
        const diff = Number(left.positionId || 0) - Number(right.positionId || 0);
        if (diff !== 0) {
          return diff;
        }

        return getEmployeeDisplayName(left).localeCompare(
          getEmployeeDisplayName(right),
          "ko"
        );
      });
  }, [employees, selectedPositionId, selectedTeamIds]);

  const selectedEmployees = useMemo(
    () =>
      visibleEmployees.filter((employee) =>
        selectedEmpNos.includes(String(employee.empNo))
      ),
    [selectedEmpNos, visibleEmployees]
  );

  useEffect(() => {
    if (!normalizedInitialOrganizationSeed) {
      setSelectedHeadquarterId("");
      setSelectedTeamIds([]);
      setSelectedPositionId("");
      setSelectedEmpNos([]);
      setEmployees([]);
      setSeedDeptText("");
      setReferenceNote("");
      setError("");
      lastSyncRef.current = "";
      return;
    }

    const nextHeadquarterId = String(
      normalizedInitialOrganizationSeed.headquarterId || ""
    ).trim();
    const nextTeamIds = Array.isArray(normalizedInitialOrganizationSeed.teamIds)
      ? normalizedInitialOrganizationSeed.teamIds
          .map((teamId) => String(teamId || "").trim())
          .filter(Boolean)
      : [];
    const nextDeptText = normalizeDeptLabel(
      normalizedInitialOrganizationSeed.deptText ||
        normalizedInitialOrganizationSeed.displayName ||
        ""
    );

    setSelectedHeadquarterId(nextHeadquarterId);
    setSelectedTeamIds(nextTeamIds);
    setSelectedPositionId(nextTeamIds.length > 0 ? "ALL" : "");
    setSelectedEmpNos([]);
    setEmployees([]);
    setSeedDeptText(nextDeptText);
    setReferenceNote("");
    setError("");
    lastSyncRef.current = "";
  }, [normalizedInitialOrganizationSeed]);

  useEffect(() => {
    let alive = true;

    const loadDepartments = async () => {
      setLoadingDepartments(true);
      setError("");

      try {
        const response = await getDepartmentTree();
        const nextTree = Array.isArray(response?.data) ? response.data : [];

        if (alive) {
          setDepartmentTree(nextTree);
        }
      } catch (fetchError) {
        if (alive) {
          setError("부서 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (alive) {
          setLoadingDepartments(false);
        }
      }
    };

    loadDepartments();

    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    let alive = true;

    const loadEmployees = async () => {
      if (selectedTeamIds.length === 0) {
        setEmployees([]);
        setSelectedEmpNos([]);
        setLoadingEmployees(false);
        return;
      }

      setLoadingEmployees(true);
      setError("");

      try {
        const results = await Promise.all(
          selectedTeamIds.map(async (teamId) => {
            try {
              const response = await getEmployeesByDepartment(teamId);
              const nextEmployees = Array.isArray(response?.data)
                ? response.data
                : [];

              return nextEmployees.map((employee) => ({
                ...employee,
                teamId: String(teamId),
              }));
            } catch (fetchError) {
              return [];
            }
          })
        );
        const nextEmployees = mergeEmployeesByEmpNo(results);

        if (alive) {
          setEmployees(nextEmployees);
        }
      } catch (fetchError) {
        if (alive) {
          setEmployees([]);
          setSelectedEmpNos([]);
          setError("사원 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (alive) {
          setLoadingEmployees(false);
        }
      }
    };

    loadEmployees();

    return () => {
      alive = false;
    };
  }, [selectedTeamIds]);

  useEffect(() => {
    if (selectedTeamIds.length === 0 && !seedDeptText) {
      if (selectedEmpNos.length > 0) {
        setSelectedEmpNos([]);
      }
      return;
    }

    if (
      selectedTeamIds.length > 0 &&
      (employees.length === 0 || !selectedPositionId)
    ) {
      return;
    }

    const nextSelected =
      selectedPositionId === "ALL"
        ? buildAllEmployeeNos(employees)
        : employees
            .filter(
              (employee) =>
                String(employee.positionId) === String(selectedPositionId)
            )
            .map((employee) => String(employee.empNo || ""))
            .filter(Boolean);

    const same =
      nextSelected.length === selectedEmpNos.length &&
      nextSelected.every((item, index) => item === selectedEmpNos[index]);

    if (!same) {
      setSelectedEmpNos(nextSelected);
    }
  }, [employees, selectedEmpNos, selectedPositionId, selectedTeamIds]);

  useEffect(() => {
    const selectedTeamLabels = selectedTeams
      .map((team) => getDeptName(team))
      .filter(Boolean);
    const selectedEmployeeLabels = selectedEmployees
      .map((employee) => getEmployeeDisplayName(employee))
      .filter(Boolean);
    const hasTeamSelection =
      Boolean(selectedHeadquarter?.deptName) || selectedTeamLabels.length > 0;
    const allSelected =
      selectedPositionId === "ALL" &&
      selectedTeamIds.length > 0 &&
      employees.length > 0 &&
      selectedEmployees.length === employees.length;

    const targetLines = [];

    if (selectedHeadquarter?.deptName) {
      targetLines.push(`대상 본부: ${selectedHeadquarter.deptName}`);
    }

    if (selectedTeamLabels.length > 0) {
      targetLines.push(`대상 팀: ${selectedTeamLabels.join(", ")}`);
    } else if (!hasTeamSelection && seedDeptText) {
      targetLines.push(`연관 부서: ${seedDeptText}`);
    }

    if (selectedPosition?.positionName) {
      targetLines.push(
        `직책 기준: ${normalizePositionName(selectedPosition.positionName)}`
      );
    }

    if (selectedPositionId === "ALL") {
      if (allSelected) {
        targetLines.push(`선택 사원: 전원(총 ${selectedEmployees.length}명)`);
      } else if (selectedEmployeeLabels.length > 0) {
        targetLines.push(`선택 사원: ${selectedEmployeeLabels.join(", ")}`);
      }
    } else if (selectedEmployeeLabels.length > 0) {
      targetLines.push(`선택 사원: ${selectedEmployeeLabels.join(", ")}`);
    }

    if (referenceNote.trim()) {
      targetLines.push(`추가 대상/참고사항: ${referenceNote.trim()}`);
    }

    const audienceParts = [];

    if (selectedHeadquarter?.deptName && selectedTeamLabels.length > 0) {
      audienceParts.push(
        `${selectedHeadquarter.deptName} > ${selectedTeamLabels.join(", ")}`
      );
    } else if (seedDeptText) {
      audienceParts.push(`연관 부서: ${seedDeptText}`);
    }

    if (selectedPosition?.positionName) {
      audienceParts.push(normalizePositionName(selectedPosition.positionName));
    }

    if (selectedPositionId === "ALL") {
      if (allSelected) {
        audienceParts.push("전체 사원");
      } else if (selectedEmployeeLabels.length > 0) {
        audienceParts.push(selectedEmployeeLabels.join(", "));
      }
    } else if (selectedEmployeeLabels.length > 0) {
      audienceParts.push(selectedEmployeeLabels.join(", "));
    }

    const nextAudience = audienceParts.filter(Boolean).join(" / ");
    const nextTargets = targetLines;
    const nextSyncKey = `${nextAudience}||${nextTargets.join("\n")}`;

    if (lastSyncRef.current !== nextSyncKey) {
      lastSyncRef.current = nextSyncKey;
      onChangeFormData("audience", nextAudience);
      onChangeFormData("targets", nextTargets);
    }
  }, [
    employees,
    onChangeFormData,
    referenceNote,
    selectedEmployees,
    selectedHeadquarter,
    selectedPosition,
    selectedPositionId,
    selectedTeamIds,
    selectedTeams,
    seedDeptText,
  ]);

  const handleHeadquarterChange = (event) => {
    const nextValue = String(event.target.value || "");
    setSelectedHeadquarterId(nextValue);
    setSelectedTeamIds([]);
    setSelectedPositionId("");
    setSelectedEmpNos([]);
    setEmployees([]);
    setError("");
  };

  const handleTeamToggle = (teamId) => {
    const key = String(teamId || "");

    setSelectedTeamIds((prev) => {
      const exists = prev.includes(key);
      const next = exists
        ? prev.filter((item) => item !== key)
        : [...prev, key];

      if (next.length > 0 && !selectedPositionId) {
        setSelectedPositionId("ALL");
      }

      return next;
    });

    setSelectedEmpNos([]);
    setEmployees([]);
  };

  const handlePositionChange = (event) => {
    const nextValue = String(event.target.value || "");
    setSelectedPositionId(nextValue);

    if (!nextValue) {
      setSelectedEmpNos([]);
      return;
    }

    if (nextValue === "ALL") {
      setSelectedEmpNos(buildAllEmployeeNos(employees));
      return;
    }

    const nextSelected = employees
      .filter(
        (employee) => String(employee.positionId) === String(nextValue)
      )
      .map((employee) => String(employee.empNo || ""))
      .filter(Boolean);

    setSelectedEmpNos(nextSelected);
  };

  const toggleEmployee = (empNo) => {
    if (selectedPositionId === "ALL") {
      return;
    }

    const key = String(empNo);
    setSelectedEmpNos((prev) =>
      prev.includes(key)
        ? prev.filter((item) => item !== key)
        : [...prev, key]
    );
  };

  return (
    <div style={{ ...shellStyle, display: "grid", gap: 12 }}>
      {error && (
        <div
          style={{
            color: "#B91C1C",
            fontSize: 13,
            padding: "10px 12px",
            borderRadius: 10,
            background: "#FEF2F2",
          }}
        >
          {error}
        </div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
          gap: 10,
          alignItems: "start",
        }}
      >
        <div>
          <div style={labelStyle}>대상 본부</div>
          <select
            value={selectedHeadquarterId}
            onChange={handleHeadquarterChange}
            style={controlStyle}
            disabled={loadingDepartments}
          >
          <option value="">
              {loadingDepartments ? "부서 목록을 불러오는 중..." : "대상 본부를 선택하세요"}
          </option>
            {headquarters.map((dept) => (
              <option key={String(getDeptId(dept))} value={String(getDeptId(dept))}>
                {getDeptName(dept)}
              </option>
            ))}
          </select>
        </div>

        <div>
          <div style={labelStyle}>팀</div>
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              gap: 8,
              minHeight: 42,
              alignItems: "center",
            }}
          >
            {teamOptions.map((dept) => {
              const teamId = String(getDeptId(dept));
              const active = selectedTeamIds.includes(teamId);

              return (
                <Chip
                  key={teamId}
                  active={active}
                  onClick={() => handleTeamToggle(teamId)}
                >
                  {getDeptName(dept)}
                </Chip>
              );
            })}
          </div>
        </div>

        <div>
          <div style={labelStyle}>직책 기준</div>
          <select
            value={selectedPositionId}
            onChange={handlePositionChange}
            style={controlStyle}
            disabled={selectedTeamIds.length === 0 || loadingEmployees || positions.length === 0}
          >
            <option value="" disabled>
              직책을 선택하세요
            </option>
            <option value="ALL">전원</option>
            {positions.map((item) => (
              <option key={String(item.positionId)} value={String(item.positionId)}>
                {normalizePositionName(item.positionName)}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div style={{ border: `1px solid ${C.border}`, borderRadius: 12, padding: 12, background: "#fff" }}>
        <div style={labelStyle}>사원 선택</div>

        {!selectedHeadquarterId ? (
          <div style={{ fontSize: 13, color: C.sub }}>
            대상 본부를 선택하면 대상 팀이 표시됩니다.
          </div>
        ) : selectedTeamIds.length === 0 ? (
          <div style={{ fontSize: 13, color: C.sub }}>
            팀을 선택하면 사원 목록이 표시됩니다.
          </div>
        ) : !selectedPositionId ? (
          <div style={{ fontSize: 13, color: C.sub }}>
            직책 기준을 선택하면 사원 목록이 표시됩니다.
          </div>
        ) : loadingEmployees ? (
          <div style={{ fontSize: 13, color: C.sub }}>사원 목록을 불러오는 중...</div>
        ) : visibleEmployees.length === 0 ? (
          <div style={{ fontSize: 13, color: C.sub }}>
            선택 가능한 사원이 없습니다.
          </div>
        ) : (
          <div style={employeeWrapStyle}>
            {visibleEmployees.map((employee) => {
              const active = selectedEmpNos.includes(String(employee.empNo));

              return (
                <Chip
                  key={String(employee.empNo)}
                  active={active}
                  onClick={() => toggleEmployee(employee.empNo)}
                >
                  {getEmployeeDisplayName(employee)}
                </Chip>
              );
            })}
          </div>
        )}
      </div>

      <div style={{ border: `1px solid ${C.border}`, borderRadius: 12, padding: 12, background: "#fff" }}>
        <div style={labelStyle}>추가 대상 / 참고사항</div>
        <textarea
          value={referenceNote}
          onChange={(event) => setReferenceNote(event.target.value)}
          placeholder="추가로 반영할 내용이나 참고 메모를 입력하세요."
          style={textareaStyle}
        />
      </div>
    </div>
  );
}

