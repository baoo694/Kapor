export const API_BASE = (import.meta.env.VITE_API_URL || 'https://api.domday.food')
  .replace(/\/+$/, '');

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

export type SubtitleLinePayload = {
  start: number;
  end: number;
  text: string;
  tokens?: unknown[];
};

export type SubtitleUpdatePayload = {
  koreanSubtitles: SubtitleLinePayload[];
  vietnameseSubtitles: SubtitleLinePayload[];
};

export type SubtitleTokenizePayload = {
  koreanSubtitles: SubtitleLinePayload[];
};

export type AdminVideoPayload = {
  id?: string; title: string; titleVi?: string; youtubeUrl?: string; youtubeVideoId?: string;
  thumbnailUrl?: string; domain: string; difficulty: string; durationSeconds?: number;
  koreanSubtitles?: SubtitleLinePayload[];
  vietnameseSubtitles?: SubtitleLinePayload[];
  quizMarkers?: unknown[];
};

export type AdminScenarioPayload = {
  id?: string; title: string; titleVi: string; domain: string; difficulty: string; order: number; missionVi?: string;
  persona: {
    name?: string; role?: string; company?: string; avatar?: string; avatarUrl?: string;
    speechStyle?: string; personality?: string;
  };
  mission: {
    titleKo?: string; titleVi?: string; contextPrompt?: string;
    objectives: { ko?: string; vi?: string; en?: string }[];
    requiredVocabulary: string[];
  };
  objectives: string[];
  requiredVocabulary: string[];
  evaluationCriteria: {
    grammarWeight: number; vocabularyWeight: number; politenessWeight: number; taskCompletionWeight: number;
  };
  promptTemplateId?: string;
  promptOverride?: string;
  active: boolean;
};

export type AdminPronunciationPayload = {
  id?: string; title: string; titleVi?: string; domain: string; difficulty: string; order: number;
  sentences: { text: string; translationVi?: string; audioUrl?: string; waveformData?: number[] }[];
};

export type AdminPromptPayload = {
  id?: string; key?: string; name: string; description?: string; content: string;
  promptVersion?: number; status?: 'draft' | 'published' | 'archived'; updatedBy?: string;
  requiredPlaceholders?: string[]; documentVersion?: number; createdAt?: string; updatedAt?: string;
};

export type RoleplayTestMessage = {
  id: string; role: 'ai' | 'user'; content: string; generationStatus?: string;
};

export type RoleplayTestSession = {
  id: string; scenarioId: string; status: string; testMode: boolean; messages: RoleplayTestMessage[];
};

export type RoleplayStreamEvent = {
  type: string; sessionId?: string; turnId?: string; userMessageId?: string; messageId?: string;
  delta?: string; message?: RoleplayTestMessage; evaluation?: unknown; code?: string;
  messageText?: string; retryable?: boolean;
};

