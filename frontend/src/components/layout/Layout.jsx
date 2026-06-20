import { useState } from "react";
import { useLocation, Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";
import styles from "./Layout.module.css";

const FULL_BLEED_ROUTES = ["/messenger"];

export function Layout() {
  const location = useLocation();
  const isFullBleed = FULL_BLEED_ROUTES.includes(location.pathname);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className={styles.shell}>
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <TopBar pathname={location.pathname} onMenuClick={() => setSidebarOpen(true)} />
      <main className={styles.main}>
        {isFullBleed ? <Outlet /> : <div className={styles.padded}><Outlet /></div>}
      </main>
    </div>
  );
}
