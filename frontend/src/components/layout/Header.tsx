import { useAuthStore } from '../../store/authStore';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from '../../i18n';
import { cn } from '../../utils';

export function Header() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const { t, locale, setLocale } = useTranslation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
      <div>
        <h2 className="text-lg font-semibold text-gray-900">
          {t.common.welcome}, {user?.name || 'User'}
        </h2>
        <p className="text-sm text-gray-500">{user?.phone}</p>
      </div>
      <div className="flex items-center space-x-4">
        <div className="flex items-center bg-gray-100 rounded-lg p-0.5">
          <button
            onClick={() => setLocale('en')}
            className={cn(
              'px-2 py-1 text-xs font-medium rounded-md transition-colors',
              locale === 'en' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            )}
          >
            EN
          </button>
          <button
            onClick={() => setLocale('my')}
            className={cn(
              'px-2 py-1 text-xs font-medium rounded-md transition-colors',
              locale === 'my' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            )}
          >
            MY
          </button>
        </div>
        <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded-full">
          {user?.kycTier} KYC
        </span>
        <button
          onClick={handleLogout}
          className="text-sm text-gray-500 hover:text-gray-700"
        >
          {t.common.logout}
        </button>
      </div>
    </header>
  );
}
