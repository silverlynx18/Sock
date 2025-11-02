import { useMemo, useState } from 'react';
import { makePreviewState, cycleStatus, SockState, GroupSummary } from './state/previewState';
import { SidebarRail } from './components/SidebarRail';
import { Dashboard } from './components/Dashboard';

type Destination = 'dashboard' | 'manage-groups' | 'settings' | 'profile';

export default function App() {
  const [state, setState] = useState<SockState>(() => makePreviewState());
  const [destination, setDestination] = useState<Destination>('dashboard');
  const [railExpanded, setRailExpanded] = useState<boolean>(false);

  const dashboardHandlers = useMemo(
    () => ({
      updateGlobalStatus: () =>
        setState((prev) => ({ ...prev, globalStatus: cycleStatus(prev.globalStatus) })),
      selectGroup: (_group: GroupSummary) => setDestination('manage-groups'),
      manageGroups: () => setDestination('manage-groups'),
      viewInvitations: () => setDestination('manage-groups')
    }),
    []
  );

  return (
    <div className="min-h-screen flex bg-slate-100 text-slate-900">
      <SidebarRail
        expanded={railExpanded}
        destination={destination}
        onToggle={() => setRailExpanded((value) => !value)}
        onNavigate={setDestination}
      />

      <main className="flex-1 px-6 py-8">
        {destination === 'dashboard' && (
          <Dashboard state={state} handlers={dashboardHandlers} />
        )}

        {destination !== 'dashboard' && (
          <div className="mx-auto max-w-3xl rounded-3xl bg-white/70 p-8 shadow-sm backdrop-blur">
            <h1 className="text-3xl font-semibold mb-2 capitalize">{destination.replace('-', ' ')}</h1>
            <p className="text-slate-600">
              Placeholder screen. Mirror the Android/iOS modules by wiring repository-backed data here.
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
