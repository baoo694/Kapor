import React, { useState, useEffect } from 'react';
import { Card } from '../components/Card';
import { api } from '../services/api';
import { Users, FileText, Activity } from 'lucide-react';

export const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const data = await api.getDashboardStats();
        setStats(data);
      } catch (err) {
        console.error("Failed to load dashboard stats", err);
        // Fallback or handle error
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  return (
    <div>
      <h1 className="mb-lg">Dashboard</h1>
      
      <div className="flex gap-lg mb-lg" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))' }}>
        <Card className="flex items-center gap-md">
          <div style={{ padding: '16px', background: 'rgba(34, 197, 94, 0.1)', borderRadius: '12px', color: 'var(--color-accent)' }}>
            <Users size={32} />
          </div>
          <div>
            <p style={{ margin: 0, color: '#94A3B8', fontSize: '14px' }}>Total Users</p>
            <h2 style={{ margin: 0 }}>{loading ? '...' : (stats?.users || 0)}</h2>
          </div>
        </Card>
        
        <Card className="flex items-center gap-md">
          <div style={{ padding: '16px', background: 'rgba(59, 130, 246, 0.1)', borderRadius: '12px', color: '#60A5FA' }}>
            <FileText size={32} />
          </div>
          <div>
            <p style={{ margin: 0, color: '#94A3B8', fontSize: '14px' }}>Total Content</p>
            <h2 style={{ margin: 0 }}>{loading ? '...' : (stats?.contentCount || 0)}</h2>
          </div>
        </Card>

        <Card className="flex items-center gap-md">
          <div style={{ padding: '16px', background: 'rgba(168, 85, 247, 0.1)', borderRadius: '12px', color: '#C084FC' }}>
            <Activity size={32} />
          </div>
          <div>
            <p style={{ margin: 0, color: '#94A3B8', fontSize: '14px' }}>DAU</p>
            <h2 style={{ margin: 0 }}>{loading ? '...' : (stats?.dau || 0)}</h2>
          </div>
        </Card>
      </div>
    </div>
  );
};
