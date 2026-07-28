import { useAuthStore } from '../../store/authStore';
import { useNavigate } from 'react-router-dom';

export function Header() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
      <div>
        <h2 className="text-lg font-semibold text-gray-900">
          Welcome, {user?.name || 'User'}
        </h2>
        <p className="text-sm text-gray-500">{user?.phone}</p>
      </div>
      <div className="flex items-center space-x-4">
        <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded-full">
          {user?.kycTier} KYC
        </span>
        <button
          onClick={handleLogout}
          className="text-sm text-gray-500 hover:text-gray-700"
        >
          Logout
        </button>
      </div>
    </header>
  );
}
