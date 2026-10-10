// 통합 검색 결과 페이지 — 직원·게시글·업무·결재·일정·메일을 종류별로 묶어 보여준다
import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Search,
  Users,
  LayoutList,
  CheckSquare,
  FileCheck,
  CalendarDays,
  Mail,
  ChevronRight,
} from "lucide-react";
import useAuthContext from "../../../store/AuthContext";
import { WSEmptyState } from "../../../components/common/CommonWidgets";
import { isOk } from "../../../api/apiRequest";
import {
  TASK_STATUS_LABEL,
  APPROVAL_STATUS_LABEL,
} from "../../../constants/statusLabels";
import { formatRelative } from "../../../utils/dateFormatter";
import { search } from "../services/searchApi";
import s from "./SearchPage.module.css";

const MIN_LENGTH = 2;

// 상태 코드로 시작하는 설명을 한글 라벨로 바꾼다 (예: "IN_PROGRESS · 홍길동")
function localize(description, labels) {
  if (!description) return "";
  const [first, ...rest] = description.split(" · ");
  return [labels[first] ?? first, ...rest].join(" · ");
}

// 종류별 표시 방법과 눌렀을 때 이동할 경로
const SECTIONS = [
  { key: "employees", label: "직원", icon: Users, path: () => "/organization" },
  {
    key: "posts",
    label: "게시글",
    icon: LayoutList,
    path: (item) => `/board/${item.parentId}/${item.id}`,
  },
  {
    key: "tasks",
    label: "업무",
    icon: CheckSquare,
    path: (item) => `/tasks/${item.id}`,
    describe: (item) => localize(item.description, TASK_STATUS_LABEL),
  },
  {
    key: "approvals",
    label: "결재 문서",
    icon: FileCheck,
    path: (item) => `/approval/${item.id}`,
    describe: (item) => localize(item.description, APPROVAL_STATUS_LABEL),
  },
  { key: "schedules", label: "일정", icon: CalendarDays, path: () => "/calendar" },
  { key: "mails", label: "메일", icon: Mail, path: (item) => `/mail?id=${item.id}` },
];

export default function SearchPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuthContext();
  const [searchParams] = useSearchParams();
  const keyword = (searchParams.get("q") ?? "").trim();
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!accessToken) return;
    if (keyword.length < MIN_LENGTH) {
      setResult(null);
      setError("검색어를 2자 이상 입력해 주세요.");
      return;
    }
    search(accessToken, keyword).then((json) => {
      if (!isOk(json)) {
        setResult(null);
        setError(json?.message ?? "검색에 실패했습니다.");
        return;
      }
      setError("");
      setResult(json.data);
    });
  }, [accessToken, keyword]);

  const total = result
    ? SECTIONS.reduce((sum, section) => sum + (result[section.key]?.length ?? 0), 0)
    : 0;

  if (error) {
    return (
      <div className={s.root}>
        <WSEmptyState icon={<Search size={32} />} title={error} />
      </div>
    );
  }
  if (!result) return null;

  return (
    <div className={s.root}>
      <p className={s.summary}>
        <strong>&lsquo;{keyword}&rsquo;</strong> 검색 결과 {total}건
        <span className={s.summarySub}>종류별 최대 5건까지 표시됩니다.</span>
      </p>

      {total === 0 ? (
        <WSEmptyState
          icon={<Search size={32} />}
          title="검색 결과가 없습니다"
          description="다른 검색어로 다시 찾아보세요."
        />
      ) : (
        SECTIONS.filter((section) => result[section.key]?.length > 0).map(
          ({ key, label, icon: Icon, path, describe }) => (
            <section key={key} className={s.section}>
              <h2 className={s.sectionTitle}>
                <Icon size={16} />
                {label}
                <span className={s.count}>{result[key].length}</span>
              </h2>
              <div className={s.items}>
                {result[key].map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={s.item}
                    onClick={() => navigate(path(item))}
                  >
                    <span className={s.itemBody}>
                      <span className={s.itemTitle}>{item.title}</span>
                      <span className={s.itemDesc}>
                        {describe ? describe(item) : item.description}
                      </span>
                    </span>
                    <span className={s.itemDate}>{formatRelative(item.dateAt)}</span>
                    <ChevronRight size={15} className={s.chevron} />
                  </button>
                ))}
              </div>
            </section>
          ),
        )
      )}
    </div>
  );
}
