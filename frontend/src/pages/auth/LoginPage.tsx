import { useState, useMemo } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';

const loginSchema = z.object({
  phone: z.string().min(10, 'Phone number must be at least 10 digits'),
  pin: z.string().min(4, 'PIN must be at least 4 digits').max(6),
});

const registerSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  phone: z.string().min(10, 'Phone number must be at least 10 digits'),
  pin: z.string().min(4, 'PIN must be at least 4 digits').max(6),
});

type LoginForm = z.infer<typeof loginSchema>;
type RegisterForm = z.infer<typeof registerSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((s) => s.login);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const isRegister = useMemo(() => location.pathname === '/register', [location.pathname]);

  const {
    register: registerField,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm | RegisterForm>({
    resolver: zodResolver(isRegister ? registerSchema : loginSchema),
  });

  const onSubmit = async (data: LoginForm | RegisterForm) => {
    setLoading(true);
    setError('');
    try {
      if (isRegister) {
        const d = data as RegisterForm;
        const response = await authApi.register({
          phone: d.phone,
          name: d.name,
          email: d.email || undefined,
          pin: d.pin,
        });
        login(response.user, response.accessToken, response.refreshToken);
      } else {
        const d = data as LoginForm;
        const response = await authApi.login(d);
        login(response.user, response.accessToken, response.refreshToken);
      }
      navigate('/wallet');
    } catch (err) {
      setError(err instanceof Error ? err.message : isRegister ? 'Registration failed' : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">FDB Pay</h1>
          <p className="text-gray-500 mt-2">
            {isRegister ? 'Create your account' : 'Sign in to your account'}
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {isRegister && (
              <>
                <Input
                  label="Full Name"
                  placeholder="John Doe"
                  error={'name' in errors ? (errors as Record<string, { message?: string }>).name?.message : undefined}
                  {...registerField('name')}
                />
                <Input
                  label="Email (optional)"
                  type="email"
                  placeholder="john@example.com"
                  error={'email' in errors ? (errors as Record<string, { message?: string }>).email?.message : undefined}
                  {...registerField('email')}
                />
              </>
            )}
            <Input
              label="Phone Number"
              placeholder="+959XXXXXXXX"
              error={errors.phone?.message}
              {...registerField('phone')}
            />
            <Input
              label="PIN"
              type="password"
              placeholder={isRegister ? 'Create a PIN (4-6 digits)' : 'Enter your PIN'}
              error={errors.pin?.message}
              {...registerField('pin')}
            />

            {error && <p className="text-sm text-red-600">{error}</p>}

            <Button type="submit" loading={loading} className="w-full">
              {isRegister ? 'Create Account' : 'Sign In'}
            </Button>
          </form>

          <div className="mt-6 text-center">
            {isRegister ? (
              <p className="text-sm text-gray-500">
                Already have an account?{' '}
                <Link to="/login" className="text-blue-600 hover:text-blue-700 font-medium">
                  Sign In
                </Link>
              </p>
            ) : (
              <p className="text-sm text-gray-500">
                Don't have an account?{' '}
                <Link to="/register" className="text-blue-600 hover:text-blue-700 font-medium">
                  Register
                </Link>
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
