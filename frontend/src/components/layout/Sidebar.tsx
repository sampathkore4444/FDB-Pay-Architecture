import { NavLink } from 'react-router-dom';
import { cn } from '../../utils';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import type { User } from '../../types';

type Role = User['role'];

const icons: Record<string, string> = {
  wallet: '💰',
  transfer: '💸',
  requestMoney: '📩',
  bills: '📄',
  airtime: '📱',
  merchant: '🏪',
  becomeMerchant: '🏪',
  invoices: '🧾',
  inventory: '📦',
  agent: '🤝',
  corporate: '🏢',
  admin: '⚙️',
  adminKyc: '🪪',
  adminAml: '🚨',
  adminUsers: '👤',
  adminMerchants: '🏬',
  disputes: '⚖️',
  audit: '📋',
  settlements: '🏦',
  savings: '🐷',
  directory: '🗺️',
  scheduled: '⏰',
  staff: '👥',
  payroll: '💳',
  remittance: '🌏',
  promotions: '🎉',
  support: '🎧',
};

interface NavItem {
  key: string;
  route: string;
  roles: Role[];
  section?: string;
}

const navItems: NavItem[] = [
  { key: 'wallet', route: '/wallet', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'] },
  { key: 'transfer', route: '/transfer', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'] },
  { key: 'requestMoney', route: '/request-money', roles: ['CONSUMER'] },
  { key: 'bills', route: '/bills', roles: ['CONSUMER'] },
  { key: 'airtime', route: '/airtime', roles: ['CONSUMER'] },
  { key: 'savings', route: '/savings', roles: ['CONSUMER'] },
  { key: 'scheduled', route: '/scheduled', roles: ['CONSUMER'] },
  { key: 'remittance', route: '/remittance', roles: ['CONSUMER'] },
  { key: 'promotions', route: '/promotions', roles: ['CONSUMER', 'MERCHANT'] },
  { key: 'directory', route: '/directory', roles: ['CONSUMER'] },

  { key: 'merchant', route: '/merchant', roles: ['MERCHANT'] },
  { key: 'becomeMerchant', route: '/merchant', roles: ['CONSUMER'] },
  { key: 'invoices', route: '/invoices', roles: ['MERCHANT'] },
  { key: 'inventory', route: '/inventory', roles: ['MERCHANT'] },
  { key: 'staff', route: '/staff', roles: ['MERCHANT'] },
  { key: 'settlements', route: '/settlements', roles: ['MERCHANT'] },

  { key: 'agent', route: '/agent', roles: ['AGENT'] },

  { key: 'corporate', route: '/corporate', roles: ['CORPORATE'] },
  { key: 'payroll', route: '/payroll', roles: ['CORPORATE'] },

  { key: 'support', route: '/support', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'] },
  { key: 'disputes', route: '/disputes', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'] },

  { key: 'admin', route: '/admin', roles: ['ADMIN'] },
  { key: 'adminKyc', route: '/admin/kyc', roles: ['ADMIN'] },
  { key: 'adminAml', route: '/admin/aml', roles: ['ADMIN'] },
  { key: 'adminUsers', route: '/admin/users', roles: ['ADMIN'] },
  { key: 'adminMerchants', route: '/admin/merchants', roles: ['ADMIN'] },
  { key: 'audit', route: '/audit', roles: ['ADMIN'] },
];

const roleLabels: Record<Role, string> = {
  CONSUMER: 'Consumer',
  MERCHANT: 'Merchant',
  AGENT: 'Agent',
  CORPORATE: 'Corporate',
  ADMIN: 'Admin',
};

export function Sidebar() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const role = user?.role ?? 'CONSUMER';

  const visibleItems = navItems.filter((item) => item.roles.includes(role));

  return (
    <aside className="w-64 bg-gray-900 text-white min-h-screen p-4 overflow-y-auto">
      <div className="mb-8">
        <h1 className="text-xl font-bold">{t.common.appName}</h1>
        <p className="text-xs text-gray-400 mt-1">{roleLabels[role]} Portal</p>
      </div>

      <nav className="space-y-1">
        {visibleItems.map((item) => (
          <NavLink
            key={item.key}
            to={item.route}
            className={({ isActive }) =>
              cn(
                'flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                isActive ? 'bg-blue-600 text-white' : 'text-gray-300 hover:bg-gray-800 hover:text-white'
              )
            }
          >
            <span className="text-base">{icons[item.key]}</span>
            <span>{t.nav[item.key as keyof typeof t.nav]}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
