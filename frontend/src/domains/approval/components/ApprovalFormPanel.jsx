import { useEffect, useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { WSCard } from "../../../components/common/CommonWidgets";
import s from "./ApprovalFormPanel.module.css";

// 지출결의서(EXPENSE) items 필드 정의
// - reason: 지출 사유
// - amount: 금액 합계
// - items: 사용 내역 배열 (JSON 문자열)

// 지출 결의서 양식 컴포넌트
// formValues: 현재 입력값 객체
// setFormValues: 입력값 업데이트 함수
function ExpenseForm({ formValues, setFormValues, myInfo, title, setTitle }) {
  const [initialized, setInitialized] = useState(false);
  // 사용 내역 행 목록 (처음엔 빈 행 1개)
  const [rows, setRows] = useState([
    {
      id: Date.now(),
      date: "",
      category: "",
      description: "",
      amount: "",
      note: "",
    },
  ]);

  // "+ 사용 내역 추가" 버튼 클릭 시 빈 행 하나 추가
  const addRow = () =>
    setRows((prev) => [
      ...prev,
      {
        id: Date.now(),
        date: "",
        category: "",
        description: "",
        amount: "",
        note: "",
      },
    ]);

  // 휴지통 버튼 클릭 시 해당 행 삭제
  const delRow = (id) => setRows((prev) => prev.filter((r) => r.id !== id));

  // 각 셀 입력값 변경 시 해당 행의 특정 key 값을 업데이트
  const updateRow = (id, key, value) => {
    const updated = rows.map((r) => (r.id === id ? { ...r, [key]: value } : r));
    setRows(updated);
    // 부모 ApprovalCreatePage의 formValues에도 반영
    setFormValues((prev) => ({ ...prev, items: updated }));
  };

  // 금액 합계 계산 (amount를 숫자로 변환 후 합산)
  const total = rows.reduce((sum, r) => sum + (Number(r.amount) || 0), 0);
  useEffect(() => {
    setFormValues((prev) => ({ ...prev, amount: total }));
  }, [total]);

  useEffect(() => {
    if (myInfo) {
      setFormValues((prev) => ({
        ...prev,
        departmentName: myInfo.departmentName,
        name: myInfo.name,
      }));
    }
  }, [myInfo]);

  // rows 값 초기세팅
  useEffect(() => {
    if (initialized) return;
    if (!formValues.items) return;
    try {
      const parsed = JSON.parse(formValues.items);
      if (parsed.length > 0) {
        setRows(parsed);
        setInitialized(true);
      }
    } catch {}
  }, [formValues.items]);

  const CATEGORIES = ["물품구입비", "교통비", "식비", "숙박비", "기타"];

  return (
    <>
      {/* 기본 정보 */}
      <WSCard title="기본 정보" subtitle="결재 문서의 기본 정보를 입력하세요">
        <div className={s.formGrid}>
          <div className={s.row2}>
            <div>
              <label className={s.label}>
                소속<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.departmentName ?? ""}
                readOnly
                className={s.input}
              />
            </div>
            <div>
              <label className={s.label}>
                작성자<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.name ?? ""}
                readOnly
                className={s.input}
              />
            </div>
          </div>
          <div>
            <label className={s.label}>
              제목 <span className={s.required}>*</span>
            </label>
            <input
              type="text"
              placeholder="결재 문서 제목을 입력하세요"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className={s.input}
            />
          </div>
          <div>
            <label className={s.label}>
              지출 사유 <span className={s.required}>*</span>
            </label>
            <textarea
              placeholder="지출 사유를 입력하세요."
              value={formValues.reason ?? ""}
              onChange={(e) =>
                setFormValues((prev) => ({ ...prev, reason: e.target.value }))
              }
              className={s.textarea}
            ></textarea>
          </div>
          <div>
            <label className={s.label}>
              금액 합계<span className={s.required}>*</span>
            </label>
            <input
              type="number"
              value={formValues.amount ?? ""}
              readOnly
              className={s.input}
            />
          </div>
        </div>
      </WSCard>

      <WSCard
        title="사용 내역"
        subtitle="영수증은 첨부파일 또는 별도로 제출하세요"
      >
        {/* 행 추가 버튼 */}
        <button onClick={addRow} className={s.addRowBtn} type="button">
          <Plus size={14} />
          사용 내역 추가
        </button>

        {/* 행 목록 렌더링 */}
        {rows.map((row) => (
          <div key={row.id} className={s.tableRow}>
            {/* 일자 */}
            <input
              type="date"
              value={row.date}
              onChange={(e) => updateRow(row.id, "date", e.target.value)}
              className={s.tableInput}
              style={{ width: 130, flex: "none" }}
            />
            {/* 분류 선택 */}
            <select
              value={row.category}
              onChange={(e) => updateRow(row.id, "category", e.target.value)}
              className={s.tableSelect}
              style={{ width: 110, flex: "none" }}
            >
              <option value="">분류 선택</option>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
            {/* 사용 내역 */}
            <input
              type="text"
              placeholder="사용 내역"
              value={row.description}
              onChange={(e) => updateRow(row.id, "description", e.target.value)}
              className={s.tableInput}
              style={{ flex: 1 }}
            />
            {/* 금액 */}
            <input
              type="number"
              placeholder="0"
              value={row.amount}
              onChange={(e) => updateRow(row.id, "amount", e.target.value)}
              className={s.tableInput}
              style={{ width: 90, flex: "none" }}
            />
            {/* 비고 */}
            <input
              type="text"
              placeholder="비고"
              value={row.note}
              onChange={(e) => updateRow(row.id, "note", e.target.value)}
              className={s.tableInput}
              style={{ width: 80, flex: "none" }}
            />
            {/* 행 삭제 버튼 */}
            <button
              onClick={() => delRow(row.id)}
              className={s.delRowBtn}
              type="button"
            >
              <Trash2 size={14} />
            </button>
          </div>
        ))}
      </WSCard>
    </>
  );
}

