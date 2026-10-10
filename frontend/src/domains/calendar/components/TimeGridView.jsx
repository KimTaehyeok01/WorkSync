// 주간·일간 일정을 시간 눈금 위에 배치해 보여주는 타임 그리드
import { useEffect, useRef } from "react";
import { Repeat } from "lucide-react";
import { WEEKDAYS } from "../../../constants/statusLabels";
import s from "./TimeGridView.module.css";

const HOUR_LABELS = Array.from(
  { length: 24 },
  (_, hour) => `${String(hour).padStart(2, "0")}:00`,
);
// 시간 한 칸의 높이(px) — CSS에는 --hour-height 변수로 넘긴다
const HOUR_HEIGHT = 48;
// 짧은 일정도 글자가 보이도록 잡는 최소 길이(분)
const MIN_MINUTES = 30;

// 날짜 문자열의 요일을 로컬 기준으로 구한다 — new Date("yyyy-MM-dd")는 UTC로 해석되어 쓰지 않는다
function getDayOfWeek(day) {
  const [year, month, date] = day.split("-").map(Number);
  return new Date(year, month - 1, date).getDay();
}

// "yyyy-MM-ddTHH:mm:ss"의 시각을 0시 기준 분으로 바꾼다
function toMinutes(dateTime) {
  return Number(dateTime.slice(11, 13)) * 60 + Number(dateTime.slice(14, 16));
}

// 종일이 아니고 하루 안에서 끝나는 일정만 시간 눈금에 놓는다 — 나머지는 종일 줄에 표시한다
function isTimed(sc) {
  return !sc.isAllDay && sc.startAt.slice(0, 10) === sc.endAt.slice(0, 10);
}

// 겹치는 일정을 나란히 놓는 레인 배정 — 시작 순으로 끝난 레인을 재사용하고, 겹침 그룹의 레인 수로 폭을 나눈다
function layoutBlocks(schedules) {
  const blocks = schedules
    .map((sc) => {
      // 최소 길이를 적용해도 자정을 넘지 않도록 시작 위치를 당긴다
      const start = Math.min(toMinutes(sc.startAt), 24 * 60 - MIN_MINUTES);
      return { sc, start, end: Math.max(toMinutes(sc.endAt), start + MIN_MINUTES) };
    })
    .sort((a, b) => a.start - b.start);

  let laneEnds = [];
  let group = null;
  let groupEnd = 0;
  blocks.forEach((block) => {
    // 앞선 일정들과 겹치지 않으면 새 겹침 그룹을 시작한다
    if (block.start >= groupEnd) {
      laneEnds = [];
      group = { lanes: 0 };
    }
    const free = laneEnds.findIndex((end) => end <= block.start);
    block.lane = free === -1 ? laneEnds.length : free;
    laneEnds[block.lane] = block.end;
    group.lanes = laneEnds.length;
    block.group = group;
    groupEnd = Math.max(groupEnd, block.end);
  });
  return blocks;
}

export default function TimeGridView({
  days,
  today,
  getDayItems,
  scopeClass,
  renderExtras,
  onCreate,
  onSelect,
}) {
  const scrollRef = useRef(null);

  // 업무 시간대가 먼저 보이도록 08시 위치에서 시작한다
  useEffect(() => {
    scrollRef.current.scrollTop = 8 * HOUR_HEIGHT;
  }, []);

  const columns = days.map((day) => ({ day, items: getDayItems(day) }));

  return (
    <div
      ref={scrollRef}
      className={s.root}
      style={{ "--cols": days.length, "--hour-height": `${HOUR_HEIGHT}px` }}
    >
      <div className={s.head}>
        <div className={s.row}>
          <div className={s.corner} />
          {days.map((day) => {
            const dayOfWeek = getDayOfWeek(day);
            return (
              <div
                key={day}
                className={`${s.dayHead} ${dayOfWeek === 0 ? s.sunday : ""} ${dayOfWeek === 6 ? s.saturday : ""}`}
              >
                <span>{WEEKDAYS[dayOfWeek]}</span>
                <span className={`${s.dayNumber} ${day === today ? s.today : ""}`}>
                  {Number(day.slice(8))}
                </span>
              </div>
            );
          })}
        </div>

        <div className={s.row}>
          <div className={s.allDayLabel}>종일</div>
          {columns.map(({ day, items }) => (
            <div key={day} className={s.allDayCell}>
              {items.schedules
                .filter((sc) => !isTimed(sc))
                .map((sc) => (
                  <button
                    key={`${sc.id}-${sc.startAt}`}
                    type="button"
                    className={`${s.chip} ${scopeClass[sc.scope]}`}
                    title={sc.title}
                    onClick={() => onSelect(sc, day)}
                  >
                    {sc.repeatType !== "NONE" && (
                      <Repeat size={10} className={s.icon} />
                    )}
                    {!sc.isAllDay && sc.startAt.slice(0, 10) === day && (
                      <span className={s.time}>{sc.startAt.slice(11, 16)}</span>
                    )}
                    {sc.title}
                  </button>
                ))}
              {renderExtras(items, s.chip)}
            </div>
          ))}
        </div>
      </div>

      <div className={s.row}>
        <div>
          {HOUR_LABELS.map((label) => (
            <div key={label} className={s.timeLabel}>
              {label}
            </div>
          ))}
        </div>

        {columns.map(({ day, items }) => (
          <div key={day} className={s.dayColumn}>
            {HOUR_LABELS.map((label, hour) => (
              <button
                key={label}
                type="button"
                className={s.hourCell}
                tabIndex={-1}
                aria-label={`${day} ${label} 일정 등록`}
                onClick={() => onCreate(day, hour)}
              />
            ))}

            {layoutBlocks(items.schedules.filter(isTimed)).map(
              ({ sc, start, end, lane, group }) => (
                <button
                  key={`${sc.id}-${sc.startAt}`}
                  type="button"
                  className={`${s.block} ${scopeClass[sc.scope]}`}
                  style={{
                    top: (start / 60) * HOUR_HEIGHT,
                    height: ((end - start) / 60) * HOUR_HEIGHT,
                    left: `${(lane / group.lanes) * 100}%`,
                    width: `${100 / group.lanes}%`,
                  }}
                  title={sc.title}
                  onClick={() => onSelect(sc, day)}
                >
                  {/* button은 내용을 세로 가운데에 두므로, 위에서부터 흐르도록 한 번 감싼다 */}
                  <span>
                    {sc.repeatType !== "NONE" && (
                      <Repeat size={10} className={s.icon} />
                    )}
                    <span className={s.time}>{sc.startAt.slice(11, 16)}</span>
                    {sc.title}
                  </span>
                </button>
              ),
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
