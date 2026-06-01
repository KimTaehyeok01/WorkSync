import { createBrowserRouter, Navigate } from "react-router-dom";
import { Layout } from "../components/layout/Layout";
import DashboardPage from "../domains/dashboard/pages/DashboardPage";
import ApprovalListPage from "../domains/approval/pages/ApprovalListPage";
import ApprovalCreatePage from "../domains/approval/pages/ApprovalCreatePage";
import ApprovalDetailPage from "../domains/approval/pages/ApprovalDetailPage";
import BoardListPage from "../domains/board/pages/BoardListPage";
import BoardCreatePage from "../domains/board/pages/BoardCreatePage";
import BoardDetailPage from "../domains/board/pages/BoardDetailPage";
import BoardUpdatePage from "../domains/board/pages/BoardUpdatePage";
import OrganizationListPage from "../domains/organization/pages/OrganizationListPage";
import EmployeeCreatePage from "../domains/organization/pages/EmployeeCreatePage";
import EmployeeEditPage from "../domains/organization/pages/EmployeeEditPage";
import TaskListPage from "../domains/task/pages/TaskListPage";
import TaskCreatePage from "../domains/task/pages/TaskCreatePage";
import TaskDetailPage from "../domains/task/pages/TaskDetailPage";
import TaskUpdatePage from "../domains/task/pages/TaskUpdatePage";
import ChatPage from "../domains/chat/pages/ChatPage";
import LoginPage from "../domains/auth/pages/LoginPage";
import AuditLogPage from "../domains/audit/pages/AuditLogPage";
import styles from "./AppRouter.module.css";
import useAuthContext from "../store/AuthContext";

function NotFound() {
  return (
    <div className={styles.auto_001}>
      <p className={styles.notFoundTitle}>404 - Page Not Found</p>
      <p className={styles.notFoundDesc}>
        The page you are looking for does not exist.
      </p>
    </div>
  );
}

const AuthRouter = ({ Component }) => {
  const { isAuthenticated, isLoading } = useAuthContext();
  if (isLoading) return null;
  return isAuthenticated ? <Component /> : <Navigate to="/login" />;
};

export const router = createBrowserRouter([
  {
    path: "/login",
    Component: LoginPage,
  },
  {
    path: "/",
    element: <AuthRouter Component={Layout} />,
    children: [
      { index: true, Component: DashboardPage },
      { path: "approval", Component: ApprovalListPage },
      { path: "approval/new", Component: ApprovalCreatePage },
      { path: "approval/:id", Component: ApprovalDetailPage },
      { path: "board", Component: BoardListPage },
      { path: "board/new", Component: BoardCreatePage },
      { path: "board/notice", Component: BoardListPage },
      { path: "board/free", Component: BoardListPage },
      { path: "board/qna", Component: BoardListPage },
      { path: "board/:boardId/:postId", Component: BoardDetailPage },
      { path: "board/edit/:boardId/:postId", Component: BoardUpdatePage },
      { path: "organization", Component: OrganizationListPage },
      { path: "organization/employee-add", Component: EmployeeCreatePage },
      { path: "organization", Component: OrganizationListPage },
      { path: "organization/new", Component: EmployeeCreatePage },
      { path: "organization/edit/:id", Component: EmployeeEditPage },
      { path: "tasks", Component: TaskListPage },
      { path: "tasks/new", Component: TaskCreatePage },
      { path: "tasks/:id", Component: TaskDetailPage },
      { path: "tasks/edit/:id", Component: TaskUpdatePage },
      { path: "messenger", Component: ChatPage },
      { path: "audit-log", Component: AuditLogPage },
      { path: "*", Component: NotFound },
    ],
  },
]);