// 연차신청서(LEAVE) items 필드 정의
// - leaveType: 휴가 종류
// - days: 휴가 기간
// - reason: 휴가 사유

// 연차 신청서 양식 컴포넌트
function LeaveForm({ formValues, setFormValues, myInfo, title, setTitle }) {
  console.log("myInfo:", myInfo);
  // 특정 key의 값만 업데이트하는 함수
  // ex: update("reason", "개인 사유") -> formValues.reason = "개인 사유"
  const update = (key, value) =>
    setFormValues((prev) => ({ ...prev, [key]: value }));

  // 휴가 종류
  const [leaveType, setLeaveType] = useState("ANNUAL");
  // 휴가 기간
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  // 반차
  const [halfDate, setHalfDate] = useState("");
  const [halfTime, setHalfTime] = useState("");

  useEffect(() => {
    if (myInfo) {
      setFormValues((prev) => ({
        ...prev,
        departmentName: myInfo.departmentName,
        name: myInfo.name,
        leaveType: "ANNUAL",
      }));
      setLeaveType("ANNUAL");
    }
  }, [myInfo]);

  return (
    <>
      {/* 기본 정보 */}
      <WSCard title="기본 정보" subtitle="결재 문서의 기본 정보를 입력하세요">
        <div className={s.formGrid}>
          <div className={s.row2}>
            <div>
              <label className={s.label}>
                소속<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.departmentName ?? ""}
                readOnly
                className={s.input}
              />
            </div>
            <div>
              <label className={s.label}>
                작성자<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.name ?? ""}
                readOnly
                className={s.input}
              />
            </div>
          </div>
          <div>
            <label className={s.label}>
              제목<span className={s.required}>*</span>
            </label>
            <input
              type="text"
              placeholder="결재 문서 제목을 입력하세요"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className={s.input}
            />
          </div>
          <div className={s.row2}>
            {/* 휴가 종류 선택 */}
            <div>
              <label className={s.label}>
                휴가 종류 <span className={s.required}>*</span>
              </label>
              <select
                value={leaveType}
                onChange={(e) => {
                  setLeaveType(e.target.value);
                  setFormValues((prev) => ({
                    ...prev,
                    leaveType: e.target.value,
                  }));
                }}
                className={s.select}
              >
                <option value="">선택</option>
                <option value="ANNUAL">연차</option>
                <option value="HALF">반차</option>
                <option value="SICK">병가</option>
                <option value="OTHER">휴가</option>
              </select>
            </div>
            <div className={s.row2}>
              {/* 휴가 기간(날짜 선택) */}
              {leaveType === "HALF" ? (
                // 반차 - 날짜 + 오전/오후 선택
                <div style={{ gridColumn: "1 / -1" }}>
                  <label className={s.label}>
                    휴가 기간 <span className={s.required}>*</span>
                  </label>
                  <div style={{ display: "flex", width: "100%", minWidth: 0 }}>
                    <input
                      type="date"
                      value={halfDate}
                      className={s.input}
                      style={{ flex: 1, marginRight: "15px", minWidth: 0 }}
                      onChange={(e) => {
                        setHalfDate(e.target.value);
                        setFormValues((prev) => ({
                          ...prev,
                          halfDate: e.target.value,
                        }));
                      }}
                    />
                    <select
                      value={halfTime}
                      className={s.input}
                      style={{ flex: 1, minWidth: 0 }}
                      onChange={(e) => {
                        setHalfTime(e.target.value);
                        setFormValues((prev) => ({
                          ...prev,
                          halfTime: e.target.value,
                        }));
                      }}
                    >
                      <option value="">선택</option>
                      <option value="AM">오전</option>
                      <option value="PM">오후</option>
                    </select>
                  </div>
                </div>
              ) : (
                leaveType !== "HALF" &&
                leaveType && (
                  <div style={{ gridColumn: "1 / -1" }}>
                    <label className={s.label}>
                      휴가 기간 <span className={s.required}>*</span>
                    </label>
                    <div
                      style={{ display: "flex", width: "100%", minWidth: 0 }}
                    >
                      <input
                        type="date"
                        value={startDate}
                        className={s.input}
                        style={{ flex: 1, minWidth: 0 }}
                        onChange={(e) => {
                          setStartDate(e.target.value);
                          setFormValues((prev) => ({
                            ...prev,
                            startDate: e.target.value,
                          }));
                        }}
                      />
                      <span style={{ padding: "5px", flexShrink: 0 }}>-</span>
                      <input
                        type="date"
                        value={endDate}
                        className={s.input}
                        style={{ flex: 1, minWidth: 0 }}
                        onChange={(e) => {
                          setEndDate(e.target.value);
                          setFormValues((prev) => ({
                            ...prev,
                            endDate: e.target.value,
                          }));
                        }}
                      />
                    </div>
                  </div>
                )
              )}
            </div>
          </div>

          {/* 휴가 사유 텍스트 입력 */}
          <div>
            <label className={s.label}>
              휴가 사유 <span className={s.required}>*</span>
            </label>
            <textarea
              placeholder="휴가 사유를 입력하세요."
              value={formValues.reason ?? ""}
              onChange={(e) => update("reason", e.target.value)}
              className={s.textarea}
            ></textarea>
          </div>
        </div>
      </WSCard>
    </>
  );
}

