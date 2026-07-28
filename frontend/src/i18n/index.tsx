import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import en from './locales/en';
import my from './locales/my';
import type { Translations } from './locales/en';

type Locale = 'en' | 'my';

const locales: Record<Locale, Translations> = { en, my };

interface I18nContextValue {
  locale: Locale;
  t: Translations;
  setLocale: (locale: Locale) => void;
}

const I18nContext = createContext<I18nContextValue | null>(null);

function getInitialLocale(): Locale {
  try {
    const stored = localStorage.getItem('fdb-pay-locale');
    if (stored === 'en' || stored === 'my') return stored;
  } catch {}
  return 'en';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(getInitialLocale);

  const setLocale = useCallback((l: Locale) => {
    setLocaleState(l);
    try {
      localStorage.setItem('fdb-pay-locale', l);
    } catch {}
  }, []);

  return (
    <I18nContext.Provider value={{ locale, t: locales[locale], setLocale }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useTranslation() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useTranslation must be used within I18nProvider');
  return ctx;
}
