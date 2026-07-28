import { useState } from 'react';
import { useTranslation } from '../../i18n';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { cn } from '../../utils';

interface ApiKey {
  id: string;
  name: string;
  key: string;
  createdAt: string;
  lastUsed?: string;
  status: 'ACTIVE' | 'REVOKED';
}

interface SyncLog {
  id: string;
  type: string;
  status: 'SUCCESS' | 'FAILED';
  itemsSynced: number;
  timestamp: string;
}

export function InventoryPage() {
  const { t } = useTranslation();
  const [posConnected, setPosConnected] = useState(false);
  const [posSystem, setPosSystem] = useState('');
  const [webhookUrl, setWebhookUrl] = useState('');
  const [showAddKey, setShowAddKey] = useState(false);
  const [newKeyName, setNewKeyName] = useState('');
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [syncLogs, setSyncLogs] = useState<SyncLog[]>([]);
  const [syncing, setSyncing] = useState(false);
  const [connecting, setConnecting] = useState(false);

  const handleConnect = async () => {
    if (!posSystem) return;
    setConnecting(true);
    await new Promise((r) => setTimeout(r, 1000));
    setPosConnected(true);
    setConnecting(false);
  };

  const handleDisconnect = () => {
    setPosConnected(false);
    setPosSystem('');
  };

  const handleSync = async () => {
    setSyncing(true);
    await new Promise((r) => setTimeout(r, 2000));
    const newLog: SyncLog = {
      id: `sync_${Date.now()}`,
      type: 'PRODUCT_SYNC',
      status: 'SUCCESS',
      itemsSynced: Math.floor(Math.random() * 50) + 10,
      timestamp: new Date().toISOString(),
    };
    setSyncLogs([newLog, ...syncLogs]);
    setSyncing(false);
  };

  const handleAddKey = () => {
    if (!newKeyName) return;
    const newKey: ApiKey = {
      id: `key_${Date.now()}`,
      name: newKeyName,
      key: `fp_live_${Math.random().toString(36).substring(2, 30)}`,
      createdAt: new Date().toISOString(),
      status: 'ACTIVE',
    };
    setApiKeys([...apiKeys, newKey]);
    setNewKeyName('');
    setShowAddKey(false);
  };

  const handleRevokeKey = (keyId: string) => {
    setApiKeys(apiKeys.map((k) => k.id === keyId ? { ...k, status: 'REVOKED' as const } : k));
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.merchant.pos}</h1>

      <Card title="POS Integration">
        <div className="space-y-4">
          <p className="text-sm text-gray-500">Connect your POS system to sync inventory and process payments.</p>
          {posConnected ? (
            <div className="flex items-center justify-between bg-green-50 border border-green-200 rounded-lg p-4">
              <div>
                <p className="text-sm font-medium text-green-800">Connected to {posSystem}</p>
                <p className="text-xs text-green-600">Last sync: Just now</p>
              </div>
              <div className="flex space-x-2">
                <Button size="sm" onClick={handleSync} loading={syncing}>Sync Now</Button>
                <Button size="sm" variant="ghost" onClick={handleDisconnect}>Disconnect</Button>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              <Input
                label="POS System"
                placeholder="e.g., Square, Clover, Toast"
                value={posSystem}
                onChange={(e) => setPosSystem(e.target.value)}
              />
              <Button onClick={handleConnect} loading={connecting} disabled={!posSystem}>
                Connect
              </Button>
            </div>
          )}
        </div>
      </Card>

      <Card title="Webhook Configuration">
        <div className="space-y-4">
          <p className="text-sm text-gray-500">Configure webhook URL to receive real-time inventory update notifications.</p>
          <Input
            label="Webhook URL"
            placeholder="https://your-server.com/webhook"
            value={webhookUrl}
            onChange={(e) => setWebhookUrl(e.target.value)}
          />
          <Button variant="secondary">Save Webhook</Button>
        </div>
      </Card>

      <Card title="API Key Management">
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-500">Manage API keys for external integrations.</p>
            <Button size="sm" onClick={() => setShowAddKey(true)}>Add API Key</Button>
          </div>
          {apiKeys.length === 0 ? (
            <p className="text-sm text-gray-400">No API keys configured.</p>
          ) : (
            <div className="space-y-2">
              {apiKeys.map((key) => (
                <div key={key.id} className="flex items-center justify-between bg-gray-50 rounded-lg p-3">
                  <div>
                    <p className="text-sm font-medium">{key.name}</p>
                    <p className="text-xs font-mono text-gray-500">{key.key.substring(0, 12)}...{key.key.substring(key.key.length - 4)}</p>
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className={cn('text-xs px-2 py-0.5 rounded-full',
                      key.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    )}>
                      {key.status}
                    </span>
                    {key.status === 'ACTIVE' && (
                      <Button size="sm" variant="ghost" onClick={() => handleRevokeKey(key.id)}>
                        Revoke
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>

      <Card title="Sync Logs">
        {syncLogs.length === 0 ? (
          <p className="text-sm text-gray-400">No sync logs yet.</p>
        ) : (
          <div className="space-y-2">
            {syncLogs.map((log) => (
              <div key={log.id} className="flex items-center justify-between bg-gray-50 rounded-lg p-3">
                <div>
                  <p className="text-sm font-medium">{log.type}</p>
                  <p className="text-xs text-gray-500">{log.itemsSynced} items synced</p>
                </div>
                <div className="text-right">
                  <span className={cn('text-xs px-2 py-0.5 rounded-full',
                    log.status === 'SUCCESS' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  )}>
                    {log.status}
                  </span>
                  <p className="text-xs text-gray-400 mt-1">{new Date(log.timestamp).toLocaleTimeString()}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Modal open={showAddKey} onClose={() => setShowAddKey(false)} title="Add API Key">
        <div className="space-y-4">
          <Input
            label="Key Name"
            placeholder="e.g., Production API Key"
            value={newKeyName}
            onChange={(e) => setNewKeyName(e.target.value)}
          />
          <div className="flex space-x-3">
            <Button onClick={handleAddKey} className="flex-1">Create Key</Button>
            <Button variant="secondary" onClick={() => setShowAddKey(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
