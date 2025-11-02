import { clsx } from 'clsx';
import { HomeIcon, GearIcon, PersonIcon, RowsIcon } from '@radix-ui/react-icons';

type Destination = 'dashboard' | 'manage-groups' | 'settings' | 'profile';

const destinations: { id: Destination; label: string; icon: React.ComponentType }[] = [
  { id: 'dashboard', label: 'Dashboard', icon: HomeIcon },
  { id: 'manage-groups', label: 'Groups', icon: RowsIcon },
  { id: 'settings', label: 'Settings', icon: GearIcon },
  { id: 'profile', label: 'Profile', icon: PersonIcon }
];

type Props = {
  expanded: boolean;
  destination: Destination;
  onNavigate: (destination: Destination) => void;
  onToggle: () => void;
};

export function SidebarRail({ expanded, destination, onNavigate, onToggle }: Props) {
  return (
    <aside
      className={clsx(
        'flex h-screen flex-col border-r border-slate-200 bg-white/70 backdrop-blur transition-all duration-200',
        expanded ? 'w-60' : 'w-20'
      )}
    >
      <button
        className="m-4 flex h-10 items-center justify-center rounded-full bg-sock-primary/20 text-sock-primary hover:bg-sock-primary/30"
        onClick={onToggle}
      >
        <span className="sr-only">Toggle navigation rail</span>
        <RowsIcon width={20} height={20} />
      </button>

      <nav className="flex flex-1 flex-col gap-2 px-2">
        {destinations.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            className={clsx(
              'flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-colors',
              destination === id
                ? 'bg-sock-primary text-white'
                : 'text-slate-600 hover:bg-slate-200'
            )}
            onClick={() => onNavigate(id)}
          >
            <Icon width={20} height={20} />
            {expanded && <span>{label}</span>}
          </button>
        ))}
      </nav>
    </aside>
  );
}
