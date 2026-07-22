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

export type AdminVideoPayload = {
  id?: string; title: string; titleVi?: string; youtubeUrl?: string; youtubeVideoId?: string;
  thumbnailUrl?: string; domain: string; difficulty: string; durationSeconds?: number;
  koreanSubtitles?: { start: number; end: number; text: string; tokens?: unknown[] }[];
  vietnameseSubtitles?: { start: number; end: number; text: string; tokens?: unknown[] }[];
  quizMarkers?: unknown[];
};

export type AdminScenarioPayload = {
  id?: string; title: string; titleVi?: string; domain: string; difficulty: string; missionVi?: string;
  persona?: { name?: string; role?: string; company?: string; avatar?: string };
  objectives?: string[]; requiredVocabulary?: string[]; active: boolean;
};

export type AdminDictionaryPayload = {
  id?: string; korean: string; pronunciation?: string; vietnamese: string; english?: string;
  domain?: string; hanja?: string; frequency?: string; searchCount?: number;
};

export type AdminPronunciationPayload = {
  id?: string; title: string; titleVi?: string; domain: string; difficulty: string; order: number;
  sentences: { text: string; translationVi?: string; audioUrl?: string; waveformData?: number[] }[];
};

export type AdminPromptPayload = { id?: string; name: string; description?: string; content: string };

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
  getVideos: async (): Promise<AdminVideoPayload[]> => handleResponse(await fetch(`${API_BASE}/api/admin/videos`, { headers: getHeaders() })),

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

  getScenarios: async (): Promise<AdminScenarioPayload[]> => handleResponse(await fetch(`${API_BASE}/api/admin/scenarios`, { headers: getHeaders() })),
  createScenario: async (data: AdminScenarioPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/scenarios`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),
  updateScenario: async (id: string, data: AdminScenarioPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/scenarios/${id}`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify(data) })),
  deleteScenario: async (id: string) => handleResponse(await fetch(`${API_BASE}/api/admin/scenarios/${id}`, { method: 'DELETE', headers: getHeaders() })),

  getDictionary: async (query = ''): Promise<AdminDictionaryPayload[]> => handleResponse(await fetch(`${API_BASE}/api/admin/dictionary${query ? `?query=${encodeURIComponent(query)}` : ''}`, { headers: getHeaders() })),
  createDictionary: async (data: AdminDictionaryPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/dictionary`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),
  updateDictionary: async (id: string, data: AdminDictionaryPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/dictionary/${id}`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify(data) })),
  deleteDictionary: async (id: string) => handleResponse(await fetch(`${API_BASE}/api/admin/dictionary/${id}`, { method: 'DELETE', headers: getHeaders() })),
  importDictionary: async (data: AdminDictionaryPayload[]) => handleResponse(await fetch(`${API_BASE}/api/admin/dictionary/import`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),

  getPronunciationExercises: async (): Promise<AdminPronunciationPayload[]> => handleResponse(await fetch(`${API_BASE}/api/admin/pronunciation-exercises`, { headers: getHeaders() })),
  createPronunciationExercise: async (data: AdminPronunciationPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/pronunciation-exercises`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),
  updatePronunciationExercise: async (id: string, data: AdminPronunciationPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/pronunciation-exercises/${id}`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify(data) })),
  deletePronunciationExercise: async (id: string) => handleResponse(await fetch(`${API_BASE}/api/admin/pronunciation-exercises/${id}`, { method: 'DELETE', headers: getHeaders() })),

  getPrompts: async (): Promise<AdminPromptPayload[]> => handleResponse(await fetch(`${API_BASE}/api/admin/prompts`, { headers: getHeaders() })),
  createPrompt: async (data: AdminPromptPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/prompts`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),
  updatePrompt: async (id: string, data: AdminPromptPayload) => handleResponse(await fetch(`${API_BASE}/api/admin/prompts/${id}`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify(data) })),
  getAdmins: async () => handleResponse(await fetch(`${API_BASE}/api/admin/admins`, { headers: getHeaders() })),
  grantAdmin: async (email: string) => handleResponse(await fetch(`${API_BASE}/api/admin/admins`, { method: 'POST', headers: getHeaders(), body: JSON.stringify({ email }) })),
  revokeAdmin: async (id: string) => handleResponse(await fetch(`${API_BASE}/api/admin/admins/${id}`, { method: 'DELETE', headers: getHeaders() })),
  
  // Dashboard stats
  getDashboardStats: async () => handleResponse(await fetch(`${API_BASE}/api/admin/dashboard/stats`, { headers: getHeaders() }))
};
