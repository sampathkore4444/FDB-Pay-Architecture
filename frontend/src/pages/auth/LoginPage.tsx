import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginForm) => {
    setLoading(true);
    setError('');
    try {
      const response = await authApi.login(data);
      login(response.user, response.accessToken, response.refreshToken);
      navigate('/wallet');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">FDB Pay</h1>
          <p className="text-gray-500 mt-2">Sign in to your account</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Phone Number"
              placeholder="+959XXXXXXXX"
              error={errors.phone?.message}
              {...register('phone')}
            />
            <Input
              label="PIN"
              type="password"
              placeholder="Enter your PIN"
              error={errors.pin?.message}
              {...register('pin')}
            />

            {error && <p className="text-sm text-red-600">{error}</p>}

            <Button type="submit" loading={loading} className="w-full">
              Sign In
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              Don't have an account?{' '}
              <a href="/register" className="text-blue-600 hover:text-blue-700 font-medium">
                Register
              </a>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
