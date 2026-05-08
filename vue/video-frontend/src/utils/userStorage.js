const SESSION_KEY = 'user_session';
const USER_DATA_PREFIX = 'user_data_';

export function getUserInfo() {
  const sessionId = sessionStorage.getItem(SESSION_KEY);
  if (!sessionId) return null;
  const userData = sessionStorage.getItem(USER_DATA_PREFIX + sessionId);
  return userData ? JSON.parse(userData) : null;
}

export function setUserInfo(userInfo) {
  let sessionId = sessionStorage.getItem(SESSION_KEY);
  if (!sessionId) {
    sessionId = 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    sessionStorage.setItem(SESSION_KEY, sessionId);
  }
  sessionStorage.setItem(USER_DATA_PREFIX + sessionId, JSON.stringify(userInfo));
}

export function clearUserInfo() {
  const sessionId = sessionStorage.getItem(SESSION_KEY);
  if (sessionId) {
    sessionStorage.removeItem(USER_DATA_PREFIX + sessionId);
    sessionStorage.removeItem(SESSION_KEY);
  }
}

export function createNewSession() {
  const sessionId = 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  sessionStorage.setItem(SESSION_KEY, sessionId);
  return sessionId;
}

export default {
  getUserInfo,
  setUserInfo,
  clearUserInfo,
  createNewSession
};
