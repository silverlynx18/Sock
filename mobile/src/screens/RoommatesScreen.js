import React, { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import {
  Title,
  Card,
  Button,
  TextInput,
  List,
  IconButton,
  Snackbar,
  Dialog,
  Portal,
  Text,
} from 'react-native-paper';
import { roommateAPI } from '../services/api';

export default function RoommatesScreen() {
  const [roommates, setRoommates] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [snackbarVisible, setSnackbarVisible] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [dialogVisible, setDialogVisible] = useState(false);
  const [selectedRoommate, setSelectedRoommate] = useState(null);

  useEffect(() => {
    loadRoommates();
  }, []);

  const loadRoommates = async () => {
    try {
      const response = await roommateAPI.getRoommates();
      setRoommates(response.data);
    } catch (error) {
      console.error('Error loading roommates:', error);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadRoommates();
    setRefreshing(false);
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      setSnackbarMessage('Please enter a search query');
      setSnackbarVisible(true);
      return;
    }

    setSearching(true);
    try {
      const response = await roommateAPI.search(searchQuery);
      setSearchResults(response.data);
    } catch (error) {
      setSnackbarMessage('Search failed');
      setSnackbarVisible(true);
    } finally {
      setSearching(false);
    }
  };

  const handleAddRoommate = async (userId) => {
    setLoading(true);
    try {
      await roommateAPI.addRoommate(userId);
      setSnackbarMessage('Roommate added successfully!');
      setSnackbarVisible(true);
      setSearchQuery('');
      setSearchResults([]);
      await loadRoommates();
    } catch (error) {
      setSnackbarMessage(
        error.response?.data?.message || 'Failed to add roommate'
      );
      setSnackbarVisible(true);
    } finally {
      setLoading(false);
    }
  };

  const confirmRemoveRoommate = (roommate) => {
    setSelectedRoommate(roommate);
    setDialogVisible(true);
  };

  const handleRemoveRoommate = async () => {
    if (!selectedRoommate) return;

    setDialogVisible(false);
    setLoading(true);
    try {
      await roommateAPI.removeRoommate(selectedRoommate._id);
      setSnackbarMessage('Roommate removed');
      setSnackbarVisible(true);
      await loadRoommates();
    } catch (error) {
      setSnackbarMessage('Failed to remove roommate');
      setSnackbarVisible(true);
    } finally {
      setLoading(false);
      setSelectedRoommate(null);
    }
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
            <Title>Find Roommates</Title>
            <View style={styles.searchContainer}>
              <TextInput
                label="Search by name or email"
                value={searchQuery}
                onChangeText={setSearchQuery}
                mode="outlined"
                style={styles.searchInput}
                onSubmitEditing={handleSearch}
              />
              <Button
                mode="contained"
                onPress={handleSearch}
                loading={searching}
                disabled={searching}
              >
                Search
              </Button>
            </View>

            {searchResults.length > 0 && (
              <View style={styles.results}>
                {searchResults.map((user) => (
                  <List.Item
                    key={user._id}
                    title={user.name}
                    description={`${user.email}${
                      user.roomNumber ? ` ? Room ${user.roomNumber}` : ''
                    }`}
                    right={() => (
                      <IconButton
                        icon="plus-circle"
                        onPress={() => handleAddRoommate(user._id)}
                      />
                    )}
                  />
                ))}
              </View>
            )}
          </Card.Content>
        </Card>

        <Card style={styles.card}>
          <Card.Content>
            <Title>Your Roommates ({roommates.length})</Title>
            {roommates.length === 0 ? (
              <Text style={styles.emptyText}>
                No roommates yet. Search and add roommates above!
              </Text>
            ) : (
              roommates.map((roommate) => (
                <List.Item
                  key={roommate._id}
                  title={roommate.name}
                  description={`${roommate.email}${
                    roommate.roomNumber
                      ? ` ? Room ${roommate.roomNumber}`
                      : ''
                  }${
                    roommate.dormName ? ` ? ${roommate.dormName}` : ''
                  }`}
                  right={() => (
                    <IconButton
                      icon="close-circle"
                      iconColor="#f44336"
                      onPress={() => confirmRemoveRoommate(roommate)}
                    />
                  )}
                />
              ))
            )}
          </Card.Content>
        </Card>
      </View>

      <Portal>
        <Dialog visible={dialogVisible} onDismiss={() => setDialogVisible(false)}>
          <Dialog.Title>Remove Roommate</Dialog.Title>
          <Dialog.Content>
            <Text>
              Are you sure you want to remove {selectedRoommate?.name} as your
              roommate?
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDialogVisible(false)}>Cancel</Button>
            <Button onPress={handleRemoveRoommate}>Remove</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>

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
  searchContainer: {
    marginTop: 16,
  },
  searchInput: {
    marginBottom: 8,
  },
  results: {
    marginTop: 16,
  },
  emptyText: {
    textAlign: 'center',
    color: '#666',
    marginTop: 16,
    marginBottom: 16,
  },
});
