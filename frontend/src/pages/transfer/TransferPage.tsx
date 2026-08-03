import { useState } from 'react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { transferApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { getApiErrorMessage } from '../../utils';

const transferSchema = z.object({
  recipient: z.string().min(1, 'Recipient is required'),
  amount: z.number().positive('Amount must be positive'),
  description: z.string().optional(),
});

type TransferForm = z.infer<typeof transferSchema>;

export function TransferPage() {
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<TransferForm>({
    resolver: zodResolver(transferSchema),
  });

  const onSubmit = async (data: TransferForm) => {
    if (!user) return;
    setLoading(true);
    try {
      const response = await transferApi.initiate(user.id, {
        recipientIdentifier: data.recipient,
        amount: data.amount,
        description: data.description,
        type: 'P2P',
      });
      toast.success(`Transfer completed! ID: ${response.id}`);
      reset();
      navigate('/wallet');
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Transfer failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Send Money</h1>

      <Card>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            label="Recipient Phone or Wallet ID"
            placeholder="+959XXXXXXXX or wallet ID"
            error={errors.recipient?.message}
            {...register('recipient')}
          />

          <Input
            label="Amount (MMK)"
            type="number"
            placeholder="0"
            error={errors.amount?.message}
            {...register('amount', { valueAsNumber: true })}
          />

          <Input
            label="Description (optional)"
            placeholder="What's this for?"
            {...register('description')}
          />

          <Button type="submit" loading={loading} className="w-full">
            Send Money
          </Button>
        </form>
      </Card>
    </div>
  );
}
