import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, setUnauthorizedHandler, tokenStore } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    if (!tokenStore.get()) {
      setReady(true);
      return;
    }
    api
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setReady(true));
  }, [logout]);

  const handleAuth = useCallback((auth) => {
    tokenStore.set(auth.token);
    setUser(auth.user);
    return auth.user;
  }, []);

  const value = useMemo(
    () => ({
      user,
      ready,
      isAdmin: !!user?.roles?.includes('ADMIN'),
      login: async (email, password) => handleAuth(await api.login({ email, password })),
      register: async (fullName, email, password) => handleAuth(await api.register({ fullName, email, password })),
      logout,
      setUser,
    }),
    [user, ready, handleAuth, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