// 구매요청서(PURCHASE) items 필드 정의
// - purpose: 구매 용도
// - amount: 금액 합계
// - items: 구매 목록 배열 (JSON 문자열)

// 구매 요청서 양식 컴포넌트
function PurchaseForm({ formValues, setFormValues, myInfo, title, setTitle }) {
  const [initialized, setInitialized] = useState(false);
  // 구매 요청 행 목록
  const [rows, setRows] = useState([
    {
      id: Date.now(),
      item: "",
      quantity: "",
      unitPrice: "",
      amount: "",
      note: "",
    },
  ]);

  //rows 값 초기세팅
  useEffect(() => {
    if (initialized) return;
    if (!formValues.items) return;
    try {
      const parsed = JSON.parse(formValues.items);
      if (parsed.length > 0) {
        setRows(parsed);
        setInitialized(true);
      }
    } catch {}
  }, [formValues.items]);

  const addRow = () =>
    setRows((prev) => [
      ...prev,
      {
        id: Date.now(),
        item: "",
        quantity: "",
        unitPrice: "",
        amount: "",
        note: "",
      },
    ]);

  const delRow = (id) => setRows((prev) => prev.filter((r) => r.id !== id));

  // 수량 또는 단가 변경 시 금액 자동 계산
  const updateRow = (id, key, value) => {
    const updated = rows.map((r) => {
      if (r.id !== id) return r;
      const newRow = { ...r, [key]: value };
      // 수량 * 단가 = 금액 자동 계산
      if (key === "quantity" || key === "unitPrice") {
        newRow.amount =
          (Number(newRow.quantity) || 0) * (Number(newRow.unitPrice) || 0);
      }
      return newRow;
    });
    setRows(updated);
    setFormValues((prev) => ({ ...prev, items: updated }));
  };

  const total = rows.reduce((sum, r) => sum + (Number(r.amount) || 0), 0);
  useEffect(() => {
    setFormValues((prev) => ({ ...prev, amount: total }));
  }, [total]);

  useEffect(() => {
    if (myInfo) {
      setFormValues((prev) => ({
        ...prev,
        departmentName: myInfo.departmentName,
        name: myInfo.name,
      }));
    }
  }, [myInfo]);

  return (
    <>
      {/* 기본 정보 */}
      <WSCard title="기본 정보" submit="결재 문서의 기본 정보를 입력하세요">
        <div className={s.formGrid}>
          <div className={s.row2}>
            <div>
              <label className={s.label}>
                소속<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.departmentName ?? ""}
                className={s.input}
                readOnly
              />
            </div>
            <div>
              <label className={s.label}>
                작성자<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.name ?? ""}
                className={s.input}
                readOnly
              />
            </div>
          </div>
          <div>
            <label className={s.label}>
              제목<span className={s.required}>*</span>
            </label>
            <input
              type="text"
              placeholder="결재 문서 제목을 입력하세요"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className={s.input}
            />
          </div>
          <div>
            <label className={s.label}>
              구매 용도<span className={s.required}>*</span>
            </label>
            <textarea
              type="text"
              placeholder="구매 용도를 입력하세요"
              value={formValues.purpose ?? ""}
              onChange={(e) =>
                setFormValues((prev) => ({ ...prev, purpose: e.target.value }))
              }
              className={s.textarea}
            />
          </div>
          <div>
            <label className={s.label}>
              요청 날짜<span className={s.required}>*</span>
            </label>
            <input
              type="date"
              value={formValues.date ?? ""}
              onChange={(e) =>
                setFormValues((prev) => ({ ...prev, date: e.target.value }))
              }
              className={s.input}
            />
          </div>
          <div>
            <label className={s.label}>
              금액 합계<span className={s.required}>*</span>
            </label>
            <input
              type="number"
              value={formValues.amount ?? ""}
              readOnly
              className={s.input}
            />
          </div>
        </div>
      </WSCard>
      <WSCard
        title="구매 요청 내역"
        subtitle="영수증은 첨부파일 또는 별도로 제출하세요"
      >
        <button onClick={addRow} className={s.addRowBtn} type="button">
          <Plus size={14} />
          구매 요청 내역 추가
        </button>
        {rows.map((row) => (
          <div key={row.id} className={s.tableRow}>
            {/* 요청 내역 */}
            <input
              type="text"
              placeholder="요청 내역"
              value={row.item}
              onChange={(e) => updateRow(row.id, "item", e.target.value)}
              className={s.tableInput}
              style={{ maxWidth: 500 }}
            />
            {/* 수량 */}
            <input
              type="number"
              placeholder="수량"
              value={row.quantity}
              onChange={(e) => updateRow(row.id, "quantity", e.target.value)}
              className={s.tableInput}
              style={{ maxWidth: 130 }}
            />
            {/* 단가 */}
            <input
              type="number"
              placeholder="단가"
              value={row.unitPrice}
              onChange={(e) => updateRow(row.id, "unitPrice", e.target.value)}
              className={s.tableInput}
              style={{ maxWidth: 130 }}
            />
            {/* 금액 (수량 * 단가 자동 계산, 읽기 전용) */}
            <input
              type="number"
              placeholder="금액"
              value={row.amount}
              readOnly
              className={s.tableInput}
              style={{ maxWidth: 130 }}
            />

            {/* 비고
             */}
            <input
              type="text"
              placeholder="비고"
              value={row.note}
              onChange={(e) => updateRow(row.id, "note", e.target.value)}
              className={s.tableInput}
              style={{ maxWidth: 200 }}
            />
            <button
              onClick={() => delRow(row.id)}
              className={s.delRowBtn}
              type="button"
            >
              <Trash2 size={14} />
            </button>
          </div>
        ))}
      </WSCard>
    </>
  );
}

