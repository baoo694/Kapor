const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const getHeaders = () => {
  const token = localStorage.getItem('kapor_admin_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
};

// Helper to unwrap standard ApiResponse { success, data, message }
const handleResponse = async (res: Response) => {
  if (!res.ok) {
    let message = 'API request failed';
    try {
      const errJson = await res.json();
      message = errJson.message || message;
    } catch (e) {}
    throw new Error(message);
  }
  const json = await res.json();
  if (json.success !== undefined) {
    if (!json.success) throw new Error(json.message || 'API request failed');
    return json.data;
  }
  return json;
};

export const api = {
  // Auth API
  login: async (credentials: any) => {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials)
    });
    return handleResponse(res);
  },

  // Users API
  getUsers: async (page = 1, search = '') => {
    try {
      const res = await fetch(`${API_BASE}/api/admin/users?page=${page}&search=${search}`, { headers: getHeaders() });
      return await handleResponse(res);
    } catch (e) {
      console.warn("getUsers failed, probably not implemented yet.", e);
      return [];
    }
  },
  
  updateUserRole: async (id: string, role: string) => {
    const res = await fetch(`${API_BASE}/api/admin/users/${id}/role`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify({ role })
    });
    return handleResponse(res);
  },
  
  deleteUser: async (id: string) => {
    const res = await fetch(`${API_BASE}/api/admin/users/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  // Videos (Mapping to Documents/Resources for this admin dashboard)
  getVideos: async () => {
    try {
      const res = await fetch(`${API_BASE}/api/admin/videos`, { headers: getHeaders() });
      return await handleResponse(res);
    } catch (e) {
      console.warn("getVideos failed, probably not implemented yet.", e);
      return [];
    }
  },

  createVideo: async (video: any) => {
    const res = await fetch(`${API_BASE}/api/admin/videos`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(video)
    });
    return handleResponse(res);
  },

  updateVideo: async (id: string, video: any) => {
    const res = await fetch(`${API_BASE}/api/admin/videos/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(video)
    });
    return handleResponse(res);
  },

  deleteVideo: async (id: string) => {
    const res = await fetch(`${API_BASE}/api/admin/videos/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },
  
  // Dashboard stats
  getDashboardStats: async () => {
    try {
      const res = await fetch(`${API_BASE}/api/admin/dashboard/stats`, { headers: getHeaders() });
      return await handleResponse(res);
    } catch (e) {
      console.warn("getDashboardStats failed, probably not implemented yet.", e);
      return { users: 0, contentCount: 0, dau: 0 };
    }
  }
};
