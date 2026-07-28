import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import type { User } from '../../types';

type Role = User['role'];
type RoleArray = readonly Role[];

const defaultRoutes: Record<Role, string> = {
  CONSUMER: '/wallet',
  MERCHANT: '/merchant',
  AGENT: '/agent',
  CORPORATE: '/corporate',
  ADMIN: '/admin',
};

interface RoleGuardProps {
  allowedRoles: RoleArray;
  children: React.ReactNode;
}

export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to={defaultRoutes[user.role]} replace />;
  }

  return <>{children}</>;
}