const getHeaders = () => {
  const token = localStorage.getItem('kapor_admin_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
};

const clearAuthTokens = () => {
  localStorage.removeItem('kapor_admin_token');
  localStorage.removeItem('kapor_admin_refresh_token');
};

let refreshPromise: Promise<boolean> | null = null;

const refreshAccessToken = async (): Promise<boolean> => {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    const refreshToken = localStorage.getItem('kapor_admin_refresh_token');
    if (!refreshToken) return false;

    try {
      const response = await fetch(`${API_BASE}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      });
      const payload = await response.json();
      const tokens = payload?.data;

      if (!response.ok || !payload?.success || !tokens?.accessToken || !tokens?.refreshToken) {
        return false;
      }

      localStorage.setItem('kapor_admin_token', tokens.accessToken);
      localStorage.setItem('kapor_admin_refresh_token', tokens.refreshToken);
      return true;
    } catch {
      return false;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
};

const fetchWithAuth = async (url: string, init: RequestInit = {}): Promise<Response> => {
  const makeRequest = () => {
    const headers = new Headers(init.headers);
    const token = localStorage.getItem('kapor_admin_token');
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    return fetch(url, { ...init, headers });
  };

  let response = await makeRequest();
  if (response.status === 401 && await refreshAccessToken()) {
    response = await makeRequest();
  }

  if (response.status === 401 || response.status === 403) {
    clearAuthTokens();
    window.location.reload();
  }

  return response;
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
      const res = await fetchWithAuth(`${API_BASE}/api/admin/users?page=${page}&search=${search}`, { headers: getHeaders() });
      return await handleResponse(res);
    } catch (e) {
      console.warn("getUsers failed, probably not implemented yet.", e);
      return [];
    }
  },
  
  updateUserRole: async (id: string, role: string) => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/users/${id}/role`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify({ role })
    });
    return handleResponse(res);
  },
  
  deleteUser: async (id: string) => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/users/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  createUser: async (data: { email: string; password: string; name: string; role?: string }) => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/users`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  // Topics API
  getAdminTopics: async (domain?: string): Promise<AdminTopicPayload[]> => {
    const query = domain ? `?domain=${encodeURIComponent(domain)}` : '';
    const res = await fetchWithAuth(`${API_BASE}/api/admin/topics${query}`, { headers: getHeaders() });
    return handleResponse(res);
  },

  createAdminTopic: async (data: AdminTopicPayload): Promise<AdminTopicPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/topics`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  updateAdminTopic: async (id: string, data: AdminTopicPayload): Promise<AdminTopicPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/topics/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  deleteAdminTopic: async (id: string): Promise<void> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/topics/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  // Lessons API
  getAdminLessons: async (topicId?: string): Promise<AdminLessonPayload[]> => {
    const query = topicId ? `?topicId=${encodeURIComponent(topicId)}` : '';
    const res = await fetchWithAuth(`${API_BASE}/api/admin/lessons${query}`, { headers: getHeaders() });
    return handleResponse(res);
  },

  createAdminLesson: async (data: AdminLessonPayload): Promise<AdminLessonPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/lessons`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  updateAdminLesson: async (id: string, data: AdminLessonPayload): Promise<AdminLessonPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/lessons/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  },

  deleteAdminLesson: async (id: string): Promise<void> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/lessons/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  // Videos (Mapping to Documents/Resources for this admin dashboard)
  getVideos: async (): Promise<AdminVideoPayload[]> => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/videos`, { headers: getHeaders() })),

  createVideo: async (video: any) => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(video)
    });
    return handleResponse(res);
  },

  updateVideo: async (id: string, video: any) => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(video)
    });
    return handleResponse(res);
  },

  updateVideoSubtitles: async (id: string, subtitles: SubtitleUpdatePayload): Promise<AdminVideoPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos/${id}/subtitles`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(subtitles)
    });
    return handleResponse(res);
  },

  tokenizeVideoSubtitles: async (id: string, subtitles: SubtitleTokenizePayload): Promise<AdminVideoPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos/${id}/subtitles/tokenize`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(subtitles)
    });
    return handleResponse(res);
  },

  tokenizeVideoSubtitlesWithAi: async (id: string, subtitles: SubtitleTokenizePayload): Promise<AdminVideoPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos/${id}/subtitles/ai-tokenize`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(subtitles)
    });
    return handleResponse(res);
  },

  translateVideoSubtitlesWithAi: async (id: string, subtitles: SubtitleTokenizePayload): Promise<AdminVideoPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos/${id}/subtitles/translate`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(subtitles)
    });
    return handleResponse(res);
  },

  analyzeVideoSubtitlesWithAi: async (id: string, subtitles: SubtitleTokenizePayload): Promise<AdminVideoPayload> => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos/${id}/subtitles/ai-analyze`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(subtitles)
    });
    return handleResponse(res);
  },

  deleteVideo: async (id: string) => {
    const res = await fetchWithAuth(`${API_BASE}/api/admin/videos/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse(res);
  },

  getScenarios: async (): Promise<AdminScenarioPayload[]> => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/scenarios`, { headers: getHeaders() })),
  createScenario: async (data: AdminScenarioPayload) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/scenarios`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),
  updateScenario: async (id: string, data: AdminScenarioPayload) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/scenarios/${id}`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify(data) })),
  deleteScenario: async (id: string) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/scenarios/${id}`, { method: 'DELETE', headers: getHeaders() })),

  getPronunciationExercises: async (): Promise<AdminPronunciationPayload[]> => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/pronunciation-exercises`, { headers: getHeaders() })),
  createPronunciationExercise: async (data: AdminPronunciationPayload) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/pronunciation-exercises`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),
  updatePronunciationExercise: async (id: string, data: AdminPronunciationPayload) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/pronunciation-exercises/${id}`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify(data) })),
  deletePronunciationExercise: async (id: string) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/pronunciation-exercises/${id}`, { method: 'DELETE', headers: getHeaders() })),

  getPrompts: async (): Promise<AdminPromptPayload[]> => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/prompts`, { headers: getHeaders() })),
  createPrompt: async (data: AdminPromptPayload) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/prompts`, { method: 'POST', headers: getHeaders(), body: JSON.stringify(data) })),
  updatePrompt: async (id: string, data: AdminPromptPayload) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/prompts/${id}`, { method: 'PUT', headers: getHeaders(), body: JSON.stringify(data) })),
  clonePrompt: async (id: string): Promise<AdminPromptPayload> => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/prompts/${id}/clone`, { method: 'POST', headers: getHeaders() })),
  publishPrompt: async (id: string): Promise<AdminPromptPayload> => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/prompts/${id}/publish`, { method: 'POST', headers: getHeaders() })),
  deletePrompt: async (id: string) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/prompts/${id}`, { method: 'DELETE', headers: getHeaders() })),

  startRoleplayTest: async (scenarioId: string): Promise<RoleplayTestSession> => handleResponse(await fetchWithAuth(`${API_BASE}/api/roleplay/start`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ scenarioId, testMode: true })
  })),
  streamRoleplayTurn: async (
    sessionId: string,
    payload: { clientTurnId: string; content: string },
    onEvent: (event: RoleplayStreamEvent) => void
  ): Promise<void> => {
    const response = await fetchWithAuth(`${API_BASE}/api/roleplay/${sessionId}/turns/stream`, {
      method: 'POST',
      headers: { ...getHeaders(), Accept: 'text/event-stream' },
      body: JSON.stringify({ ...payload, source: 'text' })
    });
    if (!response.ok || !response.body) {
      await handleResponse(response);
      throw new Error('Streaming response is unavailable');
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (true) {
      const { done, value } = await reader.read();
      buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n');
      const blocks = buffer.split('\n\n');
      buffer = blocks.pop() ?? '';
      for (const block of blocks) {
        const data = block
          .split('\n')
          .filter(line => line.startsWith('data:'))
          .map(line => line.slice(5).trimStart())
          .join('\n');
        if (data) onEvent(JSON.parse(data) as RoleplayStreamEvent);
      }
      if (done) break;
    }
    const tail = buffer
      .split('\n')
      .filter(line => line.startsWith('data:'))
      .map(line => line.slice(5).trimStart())
      .join('\n');
    if (tail) onEvent(JSON.parse(tail) as RoleplayStreamEvent);
  },
  endRoleplayTest: async (sessionId: string): Promise<RoleplayTestSession> => handleResponse(await fetchWithAuth(`${API_BASE}/api/roleplay/${sessionId}/end`, { method: 'POST', headers: getHeaders() })),
  abandonRoleplayTest: async (sessionId: string): Promise<RoleplayTestSession> => handleResponse(await fetchWithAuth(`${API_BASE}/api/roleplay/${sessionId}/abandon`, { method: 'POST', headers: getHeaders() })),
  getAdmins: async () => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/admins`, { headers: getHeaders() })),
  grantAdmin: async (email: string) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/admins`, { method: 'POST', headers: getHeaders(), body: JSON.stringify({ email }) })),
  revokeAdmin: async (id: string) => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/admins/${id}`, { method: 'DELETE', headers: getHeaders() })),
  
  // Dashboard stats
  getDashboardStats: async () => handleResponse(await fetchWithAuth(`${API_BASE}/api/admin/dashboard/stats`, { headers: getHeaders() }))
};
