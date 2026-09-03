import axios from 'axios'

const TOKEN_KEY = 'nems.token'
// Kept in sync with AuthContext's own USER_KEY — not imported from there to avoid a circular
// import (AuthContext already imports setToken/clearToken from this file).
const USER_KEY = 'nems.user'
const REDIRECT_KEY = 'nems.postLoginRedirect'
const AUTH_MESSAGE_KEY = 'nems.authMessage'

export const client = axios.create({
  baseURL: '/api/v1',
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Clears the stale session and sends the user to login — stashing where they were and why, so
 * LoginPage can show a friendly explanation and return them to the same place (e.g. a QR scan
 * page) once they're signed in again, instead of just dumping them on their role's home page.
 */
export function redirectToLogin(message?: string) {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  if (window.location.pathname !== '/login') {
    sessionStorage.setItem(REDIRECT_KEY, window.location.pathname + window.location.search)
  }
  if (message) {
    sessionStorage.setItem(AUTH_MESSAGE_KEY, message)
  }
  if (window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}

export function consumePostLoginRedirect(): string | null {
  const path = sessionStorage.getItem(REDIRECT_KEY)
  sessionStorage.removeItem(REDIRECT_KEY)
  return path
}

export function consumeAuthMessage(): string | null {
  const message = sessionStorage.getItem(AUTH_MESSAGE_KEY)
  sessionStorage.removeItem(AUTH_MESSAGE_KEY)
  return message
}

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      redirectToLogin('Your session has expired, or you were not logged in. Please log in and try again.')
    }
    return Promise.reject(error)
  },
)

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function apiErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    return err.response?.data?.message ?? err.message
  }
  return String(err)
}

/**
 * 401 = not logged in / session expired; 403 = logged in but the account lacks permission.
 * Callers that want a friendlier, actionable message than the raw backend text (e.g. ScanPage)
 * branch on this instead of just showing apiErrorMessage().
 */
export function authErrorStatus(err: unknown): 401 | 403 | null {
  if (axios.isAxiosError(err)) {
    const status = err.response?.status
    if (status === 401 || status === 403) return status
  }
  return null
}
