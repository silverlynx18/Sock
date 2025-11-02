import React, { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import {
  Title,
  Card,
  Button,
  Chip,
  TextInput,
  Snackbar,
  ActivityIndicator,
} from 'react-native-paper';
import { useAuth } from '../context/AuthContext';
import { statusAPI } from '../services/api';

const STATUS_OPTIONS = [
  { value: 'available', label: 'Available', icon: '?', color: '#4caf50' },
  { value: 'busy', label: 'Busy', icon: '??', color: '#f44336' },
  { value: 'doNotDisturb', label: 'Do Not Disturb', icon: '??', color: '#ff9800' },
  { value: 'away', label: 'Away', icon: '??', color: '#9e9e9e' },
];

export default function HomeScreen() {
  const { user, refreshUser } = useAuth();
  const [status, setStatus] = useState(user?.status || 'available');
  const [statusMessage, setStatusMessage] = useState(user?.statusMessage || '');
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [snackbarVisible, setSnackbarVisible] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [roommateStatuses, setRoommateStatuses] = useState([]);

  useEffect(() => {
    loadRoommateStatuses();
  }, []);

  const loadRoommateStatuses = async () => {
    try {
      const response = await statusAPI.getRoommateStatuses();
      setRoommateStatuses(response.data);
    } catch (error) {
      console.error('Error loading roommate statuses:', error);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await refreshUser();
    await loadRoommateStatuses();
    setRefreshing(false);
  };

  const handleUpdateStatus = async () => {
    setLoading(true);
    try {
      await statusAPI.updateStatus({ status, statusMessage });
      await refreshUser();
      await loadRoommateStatuses();
      setSnackbarMessage('Status updated successfully!');
      setSnackbarVisible(true);
    } catch (error) {
      setSnackbarMessage('Failed to update status');
      setSnackbarVisible(true);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (statusValue) => {
    const option = STATUS_OPTIONS.find(opt => opt.value === statusValue);
    return option?.color || '#9e9e9e';
  };

  const getStatusLabel = (statusValue) => {
    const option = STATUS_OPTIONS.find(opt => opt.value === statusValue);
    return option ? `${option.icon} ${option.label}` : statusValue;
  };

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      <View style={styles.content}>
        <Card style={styles.card}>
          <Card.Content>
            <Title>Your Status</Title>

            <View style={styles.statusChips}>
              {STATUS_OPTIONS.map((option) => (
                <Chip
                  key={option.value}
                  selected={status === option.value}
                  onPress={() => setStatus(option.value)}
                  style={[
                    styles.chip,
                    status === option.value && { backgroundColor: option.color },
                  ]}
                  textStyle={status === option.value && { color: 'white' }}
                >
                  {option.icon} {option.label}
                </Chip>
              ))}
            </View>

            <TextInput
              label="Status Message (Optional)"
              value={statusMessage}
              onChangeText={setStatusMessage}
              mode="outlined"
              style={styles.input}
              placeholder="e.g., Studying for exams"
              maxLength={200}
            />

            <Button
              mode="contained"
              onPress={handleUpdateStatus}
              loading={loading}
              disabled={loading}
              style={styles.button}
            >
              Update Status
            </Button>
          </Card.Content>
        </Card>

        {roommateStatuses.length > 0 && (
          <Card style={styles.card}>
            <Card.Content>
              <Title>Roommate Statuses</Title>
              {roommateStatuses.map((roommate) => (
                <View key={roommate._id} style={styles.roommateItem}>
                  <View style={styles.roommateInfo}>
                    <Title style={styles.roommateName}>{roommate.name}</Title>
                    <Chip
                      style={[
                        styles.statusChip,
                        { backgroundColor: getStatusColor(roommate.status) },
                      ]}
                      textStyle={{ color: 'white', fontSize: 12 }}
                    >
                      {getStatusLabel(roommate.status)}
                    </Chip>
                  </View>
                  {roommate.statusMessage && (
                    <View style={styles.statusMessageContainer}>
                      <Title style={styles.statusMessageText}>
                        "{roommate.statusMessage}"
                      </Title>
                    </View>
                  )}
                </View>
              ))}
            </Card.Content>
          </Card>
        )}
      </View>

      <Snackbar
        visible={snackbarVisible}
        onDismiss={() => setSnackbarVisible(false)}
        duration={3000}
      >
        {snackbarMessage}
      </Snackbar>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    padding: 16,
  },
  card: {
    marginBottom: 16,
  },
  statusChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 16,
    marginBottom: 16,
  },
  chip: {
    margin: 4,
  },
  input: {
    marginBottom: 16,
  },
  button: {
    paddingVertical: 6,
  },
  roommateItem: {
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  roommateInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  roommateName: {
    fontSize: 16,
  },
  statusChip: {
    height: 28,
  },
  statusMessageContainer: {
    marginTop: 8,
  },
  statusMessageText: {
    fontSize: 14,
    fontStyle: 'italic',
    color: '#666',
  },
});
