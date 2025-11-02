export type AvailabilityStatus = {
  id: string;
  title: string;
  description: string;
  color: string;
};

export type GroupSummary = {
  id: string;
  name: string;
  memberCount: number;
  primaryColor: string;
  secondaryColor: string;
  status: AvailabilityStatus;
};

export type InvitationSummary = {
  id: string;
  groupName: string;
  inviterName: string;
  sentAt: string;
};

export type CustomStatus = {
  id: string;
  text: string;
  tone: 'relaxed' | 'social' | 'focused' | 'boundary';
  note?: string;
};

export type SockState = {
  globalStatus: AvailabilityStatus;
  managedGroups: GroupSummary[];
  memberGroups: GroupSummary[];
  invitations: InvitationSummary[];
  customStatuses: CustomStatus[];
};

const statuses: Record<string, AvailabilityStatus> = {
  open_to_hangout: {
    id: 'open_to_hangout',
    title: 'Open to Hangout',
    description: "I'm free and would love to connect.",
    color: '#4CAF50'
  },
  busy: {
    id: 'busy',
    title: 'Busy',
    description: 'Unavailable for now; feel free to reach out later.',
    color: '#FFB300'
  },
  working: {
    id: 'working',
    title: 'Working',
    description: "Focused on work; ping if it's important.",
    color: '#E0A800'
  },
  do_not_approach: {
    id: 'do_not_approach',
    title: 'Do Not Approach',
    description: 'Need serious space right now.',
    color: '#D32F2F'
  }
};

export const makePreviewState = (): SockState => ({
  globalStatus: statuses.open_to_hangout,
  managedGroups: [
    {
      id: 'roommates',
      name: 'Roommates',
      memberCount: 4,
      primaryColor: '#6750A4',
      secondaryColor: '#24005A',
      status: statuses.open_to_hangout
    }
  ],
  memberGroups: [
    {
      id: 'book_club',
      name: 'Book Club',
      memberCount: 8,
      primaryColor: '#386A20',
      secondaryColor: '#1B370C',
      status: statuses.working
    }
  ],
  invitations: [
    {
      id: 'invite-1',
      groupName: 'Game Night',
      inviterName: 'Jordan',
      sentAt: new Date().toISOString()
    }
  ],
  customStatuses: [
    { id: 'cs-1', text: 'Take a breather', tone: 'relaxed', note: 'Happy for a quick walk' },
    { id: 'cs-2', text: 'Heads down sprint', tone: 'focused' }
  ]
});

export const cycleStatus = (current: AvailabilityStatus): AvailabilityStatus => {
  const order = [
    statuses.open_to_hangout,
    statuses.busy,
    statuses.working,
    statuses.do_not_approach
  ];
  const index = order.findIndex((status) => status.id === current.id);
  const nextIndex = index === -1 ? 0 : (index + 1) % order.length;
  return order[nextIndex];
};
