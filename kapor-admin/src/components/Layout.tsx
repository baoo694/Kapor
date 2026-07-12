import React from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { LayoutDashboard, Users, FileText, LogOut } from 'lucide-react';

export const Layout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    localStorage.removeItem('kapor_admin_token');
    navigate('/login');
  };

  const navItems = [
    { path: '/', label: 'Dashboard', icon: <LayoutDashboard size={20} /> },
    { path: '/users', label: 'Users', icon: <Users size={20} /> },
    { path: '/documents', label: 'Documents', icon: <FileText size={20} /> },
  ];

  return (
    <div className="flex w-full" style={{ height: '100vh', overflow: 'hidden' }}>
      {/* Sidebar */}
      <aside style={{ width: '250px', background: 'var(--color-muted)', borderRight: '1px solid var(--color-border)', display: 'flex', flexDirection: 'column' }}>
        <div className="p-lg" style={{ padding: '24px', borderBottom: '1px solid var(--color-border)' }}>
          <h2 style={{ margin: 0 }}>Kapor Admin</h2>
        </div>
        <nav className="flex-col gap-sm" style={{ padding: '24px', flex: 1 }}>
          {navItems.map(item => {
            const isActive = location.pathname === item.path || (item.path !== '/' && location.pathname.startsWith(item.path));
            return (
              <Link 
                key={item.path} 
                to={item.path}
                className="flex items-center gap-sm"
                style={{
                  padding: '12px 16px',
                  borderRadius: '8px',
                  background: isActive ? 'var(--color-primary)' : 'transparent',
                  color: isActive ? 'var(--color-accent)' : 'var(--color-foreground)',
                  fontWeight: isActive ? 600 : 400,
                  transition: 'all 200ms ease'
                }}
              >
                {item.icon}
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div style={{ padding: '24px', borderTop: '1px solid var(--color-border)' }}>
          <button onClick={handleLogout} className="btn-ghost flex items-center gap-sm w-full" style={{ justifyContent: 'flex-start' }}>
            <LogOut size={20} />
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main style={{ flex: 1, overflowY: 'auto', padding: '32px', background: 'var(--color-background)' }}>
        <Outlet />
      </main>
    </div>
  );
};