// 출장신청서(BUSINESS_TRIP) items 필드 정의
// - executionDate: 시행 일자
// - destination: 출장지
// - travelers: 출장자 배열 (JSON 문자열)
// - expenses: 출장비 배열 (JSON 문자열)

// 출장 신청서 양식 컴포넌트

function BusinessTripForm({
  formValues,
  setFormValues,
  myInfo,
  title,
  setTitle,
  employees = [],
}) {
  const [initialized, setInitialized] = useState(false);
  // 출장자 행 목록
  const [rows, setRows] = useState([
    { id: Date.now(), name: "", dept: "", grade: "", empNo: "" },
  ]);
  // 출장비 행 목록
  const [expenses, setExpenses] = useState([
    { id: Date.now(), category: "", amount: "", note: "" },
  ]);

  // 단순 텍스트 필드 업데이트 헬퍼
  const update = (key, value) => {
    setFormValues((prev) => ({ ...prev, [key]: value }));
  };

  // 출장자 행 추가/삭제/수정
  const addTraveler = () =>
    setRows((prev) => [
      ...prev,
      { id: Date.now(), name: "", dept: "", grade: "", empNo: "" },
    ]);
  const delTraveler = (id) =>
    setRows((prev) => prev.filter((r) => r.id !== id));
  const updateTraveler = (id, key, value) =>
    setRows((prev) =>
      prev.map((r) => (r.id === id ? { ...r, [key]: value } : r)),
    );

  // 출장비 행 추가/삭제/수정
  const addExpense = () =>
    setExpenses((prev) => [
      ...prev,
      { id: Date.now(), category: "", amount: "", note: "" },
    ]);
  const delExpense = (id) =>
    setExpenses((prev) => prev.filter((r) => r.id !== id));
  const updateExpense = (id, key, value) =>
    setExpenses((prev) =>
      prev.map((r) => (r.id === id ? { ...r, [key]: value } : r)),
    );

  useEffect(() => {
    if (myInfo) {
      setFormValues((prev) => ({
        ...prev,
        departmentName: myInfo.departmentName,
        name: myInfo.name,
      }));
    }
  }, [myInfo]);

  // 출장자(rows), 출장비(expenses) 변경시  formValues에 반영
  useEffect(() => {
    setFormValues((prev) => ({
      ...prev,
      travelers: rows,
      expenses: expenses,
    }));
  }, [rows, expenses]);

  //rows 값 초기세팅
  useEffect(() => {
    if (initialized) return;
    if (!formValues.travelers && !formValues.expenses) return;
    try {
      const parsedTravelers = JSON.parse(formValues.travelers ?? "[]");
      const parsedExpenses = JSON.parse(formValues.expenses ?? "[]");
      if (parsedTravelers.length > 0) setRows(parsedTravelers);
      if (parsedExpenses.length > 0) setExpenses(parsedExpenses);
      setInitialized(true);
    } catch {}
  }, [formValues.travelers, formValues.expenses]);

  const EXPENSE_CATEGORIES = ["교통비", "숙박비", "식비", "기타"];

  return (
    <>
      {/* 기본 정보 */}
      <WSCard title="시행 정보" subtitle="결재 문서의 기본 정보를 입력하세요">
        <div className={s.formGrid}>
          <div className={s.row2}>
            <div>
              <label className={s.label}>
                시행자<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.name ?? ""}
                readOnly
                className={s.input}
              />
            </div>
            <div>
              <label className={s.label}>
                소속<span className={s.required}>*</span>
              </label>
              <input
                type="text"
                value={myInfo?.departmentName ?? ""}
                readOnly
                className={s.input}
              />
            </div>
          </div>
          <div>
            <label className={s.label}>
              시행 일자<span className={s.required}>*</span>
            </label>
            <div className={s.row2}>
              <input
                type="date"
                value={formValues.executionStartDate ?? ""}
                onChange={(e) =>
                  setFormValues((prev) => ({
                    ...prev,
                    executionStartDate: e.target.value,
                  }))
                }
                className={s.input}
              />
              <input
                type="date"
                value={formValues.executionEndDate ?? ""}
                onChange={(e) =>
                  setFormValues((prev) => ({
                    ...prev,
                    executionEndDate: e.target.value,
                  }))
                }
                className={s.input}
              />
            </div>
          </div>
          <div>
            <label className={s.label}>
              제목<span className={s.required}>*</span>
            </label>
            <input
              type="text"
              placeholder="결재 문서 제목을 입력하세요"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className={s.input}
            />
          </div>
        </div>
      </WSCard>
      {/* 출장자 섹션 */}
      <WSCard title="출장자" subtitle="출장자 성명과 부서를 입력해 주세요">
        <button onClick={addTraveler} className={s.addRowBtn} type="button">
          <Plus size={14} />
          출장 인원 추가
        </button>
        {rows.map((row) => (
          <div key={row.id} className={s.tableRow}>
            <select
              value={row.empNo}
              onChange={(e) => {
                const selected = employees.find(
                  (emp) => emp.empNo === e.target.value,
                );

                if (selected) {
                  updateTraveler(row.id, "name", selected.name);
                  updateTraveler(row.id, "dept", selected.departmentName);
                  updateTraveler(row.id, "grade", selected.jobGrade);
                  updateTraveler(row.id, "empNo", selected.empNo);
                }
              }}
              className={s.input}
            >
              <option value="">성명 선택</option>
              {employees
                .filter((emp) => emp.departmentName === myInfo?.departmentName)
                .map((emp) => (
                  <option key={emp.id} value={emp.empNo}>
                    {emp.name}
                  </option>
                ))}
            </select>
            <input
              type="text"
              placeholder="소속"
              value={row.dept}
              onChange={(e) => updateTraveler(row.id, "dept", e.target.value)}
              className={s.input}
            />
            <input
              type="text"
              placeholder="직급"
              value={row.grade}
              onChange={(e) => updateTraveler(row.id, "grade", e.target.value)}
              className={s.input}
            />
            <input
              type="text"
              placeholder="사번"
              value={row.empNo}
              className={s.input}
              onChange={(e) => updateTraveler(row.id, "empNo", e.target.value)}
            />
            <button
              onClick={() => delTraveler(row.id)}
              className={s.delRowBtn}
              type="button"
            >
              <Trash2 size={14} />
            </button>
          </div>
        ))}
      </WSCard>
      {/* 출장지 및 일정 섹션 */}
      <WSCard
        title="출장지 및 일정별"
        subtitle="출장지, 일자별 방문처 및 해당 일자의 업무 내용을 작성해주세요."
      >
        <textarea
          placeholder="출장지를 입력하세요."
          value={formValues.destination ?? ""}
          onChange={(e) => update("destination", e.target.value)}
          className={s.textarea}
        ></textarea>
      </WSCard>
      {/* 출장비 섹션 */}
      <WSCard
        title="출장비"
        subtitle="항목별 금액과 산출 내역을 상세히 입력해주세요"
      >
        <button onClick={addExpense} className={s.addRowBtn} type="button">
          <Plus size={14} />
          항목 추가
        </button>
        {expenses.map((row) => (
          <div key={row.id} className={s.tableRow}>
            <select
              value={row.category}
              onChange={(e) =>
                updateExpense(row.id, "category", e.target.value)
              }
              className={s.tableSelect}
            >
              <option value="">항목 선택</option>
              {EXPENSE_CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
            <input
              type="number"
              placeholder="금액"
              value={row.amount}
              onChange={(e) => updateExpense(row.id, "amount", e.target.value)}
              className={s.tableInput}
              style={{ maxWidth: 120 }}
            />
            <input
              type="text"
              placeholder="산출 내역"
              value={row.note}
              onChange={(e) => updateExpense(row.id, "note", e.target.value)}
              className={s.tableInput}
            />
            <button
              onClick={() => delExpense(row.id)}
              className={s.delRowBtn}
              type="button"
            >
              <Trash2 size={14} />
            </button>
          </div>
        ))}
      </WSCard>
    </>
  );
}

