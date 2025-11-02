import { formatRelative } from '../utils/date';
import type { SockState, GroupSummary } from '../state/previewState';

type Handlers = {
  updateGlobalStatus: () => void;
  selectGroup: (group: GroupSummary) => void;
  manageGroups: () => void;
  viewInvitations: () => void;
};

type Props = {
  state: SockState;
  handlers: Handlers;
};

export function Dashboard({ state, handlers }: Props) {
  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <section className="rounded-3xl bg-white/80 p-6 shadow-sm backdrop-blur">
        <div className="flex flex-col gap-3">
          <span className="text-xs uppercase tracking-wide text-slate-500">Global Status</span>
          <h2 className="text-2xl font-semibold" style={{ color: state.globalStatus.color }}>
            {state.globalStatus.title}
          </h2>
          <p className="text-slate-600">{state.globalStatus.description}</p>
          <button
            className="w-fit rounded-full bg-sock-primary px-4 py-2 text-sm font-semibold text-white hover:bg-sock-primary/90"
            onClick={handlers.updateGlobalStatus}
          >
            Update
          </button>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        <div className="rounded-3xl bg-white/80 p-6 shadow-sm backdrop-blur">
          <h3 className="text-lg font-semibold">Groups</h3>
          <p className="text-sm text-slate-600">Create and manage your trusted circles.</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button
              className="rounded-full border border-sock-primary px-4 py-2 text-sm font-medium text-sock-primary hover:bg-sock-primary/10"
              onClick={handlers.manageGroups}
            >
              Manage Groups
            </button>
            <button
              className="rounded-full border border-sock-secondary px-4 py-2 text-sm font-medium text-sock-secondary hover:bg-sock-secondary/10"
              onClick={handlers.viewInvitations}
            >
              View Invitations ({state.invitations.length})
            </button>
          </div>
        </div>

        <div className="rounded-3xl bg-white/80 p-6 shadow-sm backdrop-blur">
          <h3 className="text-lg font-semibold">Custom Statuses</h3>
          <ul className="mt-4 space-y-2 text-sm">
            {state.customStatuses.map((status) => (
              <li key={status.id} className="flex items-start gap-3">
                <span
                  className="mt-1 block h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: toneToColor(status.tone) }}
                />
                <div>
                  <p className="font-medium text-slate-800">{status.text}</p>
                  {status.note && <p className="text-xs text-slate-500">{status.note}</p>}
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="rounded-3xl bg-white/80 p-6 shadow-sm backdrop-blur">
        <h3 className="mb-4 text-lg font-semibold">Your Circles</h3>
        <div className="grid gap-4 md:grid-cols-2">
          {state.managedGroups.map((group) => (
            <GroupCard key={group.id} group={group} onClick={() => handlers.selectGroup(group)} />
          ))}
          {state.memberGroups.map((group) => (
            <GroupCard key={group.id} group={group} onClick={() => handlers.selectGroup(group)} />
          ))}
        </div>
        {state.managedGroups.length + state.memberGroups.length === 0 && (
          <div className="rounded-2xl border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500">
            No groups yet. Create one to start sharing availability.
          </div>
        )}
      </section>

      <section className="rounded-3xl bg-white/80 p-6 shadow-sm backdrop-blur">
        <h3 className="mb-4 text-lg font-semibold">Pending Invitations</h3>
        <ul className="space-y-3">
          {state.invitations.map((invite) => (
            <li
              key={invite.id}
              className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white/60 px-4 py-3 text-sm"
            >
              <div>
                <p className="font-medium text-slate-800">{invite.groupName}</p>
                <p className="text-xs text-slate-500">
                  Invited by {invite.inviterName} ? {formatRelative(new Date(invite.sentAt))}
                </p>
              </div>
              <div className="flex gap-2">
                <button className="rounded-full border border-sock-secondary px-3 py-1 text-xs font-semibold text-sock-secondary">
                  Decline
                </button>
                <button className="rounded-full bg-sock-primary px-3 py-1 text-xs font-semibold text-white">
                  Accept
                </button>
              </div>
            </li>
          ))}
        </ul>
        {state.invitations.length === 0 && (
          <p className="rounded-2xl border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500">
            No pending invitations.
          </p>
        )}
      </section>
    </div>
  );
}

function toneToColor(tone: 'relaxed' | 'social' | 'focused' | 'boundary') {
  switch (tone) {
    case 'relaxed':
      return '#4CAF50';
    case 'social':
      return '#039BE5';
    case 'focused':
      return '#FFB300';
    case 'boundary':
      return '#D32F2F';
  }
}

type GroupCardProps = {
  group: GroupSummary;
  onClick: () => void;
};

function GroupCard({ group, onClick }: GroupCardProps) {
  return (
    <button
      onClick={onClick}
      className="flex w-full items-start gap-4 rounded-2xl border border-transparent bg-white/70 px-5 py-4 text-left shadow-sm transition hover:border-sock-primary/40 hover:shadow-md"
    >
      <span className="mt-1 block h-3 w-3 rounded-full" style={{ backgroundColor: group.status.color }} />
      <div className="flex-1">
        <p className="text-base font-semibold text-slate-900">{group.name}</p>
        <p className="text-xs text-slate-500">{group.memberCount} members</p>
        <p className="mt-2 text-xs font-semibold" style={{ color: group.status.color }}>
          {group.status.title}
        </p>
      </div>
      <span className="text-slate-400">?</span>
    </button>
  );
}
