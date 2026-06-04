import { useLocation, Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";
import styles from "./Layout.module.css";

const FULL_BLEED_ROUTES = ["/messenger"];

export function Layout() {
  const location = useLocation();
  const isFullBleed = FULL_BLEED_ROUTES.includes(location.pathname);

  return (
    <div className={styles.shell}>
      <Sidebar />
      <TopBar pathname={location.pathname} />
      <main className={styles.main}>
        {isFullBleed ? <Outlet /> : <div className={styles.padded}><Outlet /></div>}
      </main>
    </div>
  );
}
