const API_BASE = import.meta.env.VITE_API_URL || 'http://192.168.0.73:8080';

export type AdminTopicPayload = {
  id?: string;
  domain: string;
  title: string;
  titleVi: string;
  description?: string;
  order: number;
  prerequisiteTopicIds: string[];
  tags: string[];
  isActive: boolean;
};

export type LessonVocabularyPayload = {
  id?: string;
  korean?: string;
  pronunciation?: string;
  vietnamese?: string;
  english?: string;
  context?: string;
  codeSnippet?: string;
  audioUrl?: string;
};

export type LessonExercisePayload = {
  id?: string;
  type: 'multiple_choice' | 'fill_blank';
  question?: string;
  questionVi?: string;
  options?: string[];
  correctAnswer?: string;
};

export type AdminLessonPayload = {
  id?: string;
  topicId: string;
  title: string;
  titleVi: string;
  content?: string;
  contentVi?: string;
  order: number;
  vocabulary: LessonVocabularyPayload[];
  exercises: LessonExercisePayload[];
  topicTitle?: string;
  topicTitleVi?: string;
  domain?: string;
};

const getHeaders = () => {
  const token = localStorage.getItem('kapor_admin_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
};

// Helper to unwrap standard ApiResponse { success, data, message }
const handleResponse = async (res: Response) => {
  if (res.status === 401 || res.status === 403) {
    localStorage.removeItem('kapor_admin_token');
    localStorage.removeItem('kapor_admin_refresh_token');
    window.location.reload();
    throw new Error('Unauthorized access, please login again.');
  }

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

  createUser: async (data: { email: string; password: string; name: string; role?: string }) => {
    const res = await fetch(`${API_BASE}/api/admin/users`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  // Topics API
  getAdminTopics: async (domain?: string): Promise<AdminTopicPayload[]> => {
    const query = domain ? `?domain=${encodeURIComponent(domain)}` : '';
    const res = await fetch(`${API_BASE}/api/admin/topics${query}`, { headers: getHeaders() });
    return handleResponse(res);
  },

  createAdminTopic: async (data: AdminTopicPayload): Promise<AdminTopicPayload> => {
    const res = await fetch(`${API_BASE}/api/admin/topics`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  updateAdminTopic: async (id: string, data: AdminTopicPayload): Promise<AdminTopicPayload> => {
    const res = await fetch(`${API_BASE}/api/admin/topics/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  deleteAdminTopic: async (id: string): Promise<void> => {
    const res = await fetch(`${API_BASE}/api/admin/topics/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  // Lessons API
  getAdminLessons: async (topicId?: string): Promise<AdminLessonPayload[]> => {
    const query = topicId ? `?topicId=${encodeURIComponent(topicId)}` : '';
    const res = await fetch(`${API_BASE}/api/admin/lessons${query}`, { headers: getHeaders() });
    return handleResponse(res);
  },

  createAdminLesson: async (data: AdminLessonPayload): Promise<AdminLessonPayload> => {
    const res = await fetch(`${API_BASE}/api/admin/lessons`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  updateAdminLesson: async (id: string, data: AdminLessonPayload): Promise<AdminLessonPayload> => {
    const res = await fetch(`${API_BASE}/api/admin/lessons/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  deleteAdminLesson: async (id: string): Promise<void> => {
    const res = await fetch(`${API_BASE}/api/admin/lessons/${id}`, {
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