// 메인 패널
export default function ApprovalFormPanel({
  selectedForm,
  formValues,
  setFormValues,
  myInfo,
  title,
  setTitle,
  employees,
}) {
  if (!selectedForm) return null;

  const { formType } = selectedForm;

  return (
    <div className={s.panel}>
      {/* <div className={s.panelHeader}>
        <h2 className={s.panelTitle}>{selectedForm.formName}</h2>
        <p className={s.panelSub}>양식 내용을 입력하세요.</p>
      </div> */}
      {formType === "EXPENSE" && myInfo && (
        <ExpenseForm
          formValues={formValues}
          setFormValues={setFormValues}
          myInfo={myInfo}
          title={title}
          setTitle={setTitle}
        />
      )}
      {formType === "LEAVE" && myInfo && (
        <LeaveForm
          formValues={formValues}
          setFormValues={setFormValues}
          myInfo={myInfo}
          title={title}
          setTitle={setTitle}
        />
      )}
      {formType === "PURCHASE" && myInfo && (
        <PurchaseForm
          formValues={formValues}
          setFormValues={setFormValues}
          myInfo={myInfo}
          title={title}
          setTitle={setTitle}
        />
      )}
      {formType === "BUSINESS_TRIP" && myInfo && (
        <BusinessTripForm
          formValues={formValues}
          setFormValues={setFormValues}
          myInfo={myInfo}
          title={title}
          setTitle={setTitle}
          employees={employees}
        />
      )}
    </div>
  );
}
