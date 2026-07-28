import { NavLink } from 'react-router-dom';
import { cn } from '../../utils';

const navItems = [
  { to: '/wallet', label: 'Wallet' },
  { to: '/transfer', label: 'Transfer' },
  { to: '/bills', label: 'Bills' },
  { to: '/merchant', label: 'Merchant' },
  { to: '/agent', label: 'Agent' },
  { to: '/admin', label: 'Admin' },
];

export function Sidebar() {
  return (
    <aside className="w-64 bg-gray-900 text-white min-h-screen p-4">
      <div className="mb-8">
        <h1 className="text-xl font-bold">FDB Pay</h1>
        <p className="text-xs text-gray-400 mt-1">Web Portal</p>
      </div>

      <nav className="space-y-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(
                'block px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                isActive ? 'bg-blue-600 text-white' : 'text-gray-300 hover:bg-gray-800 hover:text-white'
              )
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
